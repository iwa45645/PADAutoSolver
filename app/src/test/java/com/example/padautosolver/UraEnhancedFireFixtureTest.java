package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraEnhancedFireFixtureTest {
    private int[][] refs;private int[] colors;
    private int[] pixels(DataInputStream in)throws IOException{int[] p=new int[48*48];for(int i=0;i<p.length;i++)p[i]=in.readInt();return p;}
    private String hash(String file)throws Exception{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/"+file).toPath())))out.append(String.format("%02x",b&255));return out.toString();}
    private String manifestHash()throws Exception{
        byte[] content=new String(Files.readAllBytes(new File("src/main/assets/ura-shura/normal-orbs.json").toPath()),java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(content))out.append(String.format("%02x",b&255));return out.toString();
    }
    private void loadAndReplay()throws Exception{
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b6-enhanced-fire-oct7.argb.gz")))){
            assertEquals(manifestHash(),in.readUTF());
            int count=in.readInt();refs=new int[count][];colors=new int[count];
            for(int i=0;i<count;i++){String name=in.readUTF();assertEquals(hash(name),in.readUTF());colors[i]=in.readInt();refs[i]=pixels(in);}
            int frames=in.readInt();assertEquals(6,frames);
            for(int f=0;f<frames;f++)for(int cell=0;cell<30;cell++){int wanted=in.readInt();assertEquals("Held-out B6 frame "+f+" cell "+cell,wanted,UraOrbClassifier.classify(pixels(in),refs,colors,10).color);}
        }
    }
    @Test public void stoppedAndIndependentEnhancedFireBoardsReplayNativeClassifier()throws Exception{
        loadAndReplay();assertEquals(-1,UraOrbClassifier.classify(new int[48*48],refs,colors,10).color);
    }
    @Test public void newFireReferencesDoNotReclassifyExistingIndependentPoisonBoards()throws Exception{
        loadAndReplay();
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b2-poison-oct7.argb.gz")))){
            int old=in.readInt();for(int i=0;i<old;i++){in.readUTF();in.readUTF();in.readInt();pixels(in);}
            int frames=in.readInt();assertEquals(9,frames);
            for(int f=0;f<frames;f++)for(int cell=0;cell<30;cell++){int wanted=in.readInt();assertEquals("Poison regression frame "+f+" cell "+cell,wanted,UraOrbClassifier.classify(pixels(in),refs,colors,10).color);}
        }
    }
}
