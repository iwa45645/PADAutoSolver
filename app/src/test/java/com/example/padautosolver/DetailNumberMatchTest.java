package com.example.padautosolver;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class DetailNumberMatchTest {
    private List<boolean[]> image(String name)throws Exception {
        try(java.io.DataInputStream in=new java.io.DataInputStream(new java.util.zip.GZIPInputStream(getClass().getResourceAsStream("/"+name)))) {
            int[] pixels=new int[171*47];for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();
            return DetailNumberMatch.glyphs(pixels);
        }
    }
    @Test public void realPhoneNumberMatchesDespiteOcrFailureAndRejectsOtherMonster()throws Exception {
        List<boolean[]> reference=image("number-reference-13392.argb.gz"),live=image("number-live-13392.argb.gz");
        assertEquals(5,reference.size());assertEquals(5,live.size());
        assertTrue(DetailNumberMatch.sameDigits(live,reference,5));
        assertFalse(DetailNumberMatch.sameDigits(live,image("number-reference-14072.argb.gz"),5));
    }
    @Test public void comparesEveryDigitAndRefusesLengthMismatch() {
        boolean[] a=new boolean[864],b=new boolean[864];
        Arrays.fill(a,true);Arrays.fill(b,true);
        assertTrue(DetailNumberMatch.sameDigits(Arrays.asList(a,b),Arrays.asList(a,b),2));
        assertFalse(DetailNumberMatch.sameDigits(Arrays.asList(a),Arrays.asList(a,b),2));
        boolean[] wrong=b.clone();for(int i=0;i<100;i++)wrong[i]=false;
        assertFalse(DetailNumberMatch.sameDigits(Arrays.asList(a,wrong),Arrays.asList(a,b),2));
    }
    @Test public void refusesEmptyOrNonGlyphAreas() {
        assertTrue(DetailNumberMatch.glyphs(new int[171*47]).isEmpty());
        int[] image=new int[171*47];Arrays.fill(image,0xffffffff);
        assertTrue(DetailNumberMatch.glyphs(image).isEmpty());
        assertFalse(DetailNumberMatch.sameDigits(Collections.emptyList(),Collections.emptyList(),0));
    }
}
