package com.example.padautosolver;

import java.util.Arrays;

/** Waits for a different, stable board and settled battle animation between drags. */
final class RoundGate {
    private byte[] previous, played;
    private int[] pixels;
    private long stableSince, notBefore;
    boolean ready(byte[] board, int[] frame, long now) {
        boolean stable = Arrays.equals(previous, board) && similar(pixels, frame);
        previous = board.clone(); pixels = frame.clone();
        if (!stable) stableSince = now;
        return now >= notBefore && stable && now - stableSince >= 700 && !Arrays.equals(played, board);
    }
    void played(byte[] board, long now) {
        played = board.clone(); previous = null; pixels = null;
        notBefore = now + 2200;
    }
    boolean isUnchanged(byte[] board) { return Arrays.equals(played, board); }
    boolean hasPlayed() { return played != null; }
    void reset() { previous = played = null; pixels = null; stableSince = notBefore = 0; }
    private static boolean similar(int[] a, int[] b) {
        if (a == null || a.length != b.length) return false;
        long difference = 0;
        for (int i = 0; i < a.length; i++) {
            for (int shift : new int[]{0, 8, 16}) difference += Math.abs(((a[i] >> shift) & 255) - ((b[i] >> shift) & 255));
        }
        return difference <= a.length * 3L * 6;
    }
}
