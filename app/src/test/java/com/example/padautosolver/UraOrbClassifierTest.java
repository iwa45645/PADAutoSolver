package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraOrbClassifierTest {
    private int[] pixels(DataInputStream in)throws IOException {int[] p=new int[48*48];for(int i=0;i<p.length;i++)p[i]=in.readInt();return p;}
    @Test public void nineIndependentPoisonBoardsReplayNativeClassifierWithBoundAssetHashes()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b2-poison-oct7.argb.gz")))) {
            int count=in.readInt();int[][] refs=new int[count][];int[] colors=new int[count];
            for(int k=0;k<count;k++) {
                String name=in.readUTF(),expected=in.readUTF();StringBuilder hash=new StringBuilder();
                for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())))hash.append(String.format("%02x",b&255));
                assertEquals(expected,hash.toString());colors[k]=in.readInt();refs[k]=pixels(in);
            }
            int frames=in.readInt();assertEquals(9,frames);
            for(int frame=0;frame<frames;frame++)for(int cell=0;cell<30;cell++) {
                int wanted=in.readInt();int[] sample=pixels(in);UraOrbClassifier.Match result=UraOrbClassifier.classify(sample,refs,colors,10);
                assertEquals("Actual held-out frame "+frame+" cell "+cell,wanted,result.color);
                if(wanted>=6)assertEquals("Special orb cannot enter normal six-color mode",-1,UraOrbClassifier.classify(sample,refs,colors,6).color);
            }
            assertEquals(-1,UraOrbClassifier.classify(new int[48*48],refs,colors,10).color);
        }
    }
    @Test public void ambiguousColorsAndNoReferencesRemainUnknown() {
        int[] p={0xff112233};assertEquals(-1,UraOrbClassifier.classify(p,new int[][]{p,p},new int[]{0,3},6).color);
        assertEquals(-1,UraOrbClassifier.classify(p,new int[0][],new int[0],10).color);
    }
    @Test(expected=IllegalArgumentException.class) public void malformedReferenceCannotAuthorizeColor(){UraOrbClassifier.classify(new int[2],new int[][]{new int[1]},new int[]{0},6);}
}
