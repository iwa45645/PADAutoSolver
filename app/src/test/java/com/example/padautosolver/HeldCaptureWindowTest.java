package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class HeldCaptureWindowTest {
    @Test public void acceptsFreshFrameWhileHeld() {assertTrue(HeldCaptureWindow.contains(1000,2100,2200,3000));}
    @Test public void rejectsCachedFrameBeforeTooltip() {assertFalse(HeldCaptureWindow.contains(1000,1500,2200,3000));}
    @Test public void rejectsReleasedOrTooLateFrame() {
        assertFalse(HeldCaptureWindow.contains(1000,3900,4000,3000));
        assertFalse(HeldCaptureWindow.contains(1000,3750,3800,3000));
    }
    @Test public void rejectsShortOrInvalidTimestamp() {
        assertFalse(HeldCaptureWindow.contains(1000,2100,2200,1000));
        assertFalse(HeldCaptureWindow.contains(1000,2300,2200,3000));
    }
}
