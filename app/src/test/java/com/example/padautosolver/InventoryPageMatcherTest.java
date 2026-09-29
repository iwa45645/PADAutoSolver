package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class InventoryPageMatcherTest {
 private long[] source(){Random r=new Random(314159);long[] a=new long[40];for(int i=0;i<a.length;i++)a[i]=r.nextLong();return a;}
 @Test public void keepsCopiesAtTheirPositions(){long[] a=source();a[13]=a[12];assertEquals(10,InventoryPageMatcher.locate(a,Arrays.copyOfRange(a,10,30)));}
 @Test public void toleratesIconOverlayNoise(){long[] a=source(),p=Arrays.copyOfRange(a,10,30);p[3]^=0xff;assertEquals(10,InventoryPageMatcher.locate(a,p));}
 @Test public void rejectsReorderedPage(){long[] a=source(),p=Arrays.copyOfRange(a,10,30);long t=p[0];p[0]=p[1];p[1]=t;assertEquals(-1,InventoryPageMatcher.locate(a,p));}
 @Test public void refusesAmbiguousCopies(){assertEquals(-1,InventoryPageMatcher.locate(new long[40],new long[20]));}
 @Test public void matchesObservedAnimationVariants(){long[] a=source(),page=Arrays.copyOfRange(a,10,30);long[][] variants=new long[a.length][];for(int i=0;i<a.length;i++)variants[i]=new long[]{a[i]};page[5]^=0xffffff;variants[15]=new long[]{a[15],page[5]};assertEquals(10,InventoryPageMatcher.locate(variants,page));}
 @Test public void refusesMissingPage(){assertEquals(-1,InventoryPageMatcher.locate(source(),new long[0]));}
}
