package com.example.padautosolver;
import java.io.*;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraPortraitStripFixtureTest {
    @Test public void actualBlinkingCooldownDoesNotHideAChangedTransformation()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b1-portrait-strips.argb.gz")))) {
            assertArrayEquals(UraRunPortraitBands.Y,new int[]{in.readInt(),in.readInt()});
            assertArrayEquals(UraRunPortraitBands.X,new int[]{in.readInt(),in.readInt()});
            assertEquals(UraRunPortraitBands.WIDTH,in.readInt());assertEquals(UraRunPortraitBands.HEIGHT,in.readInt());
            int frames=in.readInt();assertEquals(4,frames);
            int[][] reference=read(in,12);
            for(int frame=0;frame<frames;frame++) {
                int[][] actual=read(in,12);
                for(int band=0;band<12;band++)assertTrue("Native rejected frame "+frame+" same run band "+band,TeamIconMatch.distance(reference[band],actual[band])<=.04);
            }
            for(int a=0;a<6;a++)for(int b=a+1;b<6;b++)assertTrue("Different slots must stay distinct",
                    TeamIconMatch.distance(reference[a*2],reference[b*2])>.04||TeamIconMatch.distance(reference[a*2+1],reference[b*2+1])>.04);
            int[][] beforeEsper=read(in,12);
            assertTrue("Untransformed Esper must not restore its post-transform state",
                    TeamIconMatch.distance(reference[0],beforeEsper[0])>.04||TeamIconMatch.distance(reference[1],beforeEsper[1])>.04);
            int[][] blinking=read(in,2);
            assertTrue("Old strip contains the actual blinking 35",TeamIconMatch.distance(blinking[0],blinking[1])>.04);
        }
    }
    @Test public void actualB3BlinkingCooldownsCannotInvalidateAnUnchangedRun()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b3-portrait-strips.argb.gz")))) {
            assertArrayEquals(UraRunPortraitBands.Y,new int[]{in.readInt(),in.readInt()});
            assertArrayEquals(UraRunPortraitBands.X,new int[]{in.readInt(),in.readInt()});
            assertEquals(UraRunPortraitBands.WIDTH,in.readInt());assertEquals(UraRunPortraitBands.HEIGHT,in.readInt());
            int[][] stopped=read(in,12),current=read(in,12);
            for(int band=0;band<12;band++)assertTrue("B3 band "+band,TeamIconMatch.distance(stopped[band],current[band])<=.04);
            for(int a=0;a<6;a++)for(int b=a+1;b<6;b++)assertTrue(TeamIconMatch.distance(stopped[a*2],stopped[b*2])>.04||TeamIconMatch.distance(stopped[a*2+1],stopped[b*2+1])>.04);
            int[][] blink=read(in,2);assertTrue(TeamIconMatch.distance(blink[0],blink[1])>.04);
        }
    }
    private static int[][] read(DataInputStream in,int count)throws IOException {
        int[][] data=new int[count][48*16];for(int[] pixels:data)for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();return data;
    }
}
