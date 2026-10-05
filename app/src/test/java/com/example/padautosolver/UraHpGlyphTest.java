package com.example.padautosolver;
import org.junit.Test;
import java.io.File;
import java.io.DataInputStream;
import java.util.zip.GZIPInputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import static org.junit.Assert.*;
public class UraHpGlyphTest {
    private int[] pixels(String name)throws Exception {
        File asset=new File("src/main/assets/ura-shura/progress-b22-"+name+"-max-hp.png");
        StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(asset.toPath())))hash.append(String.format("%02x",b&255));
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b22-"+name+"-max-hp.argb.gz")))){
            assertEquals(hash.toString(),in.readUTF());int[] p=new int[in.readInt()];for(int i=0;i<p.length;i++)p[i]=in.readInt();return p;
        }
    }
    @Test public void maximumShapeMatchesBothGameColorsButRejectsAwokenNullMaximum()throws Exception {
        int[] ref=pixels("restored");
        assertTrue(UraHpGlyph.matches(ref,ref,220,54));
        assertTrue(UraHpGlyph.matches(ref,pixels("entry"),220,54));
        assertFalse(UraHpGlyph.matches(ref,pixels("null"),220,54));
        assertFalse(UraHpGlyph.matches(ref,new int[220*54],220,54));
    }
}
