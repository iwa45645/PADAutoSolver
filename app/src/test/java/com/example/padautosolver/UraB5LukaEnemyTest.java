package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraB5LukaEnemyTest {
    @Test public void independentLiveLukaRejectsNapoleonAndGear()throws Exception {
        String[] names={"b5-luka.png","b5-napoleon.png","b4-gear-1.png"};
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b5-luka-enemy.argb.gz")))){
            for(String name:names){StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())))hash.append(String.format("%02x",b&255));assertEquals(hash.toString(),in.readUTF());}
            int[][] frames=new int[4][48*32];for(int[] f:frames)for(int i=0;i<f.length;i++)f[i]=in.readInt();
            assertTrue(TeamIconMatch.distance(frames[0],frames[3])<.055);
            for(int i:new int[]{1,2})assertTrue(TeamIconMatch.distance(frames[0],frames[i])>.055);
            assertTrue(TeamIconMatch.distance(frames[0],new int[48*32])>.055);
        }
    }
}
