package com.example.padautosolver;

import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;

public class UraLockedWaterAnimationTest {
    @Test public void independentAnimationFramesAndPreviousStopMatchWaterWithUnchangedLimits()throws Exception {
        verifyAnimation("locked-water",8);
    }
    @Test public void enhancedLockedWaterAfterYukineMatchesIndependentFramesWithUnchangedLimits()throws Exception {
        verifyAnimation("enhanced-locked-water",20);
    }
    private void verifyAnimation(String variant,int expectedReferences)throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b1-"+variant+"-animation.argb.gz")))) {
            int references=in.readInt();assertEquals(expectedReferences,references);
            for(int r=0;r<references;r++) {
                File image=new File("src/main/assets/ura-shura/stability-b1-"+variant+"-pose-"+r+".png");
                StringBuilder hash=new StringBuilder();
                for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image.toPath())))hash.append(String.format("%02x",b&255));
                assertEquals(hash.toString(),in.readUTF());
            }
            int observations=in.readInt();assertEquals(9,observations);
            int[][] water=read(in,references),heldOut=read(in,observations),other=read(in,5);
            for(int frame=0;frame<heldOut.length;frame++) {
                double same=nearest(heldOut[frame],water),alternative=nearest(heldOut[frame],other);
                assertTrue("Held-out frame "+frame+" must recognize water",same<=.07);
                assertTrue("Held-out frame "+frame+" must retain color margin",alternative-same>=.035);
            }
            assertTrue(nearest(new int[48*48],water)>.07);
        }
    }
    private static int[][] read(DataInputStream in,int count)throws IOException {
        int[][] data=new int[count][48*48];
        for(int[] pixels:data)for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();
        return data;
    }
    private static double nearest(int[] frame,int[][] refs) {
        double distance=1;for(int[] reference:refs)distance=Math.min(distance,TeamIconMatch.distance(frame,reference));return distance;
    }
}
