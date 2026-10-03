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
        public final int waterCombos, healCombos;
        public final boolean goalSatisfied;

        Result(List<Integer> path, int combos, int matchedOrbs, int exploredDepth) {
            this(path, combos, matchedOrbs, exploredDepth, 0, 0, true);
        }
        Result(List<Integer> path, int combos, int matchedOrbs, int exploredDepth,
               int waterCombos, int healCombos, boolean goalSatisfied) {
            this.path = path;
            this.combos = combos;
            this.matchedOrbs = matchedOrbs;
            this.exploredDepth = exploredDepth;
            this.waterCombos = waterCombos; this.healCombos = healCombos;
            this.goalSatisfied = goalSatisfied;
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
        return solve(initial, cols, rows, maxSteps, beamWidth, timeBudgetMs, null);
    }

    public static Result solve(byte[] initial, int cols, int rows, int maxSteps, int beamWidth,
                               long timeBudgetMs, PuzzleGoal goal) {
        return solve(initial,cols,rows,maxSteps,beamWidth,timeBudgetMs,goal,0);
    }

    public static Result solve(byte[] initial, int cols, int rows, int maxSteps, int beamWidth,
                               long timeBudgetMs, PuzzleGoal goal, long comboDropMask) {
        if (cols < 3 || rows < 3 || cols * rows > 63 || initial == null || initial.length != cols * rows
                || maxSteps < 0 || maxSteps > 50 || beamWidth < 1 || beamWidth > 5000) {
            throw new IllegalArgumentException("Invalid board size");
        }
        for (byte color : initial) {
            if (color < 0 || color > (goal == null ? 5 : 9)) throw new IllegalArgumentException("Unknown/invalid orb color");
        }

        if(comboDropMask<0||(comboDropMask>>>initial.length)!=0)throw new IllegalArgumentException("Combo drop outside board");
        long basePacked = evaluateForGoal(initial, cols, rows, goal, comboDropMask);
        int baseHeuristic = unpackHeuristic(basePacked);
        int baseObjective = unpackObjective(basePacked);

        List<Node> beam = new ArrayList<>(initial.length);
        Node best = null;
        for (int start = 0; start < initial.length; start++) {
            Node n = new Node(initial.clone(), start, -1, null, 0, baseHeuristic, baseObjective,comboDropMask);
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

                    long nextComboDropMask=node.comboDropMask;
                    if(((nextComboDropMask>>>node.pos)&1)!=((nextComboDropMask>>>nextPos)&1))
                        nextComboDropMask^=(1L<<node.pos)|(1L<<nextPos);
                    long key = stateHash(nextBoard, nextPos, node.pos)^Long.rotateLeft(nextComboDropMask,17);
                    if (!seen.add(key)) continue;

                    long packed = evaluateForGoal(nextBoard, cols, rows, goal,nextComboDropMask);
                    int heuristic = unpackHeuristic(packed);
                    int objective = unpackObjective(packed);
                    Node child = new Node(nextBoard, nextPos, node.pos, node,
                            depth, heuristic, objective,nextComboDropMask);

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
        if (goal == null) return new Result(path, best.objective / 1000, best.objective % 1000, best.depth);
        MatchStats stats = analyze(best.board, cols, rows,best.comboDropMask);
        return new Result(path, stats.combos, stats.matched, best.depth, stats.colorCombos[3],
                stats.colorCombos[5], goal.satisfied(stats, initial.length));
    }

    private static long evaluateForGoal(byte[] b, int cols, int rows, PuzzleGoal goal,long comboDropMask) {
        if (goal == null) return evaluatePacked(b, cols, rows);
        MatchStats stats = analyze(b, cols, rows,comboDropMask);
        int objective = goal.score(stats, b.length);
        int heuristic = objective * 100 + potentialScore(b, cols, rows) * 20;
        return ((long)heuristic << 32) | (objective & 0xffffffffL);
    }

    public static final class MatchStats {
        public int combos, matched, comboDropsMatched;
        public final int[] colorCombos = new int[10], colorMatched = new int[10], squares = new int[10], lShapes = new int[10], crosses = new int[10];
    }

    /** Deterministic cascades only: no invented skyfall, roulette timing, or obscured cells. */
    public static MatchStats analyze(byte[] initial, int cols, int rows) {
        return analyze(initial,cols,rows,0);
    }
    public static MatchStats analyze(byte[] initial, int cols, int rows,long comboDropMask) {
        return analyze(initial,cols,rows,comboDropMask,false);
    }
    /** Matches before any gravity. Roulette proofs must not rely on a snapshot's cascades. */
    static MatchStats firstWave(byte[] initial,int cols,int rows) {
        return analyze(initial,cols,rows,0,true);
    }
    private static MatchStats analyze(byte[] initial,int cols,int rows,long comboDropMask,boolean firstOnly) {
        if (initial == null || cols < 3 || rows < 3 || cols * rows > 63 || initial.length != cols * rows)
            throw new IllegalArgumentException("Invalid board");
        for (byte value : initial) if (value < 0 || value > 9) throw new IllegalArgumentException("Unknown board");
        if(comboDropMask<0||(comboDropMask>>>initial.length)!=0)throw new IllegalArgumentException("Invalid combo drop mask");
        MatchStats result = new MatchStats();
        byte[] board = initial.clone();
        long mask;
        while ((mask = findMatchMask(board, cols, rows)) != 0) {
            long remaining = mask;
            while (remaining != 0) {
                int start = Long.numberOfTrailingZeros(remaining);
                byte color = board[start];
                long component = 0, frontier = 1L << start;
                while (frontier != 0) {
                    int p = Long.numberOfTrailingZeros(frontier);
                    long bit = 1L << p; frontier &= ~bit;
                    if ((component & bit) != 0) continue;
                    component |= bit;
                    int x = p % cols, y = p / cols;
                    if (x > 0) frontier = addIfSame(frontier, component, mask, board, p-1, color);
                    if (x+1 < cols) frontier = addIfSame(frontier, component, mask, board, p+1, color);
                    if (y > 0) frontier = addIfSame(frontier, component, mask, board, p-cols, color);
                    if (y+1 < rows) frontier = addIfSame(frontier, component, mask, board, p+cols, color);
                }
                remaining &= ~component;
                result.combos++; result.colorCombos[color]++;
                int size = Long.bitCount(component);
                result.colorMatched[color]+=size;
                if (size == 9) {
                    for (int y=0;y<=rows-3;y++) for(int x=0;x<=cols-3;x++) {
                        long shape=0;for(int dy=0;dy<3;dy++)for(int dx=0;dx<3;dx++)shape|=1L<<((y+dy)*cols+x+dx);
                        if (shape == component) result.squares[color]++;
                    }
                }
                if (size == 5) {
                    for (int y=0;y<rows;y++) for(int x=0;x<cols;x++) {
                        int p=y*cols+x;
                        if (x>0 && x+1<cols && y>0 && y+1<rows && component ==
                                ((1L<<p)|(1L<<(p-1))|(1L<<(p+1))|(1L<<(p-cols))|(1L<<(p+cols)))) result.crosses[color]++;
                        for(int dx:new int[]{-1,1})for(int dy:new int[]{-1,1}) {
                            if(x+2*dx<0||x+2*dx>=cols||y+2*dy<0||y+2*dy>=rows)continue;
                            long shape=(1L<<p)|(1L<<(p+dx))|(1L<<(p+2*dx))|(1L<<(p+dy*cols))|(1L<<(p+2*dy*cols));
                            if(shape==component)result.lShapes[color]++;
                        }
                    }
                }
            }
            result.matched += Long.bitCount(mask);
            result.comboDropsMatched+=Long.bitCount(mask&comboDropMask);
            if(firstOnly)break;
            comboDropMask&=~mask;
            for(int p=0;p<board.length;p++)if((mask&(1L<<p))!=0)board[p]=-1;
            for(int x=0;x<cols;x++) {
                int write=rows-1;
                for(int y=rows-1;y>=0;y--)if(board[y*cols+x]>=0){
                    int from=y*cols+x,to=write--*cols+x;board[to]=board[from];
                    if(from!=to){boolean tagged=(comboDropMask&(1L<<from))!=0;
                        comboDropMask&=~((1L<<from)|(1L<<to));if(tagged)comboDropMask|=1L<<to;}
                }
                while(write>=0)board[write--*cols+x]=-1;
            }
        }
        return result;
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
        final long comboDropMask;

        Node(byte[] board, int pos, int prevPos, Node parent, int depth,
             int heuristic, int objective,long comboDropMask) {
            this.board = board;
            this.pos = pos;
            this.prevPos = prevPos;
            this.parent = parent;
            this.depth = depth;
            this.heuristic = heuristic;
            this.objective = objective;
            this.comboDropMask=comboDropMask;
        }
    }
}
