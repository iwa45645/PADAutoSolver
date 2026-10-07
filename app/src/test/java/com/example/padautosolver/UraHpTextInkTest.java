package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraHpTextInkTest {
    @Test public void literalGreenYellowAndWhiteDigitsSurvive(){
        for(int p:new int[]{0xff24ee18,0xffffed24,0xffeeeeee})assertTrue(UraHpTextInk.isInk(p));
    }
    @Test public void pinkFillBlackOutlineAndBlueArtworkDoNotMergeWithDigits(){
        for(int p:new int[]{0xfff078b4,0xff080808,0xff34a4ec,0xff506030})assertFalse(UraHpTextInk.isInk(p));
    }
}
