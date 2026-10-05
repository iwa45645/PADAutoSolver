package com.example.padautosolver;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraB2SpecialOrbAnimationTest {
    @Test public void independentLockedWoodAndPoisonFramesRetainEveryOtherColorMargin()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b2-special-orbs.argb.gz")))) {
            int n=in.readInt();int[] colors=new int[n];int[][] references=new int[n][];
            for(int r=0;r<n;r++) {
                String name=in.readUTF(),expectedHash=in.readUTF();StringBuilder hash=new StringBuilder();
                for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())))hash.append(String.format("%02x",b&255));
                assertEquals(expectedHash,hash.toString());colors[r]=in.readInt();references[r]=read(in);
            }
            int observations=in.readInt();assertEquals(27,observations);
            for(int frame=0;frame<observations;frame++) {
                int expected=in.readInt();int[] sample=read(in);double same=1,other=1;
                for(int r=0;r<n;r++){double d=TeamIconMatch.distance(sample,references[r]);if(colors[r]==expected)same=Math.min(same,d);else other=Math.min(other,d);}
                assertTrue("frame="+frame,same<=.07);assertTrue("margin frame="+frame,other-same>=.035);
            }
            for(int[] reference:references)assertTrue(TeamIconMatch.distance(new int[48*48],reference)>.07);
        }
    }
    private static int[] read(DataInputStream in)throws IOException{int[] pixels=new int[48*48];for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();return pixels;}
}
