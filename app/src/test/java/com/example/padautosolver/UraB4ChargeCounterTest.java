package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraB4ChargeCounterTest {
    @Test public void independentTwoSevenTwoAndLegacyTwoFiveTwoRejectOtherDigits()throws Exception{
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b4-charge-counter-5-7.glyph.gz")))){
            for(String prefix:new String[]{"b4-count-","b4-charge-oct7-"})for(int i=0;i<3;i++){
                StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+prefix+i+".png").toPath())))hash.append(String.format("%02x",b&255));assertEquals(hash.toString(),in.readUTF());
            }
            boolean[][] glyphs=new boolean[10][45*55];for(boolean[] glyph:glyphs)for(int i=0;i<glyph.length;i++)glyph[i]=in.readByte()==1;
            for(int i=0;i<3;i++)assertTrue("Actual B4 counter "+i,UraCounterGlyph.matches(glyphs[3+i],glyphs[6+i],45,55));
            for(int i=0;i<3;i++)assertTrue("Legacy counter",UraCounterGlyph.matches(glyphs[i],glyphs[i],45,55));
            assertFalse(UraCounterGlyph.matches(glyphs[1],glyphs[7],45,55));
            assertFalse(UraCounterGlyph.matches(glyphs[4],glyphs[1],45,55));
            assertFalse(UraCounterGlyph.matches(glyphs[4],glyphs[9],45,55));
            assertFalse(UraCounterGlyph.matches(glyphs[4],new boolean[45*55],45,55));
        }
    }
}
