package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraB15CounterFixtureTest {
    @Test public void observedPaleRedCounterPixelsSurviveWithoutSelectingGreenArtworkOrPinkBar(){
        for(int p:new int[]{0xffff8080,0xfffe8080,0xffff0000,0xffffffff})assertTrue(UraCounterGlyph.counterInk(p));
        for(int p:new int[]{0xfff078b4,0xff14ee40,0xff504020,0xff000000})assertFalse(UraCounterGlyph.counterInk(p));
    }
    @Test public void actualFiveTwoFourFiveCountersMatchIndependentFrameAndRejectBlankOrWrongDigit()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b15-counter-oct7.mask.gz")))){
            for(int i=0;i<4;i++){
                StringBuilder h=new StringBuilder();
                for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/progress-b15-counter-oct7-"+i+".png").toPath())))h.append(String.format("%02x",b&255));
                assertEquals(h.toString(),in.readUTF());
            }
            boolean[][][] masks=new boolean[4][3][34*43];
            for(boolean[][] set:masks)for(boolean[] m:set)for(int i=0;i<m.length;i++)m[i]=in.readByte()==1;
            for(int i=0;i<4;i++){
                assertTrue("counter "+i,UraCounterGlyph.matches(masks[i][0],masks[i][1],34,43));
                assertFalse(UraCounterGlyph.matches(masks[i][0],masks[i][2],34,43));
            }
            assertFalse(UraCounterGlyph.matches(masks[0][0],masks[1][1],34,43));
            assertFalse(UraCounterGlyph.matches(masks[2][0],masks[1][1],34,43));
        }
    }
}
