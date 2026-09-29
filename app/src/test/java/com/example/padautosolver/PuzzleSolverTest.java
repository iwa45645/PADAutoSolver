package com.example.padautosolver;

import org.junit.Test;
import java.util.Arrays;
import java.util.Random;
import static org.junit.Assert.*;

public class PuzzleSolverTest {
    private static byte[] board(String... rows) {
        String s = String.join("", rows);
        byte[] b = new byte[s.length()];
        for (int i = 0; i < b.length; i++) b[i] = (byte) (s.charAt(i) - '0');
        return b;
    }
    private static int score(byte[] b, int cols, int rows) {
        return (int) PuzzleSolver.evaluatePacked(b, cols, rows);
    }
    @Test public void gravityCreatesSecondCombo() {
        byte[] b = board("120", "201", "000");
        // Bottom red clears; remaining colors have no triples.
        assertEquals(1003, score(b, 3, 3));
        // Hand-constructed 3x4 cascade (0 = red, 1 = blue).
        b = board("213", "302", "401", "105");
        // Middle-column red at rows 1..3 clears. Blue at (1,0) falls to
        // bottom between (0,3) and (2,2), which does not fall: no extra match.
        assertEquals(1003, score(b, 3, 4));
        b = board("213", "302", "405", "101");
        assertEquals(2006, score(b, 3, 4));
    }
    @Test public void crossIsOneCombo() {
        assertEquals(1005, score(board("102", "000", "304"), 3, 3));
    }
    @Test public void separatedSameColorGroupsAreTwoCombos() {
        assertEquals(2006, score(board("0001000", "1234512", "2345123"), 7, 3));
    }
    @Test public void connectedParallelTriplesAreOneCombo() {
        assertEquals(1006, score(board("000", "000", "123"), 3, 3));
    }
    @Test public void emptyCellsDoNotCreateCombos() {
        assertEquals(1009, score(board("000", "000", "000"), 3, 3));
    }
    @Test public void diagonalDoesNotMatch() {
        assertEquals(0, score(board("012", "201", "120"), 3, 3));
    }
    @Test public void evaluationDoesNotMutateBoard() {
        byte[] b = board("213", "302", "405", "101");
        byte[] copy = b.clone(); score(b, 3, 4); assertArrayEquals(copy, b);
    }
    @Test public void routesAreContinuousAndScoresMatchReplayOnBothBoardSizes() {
        Random random = new Random(2042);
        for (int cols : new int[]{6, 7}) {
            int rows = cols - 1;
            for (int sample = 0; sample < 4; sample++) {
                byte[] b = new byte[cols * rows];
                for (int i = 0; i < b.length; i++) b[i] = (byte) random.nextInt(6);
                byte[] original = b.clone();
                PuzzleSolver.Result result = PuzzleSolver.solve(b, cols, rows, 12, 150);
                assertArrayEquals(original, b);
                assertTrue(result.path.size() <= 13);
                for (int i = 1; i < result.path.size(); i++) {
                    int a = result.path.get(i - 1), c = result.path.get(i);
                    assertEquals(1, Math.abs(a % cols - c % cols) + Math.abs(a / cols - c / cols));
                    byte v = b[a]; b[a] = b[c]; b[c] = v;
                }
                assertEquals(score(b, cols, rows), result.combos * 1000 + result.matchedOrbs);
                assertTrue(result.combos * 1000 + result.matchedOrbs >= score(original, cols, rows));
            }
        }
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsInvalidColor() {
        byte[] b = new byte[30]; Arrays.fill(b, (byte) 6);
        PuzzleSolver.solve(b, 6, 5, 8, 100);
    }
    @Test public void searchRespectsTimeBudgetAndKeepsBestRoute() {
        byte[] b = board("012345", "120453", "230154", "345012", "450123");
        long start = System.nanoTime();
        PuzzleSolver.Result result = PuzzleSolver.solve(b, 6, 5, 50, 5000, 20);
        assertTrue((System.nanoTime() - start) / 1_000_000 < 1000);
        assertFalse(result.path.isEmpty());
        assertTrue(result.path.size() <= 51);
    }
}
