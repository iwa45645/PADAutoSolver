package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraMionPostHeadingTest {
    @Test public void independentHeldMionHeadingMatchesExistingReferenceAndRejectsOtherSkills()throws Exception{
        String[] names={"post-mion-skill-header.png","post-sekka-skill-header.png","post-ruka-skill-header.png"};
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b3-mion-post-heading.argb.gz")))){
            for(String name:names){StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())))hash.append(String.format("%02x",b&255));assertEquals(hash.toString(),in.readUTF());}
            int[][] frames=new int[4][48*32];for(int[] frame:frames)for(int i=0;i<frame.length;i++)frame[i]=in.readInt();
            assertTrue(TeamIconMatch.distance(frames[0],frames[1])<.025);
            for(int i:new int[]{2,3})assertTrue(TeamIconMatch.distance(frames[0],frames[i])>.025);
            assertTrue(TeamIconMatch.distance(frames[1],new int[48*32])>.025);
        }
    }
}
