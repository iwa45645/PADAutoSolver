package com.example.padautosolver;

import org.junit.Test;
import static org.junit.Assert.*;

public class BoxPageStabilityTest {
    @Test public void acceptsRecordedBoundaryFlicker(){
        // Device frames: first-row phase 1 versus 236, measured row pitch 237.
        assertTrue(BoxPageStability.samePosition(70,1156.5f,30,75,1391.5f,25,5,237));
        assertTrue(BoxPageStability.samePosition(75,1391.5f,25,70,1156.5f,30,5,237));
    }
    @Test public void rejectsRealOneRowScroll(){assertFalse(BoxPageStability.samePosition(70,1156.5f,30,75,1156.5f,30,5,237));}
    @Test public void rejectsMovingSameCards(){assertFalse(BoxPageStability.samePosition(70,1156.5f,30,70,1170,30,5,237));}
    @Test public void requiresSharedRows(){assertFalse(BoxPageStability.samePosition(70,1156.5f,30,100,2578.5f,30,5,237));}
    @Test public void rejectsColumnShift(){assertFalse(BoxPageStability.samePosition(70,1156.5f,30,71,1156.5f,30,5,237));}
    @Test public void initialFrameIsNotStable(){assertFalse(BoxPageStability.samePosition(-1,-1000,0,70,1156.5f,30,5,237));}
}
