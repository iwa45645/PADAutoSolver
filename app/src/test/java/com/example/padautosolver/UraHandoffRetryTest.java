package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraHandoffRetryTest {
    @Test public void glowCanOnlyCauseFourExtraObservations(){
        UraHandoffRetry retry=new UraHandoffRetry();
        for(int i=0;i<4;i++)assertTrue(retry.observeAgain("B2_CLEAR_VERIFIED_B3_CAPTURE_REQUIRED"));
        assertFalse(retry.observeAgain("B2_CLEAR_VERIFIED_B3_CAPTURE_REQUIRED"));
        assertFalse(retry.observeAgain("B2_CLEAR_VERIFIED_B3_CAPTURE_REQUIRED"));
        retry.reset();assertTrue(retry.observeAgain("B2_CLEAR_VERIFIED_B3_CAPTURE_REQUIRED"));
    }
    @Test public void onlyVerifiedAdjacentFloorsCanWait(){
        for(String value:new String[]{null,"B3_GAME_OVER_OR_PURCHASE","B3_REFRESH_TURN_NOT_VERIFIED",
                "B2_CLEAR_VERIFIED_B4_CAPTURE_REQUIRED","B0_CLEAR_VERIFIED_B1_CAPTURE_REQUIRED",
                "B22_CLEAR_VERIFIED_B23_CAPTURE_REQUIRED","B2_CLEAR_B3_CAPTURE_REQUIRED"})
            assertFalse(UraHandoffRetry.verifiedTransition(value));
        assertTrue(UraHandoffRetry.verifiedTransition("B1_CLEAR_VERIFIED_B2_CURRENT_INSTRUCTION_REQUIRED"));
        assertTrue(UraHandoffRetry.verifiedTransition("B21_CLEAR_VERIFIED_B22_CAPTURE_REQUIRED"));
    }
    @Test public void NextStageGetsItsOwnBoundedBudget(){
        UraHandoffRetry retry=new UraHandoffRetry();
        for(int i=0;i<5;i++)retry.observeAgain("B2_CLEAR_VERIFIED_B3_CAPTURE_REQUIRED");
        assertTrue(retry.observeAgain("B3_CLEAR_VERIFIED_B4_CAPTURE_REQUIRED"));
    }
}
