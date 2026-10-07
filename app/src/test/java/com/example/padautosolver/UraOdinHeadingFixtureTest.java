package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraOdinHeadingFixtureTest {
    @Test public void readinessHeadingMatchesIndependentUsedOdinAndRejectsOtherSkills()throws Exception{
        String[] names={"post-odin-skill-header.png","post-sekka-skill-header.png","post-ruka-skill-header.png","post-mion-skill-header.png"};
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/progress-odin-heading.argb.gz")))){
            for(String name:names){StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())))hash.append(String.format("%02x",b&255));assertEquals(hash.toString(),in.readUTF());}
            int[][] frames=new int[6][48*32];for(int[] f:frames)for(int i=0;i<f.length;i++)f[i]=in.readInt();
            for(int i:new int[]{1,2})assertTrue(TeamIconMatch.distance(frames[0],frames[i])<.025);
            for(int i:new int[]{3,4,5})assertTrue(TeamIconMatch.distance(frames[0],frames[i])>.025);
            assertTrue(TeamIconMatch.distance(frames[0],new int[48*32])>.025);
        }
    }
}
