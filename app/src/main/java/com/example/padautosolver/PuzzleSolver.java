package com.example.padautosolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CancellationException;

public final class PuzzleSolver {
    public static final class Result {
        public final List<Integer> path;
        public final int combos;
        public final int matchedOrbs;
        public final int exploredDepth;

        Result(List<Integer> path, int combos, int matchedOrbs, int exploredDepth) {
            this.path = path;
            this.combos = combos;
            this.matchedOrbs = matchedOrbs;
            this.exploredDepth = exploredDepth;
        }
    }

    private static final int[][] DIRS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    private PuzzleSolver() {}

    public static Result solve(byte[] initial, int cols, int rows, int maxSteps, int beamWidth) {
        return solve(initial, cols, rows, maxSteps, beamWidth, 0);
    }

    public static Result solve(byte[] initial, int cols, int rows, int maxSteps, int beamWidth, long timeBudgetMs) {
        if (cols < 3 || rows < 3 || cols * rows > 63 || initial == null || initial.length != cols * rows
                || maxSteps < 0 || maxSteps > 50 || beamWidth < 1 || beamWidth > 5000) {
            throw new IllegalArgumentException("Invalid board size");
        }
        for (byte color : initial) {
            if (color < 0 || color > 5) throw new IllegalArgumentException("Invalid orb color");
        }

        long basePacked = evaluatePacked(initial, cols, rows);
        int baseHeuristic = unpackHeuristic(basePacked);
        int baseObjective = unpackObjective(basePacked);

        List<Node> beam = new ArrayList<>(initial.length);
        Node best = null;
        for (int start = 0; start < initial.length; start++) {
            Node n = new Node(initial.clone(), start, -1, null, 0, baseHeuristic, baseObjective);
            beam.add(n);
            if (betterFinal(n, best)) best = n;
        }

        long deadline = timeBudgetMs <= 0 ? Long.MAX_VALUE : System.nanoTime() + timeBudgetMs * 1_000_000L;
        search:
        for (int depth = 1; depth <= maxSteps; depth++) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            PriorityQueue<Node> top = new PriorityQueue<>(beamWidth + 1,
                    Comparator.comparingInt((Node n) -> n.heuristic)
                            .thenComparingInt(n -> n.objective));
            HashSet<Long> seen = new HashSet<>(beamWidth * 6);

            for (Node node : beam) {
                if (Thread.currentThread().isInterrupted()) throw new CancellationException();
                if (System.nanoTime() >= deadline) break search;
                int x = node.pos % cols;
                int y = node.pos / cols;

                for (int[] dir : DIRS) {
                    int nx = x + dir[0];
                    int ny = y + dir[1];
                    if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) continue;
                    int nextPos = ny * cols + nx;
                    if (nextPos == node.prevPos) continue; // 即時往復は探索上ほぼ無駄

                    byte[] nextBoard = node.board.clone();
                    byte tmp = nextBoard[node.pos];
                    nextBoard[node.pos] = nextBoard[nextPos];
                    nextBoard[nextPos] = tmp;

                    long key = stateHash(nextBoard, nextPos, node.pos);
                    if (!seen.add(key)) continue;

                    long packed = evaluatePacked(nextBoard, cols, rows);
                    int heuristic = unpackHeuristic(packed);
                    int objective = unpackObjective(packed);
                    Node child = new Node(nextBoard, nextPos, node.pos, node,
                            depth, heuristic, objective);

                    if (betterFinal(child, best)) best = child;

                    if (top.size() < beamWidth) {
                        top.offer(child);
                    } else {
                        Node worst = top.peek();
                        if (compareBeam(child, worst) > 0) {
                            top.poll();
                            top.offer(child);
                        }
                    }
                }
            }

            if (top.isEmpty()) break;
            beam = new ArrayList<>(top);
        }

        List<Integer> path = reconstruct(best);
        int combos = best.objective / 1000;
        int matched = best.objective % 1000;
        return new Result(path, combos, matched, best.depth);
    }

    private static int compareBeam(Node a, Node b) {
        int c = Integer.compare(a.heuristic, b.heuristic);
        if (c != 0) return c;
        return Integer.compare(a.objective, b.objective);
    }

    private static boolean betterFinal(Node a, Node b) {
        if (b == null) return true;
        if (a.objective != b.objective) return a.objective > b.objective;
        return a.depth < b.depth;
    }

    private static List<Integer> reconstruct(Node node) {
        ArrayList<Integer> reverse = new ArrayList<>();
        Node cur = node;
        while (cur != null) {
            reverse.add(cur.pos);
            cur = cur.parent;
        }
        Collections.reverse(reverse);
        return reverse;
    }

    /**
     * 上位32bit = beam探索用ヒューリスティック、下位32bit = 最終評価。
     * objective = combos * 1000 + matchedOrbs
     */
    static long evaluatePacked(byte[] b, int cols, int rows) {
        byte[] settled = null;
        byte[] current = b;
        int combos = 0;
        int matched = 0;
        long mask;
        // 消去→重力→再消去を繰り返す。盤面外からのランダムな落ちコンは仮定しない。
        while ((mask = findMatchMask(current, cols, rows)) != 0L) {
            combos += countConnectedMatches(current, cols, rows, mask);
            matched += Long.bitCount(mask);
            if (settled == null) settled = b.clone();
            for (int p = 0; p < settled.length; p++) {
                if ((mask & (1L << p)) != 0) settled[p] = -1;
            }
            for (int x = 0; x < cols; x++) {
                int writeY = rows - 1;
                for (int y = rows - 1; y >= 0; y--) {
                    byte color = settled[y * cols + x];
                    if (color >= 0) settled[writeY-- * cols + x] = color;
                }
                while (writeY >= 0) settled[writeY-- * cols + x] = -1;
            }
            current = settled;
        }
        int objective = combos * 1000 + matched;
        int heuristic = combos * 70_000 + matched * 900 + potentialScore(b, cols, rows) * 120;
        return ((long) heuristic << 32) | (objective & 0xffffffffL);
    }

    private static long findMatchMask(byte[] b, int cols, int rows) {
        long matchMask = 0L;

        // 横3個以上
        for (int y = 0; y < rows; y++) {
            int x = 0;
            while (x < cols) {
                int j = x + 1;
                byte color = b[y * cols + x];
                while (j < cols && b[y * cols + j] == color) j++;
                if (color >= 0 && j - x >= 3) {
                    for (int k = x; k < j; k++) matchMask |= 1L << (y * cols + k);
                }
                x = j;
            }
        }

        // 縦3個以上
        for (int x = 0; x < cols; x++) {
            int y = 0;
            while (y < rows) {
                int j = y + 1;
                byte color = b[y * cols + x];
                while (j < rows && b[j * cols + x] == color) j++;
                if (color >= 0 && j - y >= 3) {
                    for (int k = y; k < j; k++) matchMask |= 1L << (k * cols + x);
                }
                y = j;
            }
        }

        return matchMask;
    }

    private static int countConnectedMatches(byte[] b, int cols, int rows, long matchMask) {
        int combos = 0;
        long remaining = matchMask;
        while (remaining != 0L) {
            int start = Long.numberOfTrailingZeros(remaining);
            byte color = b[start];
            long component = 0L;
            long frontier = 1L << start;

            while (frontier != 0L) {
                int p = Long.numberOfTrailingZeros(frontier);
                long bit = 1L << p;
                frontier &= ~bit;
                if ((component & bit) != 0L) continue;
                component |= bit;

                int x = p % cols;
                int y = p / cols;
                if (x > 0) frontier = addIfSame(frontier, component, matchMask, b, p - 1, color);
                if (x + 1 < cols) frontier = addIfSame(frontier, component, matchMask, b, p + 1, color);
                if (y > 0) frontier = addIfSame(frontier, component, matchMask, b, p - cols, color);
                if (y + 1 < rows) frontier = addIfSame(frontier, component, matchMask, b, p + cols, color);
            }

            remaining &= ~component;
            combos++;
        }
        return combos;
    }

    private static long addIfSame(long frontier, long component, long matchMask,
                                  byte[] b, int index, byte color) {
        long bit = 1L << index;
        if ((matchMask & bit) != 0L && (component & bit) == 0L && b[index] == color) {
            frontier |= bit;
        }
        return frontier;
    }

    private static int potentialScore(byte[] b, int cols, int rows) {
        int score = 0;

        // 3マス窓の2個揃いを重視。
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x <= cols - 3; x++) {
                byte a = b[y * cols + x];
                byte c = b[y * cols + x + 1];
                byte d = b[y * cols + x + 2];
                score += triplePotential(a, c, d);
            }
        }
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y <= rows - 3; y++) {
                byte a = b[y * cols + x];
                byte c = b[(y + 1) * cols + x];
                byte d = b[(y + 2) * cols + x];
                score += triplePotential(a, c, d);
            }
        }

        // 隣接同色を少し評価。
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int i = y * cols + x;
                if (x + 1 < cols && b[i] == b[i + 1]) score += 2;
                if (y + 1 < rows && b[i] == b[i + cols]) score += 2;
            }
        }
        return score;
    }

    private static int triplePotential(byte a, byte b, byte c) {
        if (a == b && b == c) return 40;
        if (a == b || a == c || b == c) return 10;
        return 0;
    }

    private static long stateHash(byte[] board, int pos, int prevPos) {
        long h = 0xcbf29ce484222325L;
        for (byte v : board) {
            h ^= (v + 1);
            h *= 0x100000001b3L;
        }
        h ^= (pos + 31L * (prevPos + 1));
        h *= 0x100000001b3L;
        return h;
    }

    private static int unpackHeuristic(long packed) {
        return (int) (packed >>> 32);
    }

    private static int unpackObjective(long packed) {
        return (int) packed;
    }

    private static final class Node {
        final byte[] board;
        final int pos;
        final int prevPos;
        final Node parent;
        final int depth;
        final int heuristic;
        final int objective;

        Node(byte[] board, int pos, int prevPos, Node parent, int depth,
             int heuristic, int objective) {
            this.board = board;
            this.pos = pos;
            this.prevPos = prevPos;
            this.parent = parent;
            this.depth = depth;
            this.heuristic = heuristic;
            this.objective = objective;
        }
    }
}
