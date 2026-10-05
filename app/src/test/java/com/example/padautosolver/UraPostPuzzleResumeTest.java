package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraPostPuzzleResumeTest {
    @Test public void onlyNativeCompletedGestureCanResumeFloorObservation() {
        assertTrue(UraPostPuzzleResume.observationOnly(false,17,6,true,true,true,true));
        assertFalse(UraPostPuzzleResume.observationOnly(false,16,6,true,true,true,true));
        assertFalse(UraPostPuzzleResume.observationOnly(false,17,5,true,true,true,true));
        assertFalse(UraPostPuzzleResume.observationOnly(true,17,6,true,true,true,true));
        assertFalse(UraPostPuzzleResume.observationOnly(false,17,6,false,true,true,true));
        assertFalse(UraPostPuzzleResume.observationOnly(false,17,6,true,false,true,true));
        assertFalse(UraPostPuzzleResume.observationOnly(false,17,6,true,true,false,true));
        assertFalse(UraPostPuzzleResume.observationOnly(false,17,6,true,true,true,false));
    }
}
