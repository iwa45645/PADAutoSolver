package com.example.padautosolver;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
public class UraPreflightTurnTest {
    @Test public void fullSkillLineIsNotRejectedForHavingACentreLeftOfCooldownColumn() {
        assertEquals(Integer.valueOf(23),UraPreflightController.turn(List.of(new StagePolicy.Item("スキル 雪集の一変 Lv.最大 ターン:23",610,2220)),2180,2280));
    }
    @Test public void assistantTurnIsSeparateFromBase() {
        List<StagePolicy.Item> lines=List.of(new StagePolicy.Item("ターン:23",1100,2220),new StagePolicy.Item("ターン:41",1100,2315));
        assertEquals(Integer.valueOf(23),UraPreflightController.turn(lines,2180,2280));
        assertEquals(Integer.valueOf(41),UraPreflightController.turn(lines,2280,2365));
    }
    @Test public void conflictingOrMissingDigitsAreUnknown() {
        assertNull(UraPreflightController.turn(List.of(new StagePolicy.Item("ターン:23",610,2220),new StagePolicy.Item("ターン:28",610,2220)),2180,2280));
        assertNull(UraPreflightController.turn(List.of(new StagePolicy.Item("ターン:2B",610,2220)),2180,2280));
    }
}
