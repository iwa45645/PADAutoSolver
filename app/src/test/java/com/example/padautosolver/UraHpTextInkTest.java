package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraHpTextInkTest {
    @Test public void literalGreenYellowAndWhiteDigitsSurvive(){
        for(int p:new int[]{0xff24ee18,0xffffed24,0xffeeeeee})assertTrue(UraHpTextInk.isInk(p));
    }
    @Test public void pinkFillBlackOutlineAndMutedBlueArtworkDoNotMergeWithDigits(){
        for(int p:new int[]{0xfff078b4,0xff080808,0xff34a4ec,0xff506030})assertFalse(UraHpTextInk.isInk(p));
    }
    @Test public void actualCyanHpCoreSurvivesWithoutSelectingPinkFillOrDarkOutline(){
        for(int p:new int[]{0xff40e0ff,0xff40dffe,0xff3fd2f1,0xff3ecfea})assertTrue(UraHpTextInk.isInk(p));
        for(int p:new int[]{0xfff078b4,0xff34a4ec,0xff406070,0xff80c0c0})assertFalse(UraHpTextInk.isInk(p));
    }
}
