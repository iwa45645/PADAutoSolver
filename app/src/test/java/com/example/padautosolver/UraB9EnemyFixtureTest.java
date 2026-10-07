package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraB9EnemyFixtureTest {
    @Test public void independentThreeEnemyRowRejectsB8AndMissingLeftEnemy()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b9-red-red-kappa.argb.gz")))){
            StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/progress-b9-red-red-kappa.png").toPath())))hash.append(String.format("%02x",b&255));assertEquals(hash.toString(),in.readUTF());
            int[][] frames=new int[3][48*32];for(int[] f:frames)for(int i=0;i<f.length;i++)f[i]=in.readInt();
            assertTrue(TeamIconMatch.distance(frames[0],frames[1])<.055);
            assertTrue(TeamIconMatch.distance(frames[0],frames[2])>.055);
            int[] missingLeft=frames[1].clone();for(int y=0;y<32;y++)for(int x=0;x<16;x++)missingLeft[y*48+x]=0xff000000;
            assertTrue(TeamIconMatch.distance(frames[0],missingLeft)>.055);
            assertTrue(TeamIconMatch.distance(frames[0],new int[48*32])>.055);
        }
    }
}
