package com.example.padautosolver;
import java.io.*;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraPortraitStripFixtureTest {
    @Test public void actualBlinkingCooldownDoesNotHideAChangedTransformation()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b1-portrait-strips.argb.gz")))) {
            assertArrayEquals(UraRunPortraitBands.Y,new int[]{in.readInt(),in.readInt()});
            int[][] reference=read(in,12),actual=read(in,12),beforeEsper=read(in,12);
            for(int band=0;band<12;band++)assertTrue("Actual same run band "+band,TeamIconMatch.distance(reference[band],actual[band])<=.04);
            assertTrue("Untransformed Esper must not restore its post-transform state",
                    TeamIconMatch.distance(reference[0],beforeEsper[0])>.04||TeamIconMatch.distance(reference[1],beforeEsper[1])>.04);
            int[][] blinking=read(in,2);
            assertTrue("Old strip contains the actual blinking 35",TeamIconMatch.distance(blinking[0],blinking[1])>.04);
        }
    }
    private static int[][] read(DataInputStream in,int count)throws IOException {
        int[][] data=new int[count][48*16];for(int[] pixels:data)for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();return data;
    }
}
