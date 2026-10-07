package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraComboCycleFixtureTest {
    @Test public void allTwentyActualAnimationPosesStayDistinctFromOtherColors()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b1-combo-cycle.argb.gz")))) {
            int count=in.readInt();assertEquals(20,count);int[][] water=new int[count][48*48];
            for(int pose=0;pose<count;pose++) {
                String name=in.readUTF(),expected=in.readUTF();StringBuilder hash=new StringBuilder();
                for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())))hash.append(String.format("%02x",b&255));
                assertEquals(expected,hash.toString());for(int i=0;i<water[pose].length;i++)water[pose][i]=in.readInt();
            }
            int[][] other=new int[5][48*48];for(int[] p:other)for(int i=0;i<p.length;i++)p[i]=in.readInt();
            for(int[] w:water){for(int[] c:other)assertTrue(TeamIconMatch.distance(w,c)>.07+.035);assertTrue(TeamIconMatch.distance(w,new int[48*48])>.07);}
            int[] independentStop=new int[48*48];for(int i=0;i<independentStop.length;i++)independentStop[i]=in.readInt();
            double nearest=1,alternative=1;for(int[] w:water)nearest=Math.min(nearest,TeamIconMatch.distance(w,independentStop));
            for(int[] c:other)alternative=Math.min(alternative,TeamIconMatch.distance(c,independentStop));
            assertTrue("Prior native failed frame must now pass unchanged runtime distance",nearest<=.07);
            assertTrue("Other-color margin must remain unambiguous",alternative-nearest>=.035);
        }
    }
}
