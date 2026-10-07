package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraMenuFloorProofTest {
    @Test public void independentActualElevenRejectsOtherFloorAndMissingNumeral()throws Exception {
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/menu-floor11.mask.gz")))){
            StringBuilder hash=new StringBuilder();
            for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/menu-floor11.png").toPath())))hash.append(String.format("%02x",b&255));
            assertEquals(hash.toString(),in.readUTF());
            boolean[][] masks=new boolean[4][175*70];
            for(boolean[] m:masks)for(int i=0;i<m.length;i++)m[i]=in.readByte()==1;
            assertTrue(UraMenuFloorProof.matches(masks[0],masks[1]));
            assertFalse(UraMenuFloorProof.matches(masks[0],masks[2]));
            assertFalse(UraMenuFloorProof.matches(masks[0],masks[3]));
            assertFalse(UraMenuFloorProof.matches(new boolean[175*70],new boolean[175*70]));
        }
    }
    @Test public void onlyVerifiedElevenAtReviewedMenuCanResolveDroppedDigit() {
        assertEquals(11,UraMenuFloorProof.resolvedEleven(1,10,true,true));
        assertEquals(11,UraMenuFloorProof.resolvedEleven(-1,11,true,true));
        assertEquals(1,UraMenuFloorProof.resolvedEleven(1,10,true,false));
        assertEquals(1,UraMenuFloorProof.resolvedEleven(1,10,false,true));
        assertEquals(1,UraMenuFloorProof.resolvedEleven(1,1,true,true));
        assertEquals(2,UraMenuFloorProof.resolvedEleven(2,10,true,true));
    }
}
