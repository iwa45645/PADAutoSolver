package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraComboLightFixtureTest {
    @Test public void twoActualComboLightPosesRejectTheOtherFiveColors()throws Exception {
        for(int pose=0;pose<2;pose++) {
            String name="stability-b1-combo-light-"+pose+".png";StringBuilder hash=new StringBuilder();
            for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())))hash.append(String.format("%02x",b&255));
            try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/"+name+".argb.gz")))) {
                assertEquals(hash.toString(),in.readUTF());int[][] a=new int[6][48*48];for(int[] p:a)for(int i=0;i<p.length;i++)p[i]=in.readInt();
                for(int c=1;c<6;c++)assertTrue(name+" vs "+c,TeamIconMatch.distance(a[0],a[c])>.07+.035);
                assertTrue(TeamIconMatch.distance(a[0],new int[48*48])>.07);
            }
        }
    }
}
