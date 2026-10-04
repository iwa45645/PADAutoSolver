package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;
public class UraCounterGlyphTest {
    private boolean[] glyph(String name)throws Exception{
        try(InputStream input=getClass().getResourceAsStream("/counter-"+name+".txt")){
            String bits=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.US_ASCII).replaceAll("\\s","");
            assertEquals(34*43,bits.length());boolean[] pixels=new boolean[bits.length()];
            for(int i=0;i<pixels.length;i++)pixels[i]=bits.charAt(i)=='1';return pixels;
        }
    }
    @Test public void reviewedDigitsRemainRecognizableDuringGreenGlow()throws Exception{
        for(int i=0;i<4;i++)assertTrue("counter "+i,UraCounterGlyph.matches(glyph("entry-"+i),glyph("glow-"+i),34,43));
    }
    @Test public void DifferentDigitsAndMissingCaptureDoNotPass()throws Exception{
        for(int i=0;i<3;i++)for(int j=0;j<3;j++)if(i!=j)assertFalse(UraCounterGlyph.matches(glyph("entry-"+i),glyph("glow-"+j),34,43));
        assertFalse(UraCounterGlyph.matches(glyph("entry-0"),new boolean[34*43],34,43));
        assertFalse(UraCounterGlyph.matches(new boolean[34*43],new boolean[34*43],34,43));
    }
}
