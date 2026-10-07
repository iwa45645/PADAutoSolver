package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraEnhancedCycleFixtureTest {
    private static int[] pixels(DataInputStream in)throws IOException {int[] p=new int[48*48];for(int i=0;i<p.length;i++)p[i]=in.readInt();return p;}
    @Test public void enhancedAnimationKeepsSixColorsDistinctAndRecoversIndependentStop()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b1-enhanced-cycle.argb.gz")))) {
            int count=in.readInt();assertEquals(40,count);int[][] poses=new int[count][];int[] colors=new int[count];
            for(int k=0;k<count;k++) {
                colors[k]=in.readInt();String name=in.readUTF(),expected=in.readUTF();StringBuilder hash=new StringBuilder();
                for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())))hash.append(String.format("%02x",b&255));
                assertEquals(expected,hash.toString());poses[k]=pixels(in);
            }
            int[][] normal=new int[6][];for(int i=0;i<6;i++)normal[i]=pixels(in);
            for(int k=0;k<count;k++) {
                for(int color=0;color<6;color++)if(color!=colors[k])assertTrue("Pose must remain distinct from color "+color,TeamIconMatch.distance(poses[k],normal[color])>.105);
                assertTrue(TeamIconMatch.distance(poses[k],new int[48*48])>.07);
                for(int j=0;j<count;j++)if(colors[j]!=colors[k])assertTrue(TeamIconMatch.distance(poses[k],poses[j])>.105);
            }
            for(int wanted:new int[]{5,3}) {
                int[] sample=pixels(in);double nearest=1,alternative=1;
                for(int k=0;k<count;k++)if(colors[k]==wanted)nearest=Math.min(nearest,TeamIconMatch.distance(poses[k],sample));else alternative=Math.min(alternative,TeamIconMatch.distance(poses[k],sample));
                for(int color=0;color<6;color++)if(color!=wanted)alternative=Math.min(alternative,TeamIconMatch.distance(normal[color],sample));
                assertTrue("Independent prior failed frame must pass existing threshold",nearest<=.07);
                assertTrue("Different color must keep existing margin",alternative-nearest>=.035);
            }
        }
    }
}
