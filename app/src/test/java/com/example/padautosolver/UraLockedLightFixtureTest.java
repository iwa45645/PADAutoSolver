package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraLockedLightFixtureTest {
    @Test public void reviewedLockedLightIsNotAnyOtherOrbColor()throws Exception{
        File asset=new File("src/main/assets/ura-shura/stability-b1-locked-light.png");StringBuilder hash=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(asset.toPath())))hash.append(String.format("%02x",b&255));
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b1-locked-light.argb.gz")))){
            assertEquals(hash.toString(),in.readUTF());int[][] colors=new int[6][48*48];
            for(int[] pixels:colors)for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();
            assertEquals(0,TeamIconMatch.distance(colors[0],colors[0]),0);
            for(int c=1;c<6;c++)assertTrue("Locked light must remain distinct from color "+c,TeamIconMatch.distance(colors[0],colors[c])>.07+.035);
            assertTrue(TeamIconMatch.distance(colors[0],new int[48*48])>.07);
        }
    }
}
