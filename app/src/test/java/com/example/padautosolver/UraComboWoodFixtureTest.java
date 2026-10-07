package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraComboWoodFixtureTest {
    private String hash(byte[] bytes)throws Exception{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))out.append(String.format("%02x",b&255));return out.toString();}
    private int[] pixels(DataInputStream in)throws IOException{int[] p=new int[48*48];for(int i=0;i<p.length;i++)p[i]=in.readInt();return p;}
    @Test public void independentComboWoodAndFullBoardsReplayCurrentNativeReferences()throws Exception{
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/stability-b7-combo-wood-oct7.argb.gz")))){
            byte[] manifest=Files.readAllBytes(new File("src/main/assets/ura-shura/normal-orbs.json").toPath());
            assertEquals(hash(new String(manifest,java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)),in.readUTF());
            int count=in.readInt();int[][] refs=new int[count][];int[] colors=new int[count];
            for(int k=0;k<count;k++){String name=in.readUTF();assertEquals(hash(Files.readAllBytes(new File("src/main/assets/ura-shura/"+name).toPath())),in.readUTF());colors[k]=in.readInt();refs[k]=pixels(in);}
            int frames=in.readInt();assertEquals(6,frames);
            for(int f=0;f<frames;f++)for(int cell=0;cell<30;cell++){int color=in.readInt();assertEquals("Held-out B7 frame "+f+" cell "+cell,color,UraOrbClassifier.classify(pixels(in),refs,colors,10).color);}
            assertEquals(-1,UraOrbClassifier.classify(new int[48*48],refs,colors,10).color);
        }
    }
}
