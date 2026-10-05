package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraClearGlyphTest {
    private int[] pixels(String name)throws Exception {
        File asset=new File("src/main/assets/ura-shura/progress-b22-clear-"+name+".png");StringBuilder hash=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(asset.toPath())))hash.append(String.format("%02x",b&255));
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b22-clear-"+name+".argb.gz")))){
            assertEquals(hash.toString(),in.readUTF());int[] p=new int[in.readInt()];for(int i=0;i<p.length;i++)p[i]=in.readInt();return p;
        }
    }
    @Test public void reviewedClearLetteringSurvivesParticlesAndRejectsTheLiveEnemy()throws Exception {
        int[] ref=pixels("logo");assertTrue(UraClearGlyph.matches(ref,pixels("later")));
        assertFalse(UraClearGlyph.matches(ref,pixels("enemy")));assertFalse(UraClearGlyph.matches(ref,new int[840*200]));
        int[] filled=new int[840*200];java.util.Arrays.fill(filled,0xff00bbee);assertFalse(UraClearGlyph.matches(ref,filled));
    }
}
