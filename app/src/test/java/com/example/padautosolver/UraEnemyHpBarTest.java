package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraEnemyHpBarTest {
    @Test public void actualIndependentYellowRedFullBarsRejectHalfBarsAndMissingEnemy()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b19-attribute-bars.argb.gz")))){
            for(String name:new String[]{"progress-b19-entry-hp.png","progress-b19-half-hp.png"}){
                StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())))hash.append(String.format("%02x",b&255));assertEquals(hash.toString(),in.readUTF());
            }
            int[][] frames=new int[4][48*32];for(int[] f:frames)for(int i=0;i<f.length;i++)f[i]=in.readInt();
            assertTrue(TeamIconMatch.distance(frames[0],frames[3])>.025);
            assertTrue(UraEnemyHpBar.distance(frames[0],frames[2])<.025);
            assertTrue(UraEnemyHpBar.distance(frames[0],frames[3])<.025);
            assertTrue(UraEnemyHpBar.distance(frames[0],frames[1])>.025);
            assertTrue(UraEnemyHpBar.distance(frames[1],frames[3])>.025);
            int[] missing=frames[3].clone();for(int y=0;y<32;y++)for(int x=24;x<48;x++)missing[y*48+x]=0xff000000;
            assertTrue(UraEnemyHpBar.distance(frames[0],missing)>.025);
            assertTrue(UraEnemyHpBar.distance(frames[0],new int[48*32])>.025);
        }
    }
    @Test public void uncalibratedOrAbsentBarsCannotProveHp(){
        assertEquals(1,UraEnemyHpBar.distance(null,new int[48*32]),0);
        assertEquals(1,UraEnemyHpBar.distance(new int[48*32],null),0);
        assertEquals(1,UraEnemyHpBar.distance(new int[48*32],new int[48*31]),0);
        assertEquals(1,UraEnemyHpBar.distance(new int[1],new int[1]),0);
    }
}
