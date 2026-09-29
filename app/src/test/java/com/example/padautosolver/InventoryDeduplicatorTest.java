package com.example.padautosolver;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class InventoryDeduplicatorTest {
 long[] row(long n){return new long[]{n,n,n,n,n};}
 @Test public void overlapPreservesSeparateSameMonsters(){List<long[]> a=Arrays.asList(row(0),row(-1),row(0xaaaaaaaaaaaaaaaaL));List<long[]> b=Arrays.asList(row(-1),row(0xaaaaaaaaaaaaaaaaL),row(0));assertEquals(2,InventoryDeduplicator.overlap(a,b));}
 @Test(expected=IllegalStateException.class)public void repeatedIdenticalRowsAreAmbiguous(){InventoryDeduplicator.overlap(Arrays.asList(row(0),row(0)),Arrays.asList(row(0),row(0)));}
 @Test(expected=IllegalStateException.class)public void noOverlapStops(){InventoryDeduplicator.overlap(Arrays.asList(row(0)),Arrays.asList(row(-1)));}
 @Test public void endRequiresCountAndMultipleScrolls(){assertFalse(InventoryDeduplicator.endVerified(3,30,707,true,true));assertFalse(InventoryDeduplicator.endVerified(1,707,707,true,true));assertFalse(InventoryDeduplicator.endVerified(3,707,707,false,true));assertTrue(InventoryDeduplicator.endVerified(3,707,707,true,true));}
 @Test public void realScrolledPageWithAnimatedBadgeUsesFourRowEvidence(){
  List<long[]> previous=Arrays.asList(new long[]{0x30f0fcfa70400000L,0x30f0fcfa70400000L,0x30f0fcfa70400000L,0x123b3938181800f8L,0xacb0b0b0e28262f8L},new long[]{0xeae7623c3c3c3cL,0xc31333397868786cL,0xd0e0c0d808987cf8L,0x30387cf8fc7cL,0xdf3a3a38080000fcL},new long[]{0xf181d3d0a034fcfcL,0x3f5e5f53d3000f8L,0x3f5e5f53d3000f8L,0xfde9d039a20404ecL,0xfde9dc3922843464L},new long[]{0x898787035d174e0cL,0xa9c7ef0d0507260cL,0xe6c28282baa4fcfcL,0x9983dbd52f2fee2cL,0x430585d785c5c54L},new long[]{0xe0e0f0f8f8f060L,0xe6cedfd791303018L,0x7c7060d01c1c5878L,0xf4f874fc444404fcL,0xf4f874fc44c4c444L},new long[]{0xf0c0183a898db8d8L,0x302030f79ffbdaf8L,0x84a6e6f6606020b1L,0x85a1e5ffe38180e4L,0xff43a37b64647061L});
  List<long[]> current=Arrays.asList(new long[]{0xf081f058a434fc58L,0x1f7e4717930b0a0L,0x1f7e4717930b0a0L,0xfdc9b03906647424L,0xedc9b43906a474e4L},new long[]{0x8f8787015f174e0cL,0xe9e72f0d05270e0cL,0xe6c28222b2acfc7cL,0x8b9bdbd52f6e2e2cL,0x430595d785c5454L},new long[]{0x40e0e0f0f8f87020L,0xe6dedfde30303018L,0x746040d21c1c7878L,0xf47074fc44444444L,0xf4747cfc44c444c4L},new long[]{0xd0801a3ac9a9b8d8L,0x103070f7dfdb9af8L,0x84a6e6f26060a0f0L,0x5e5edfee18184e4L,0xfdc3e35564506060L},new long[]{0xebdaec9d380200e0L,0x3c3c3c3878581010L,0x3c3c3c3878581010L,0x3c3c3c3878581010L,0x3c3c3c3878781010L});
  assertFalse(InventoryDeduplicator.same(previous.get(2),current.get(0)));
  assertEquals(4,InventoryDeduplicator.overlap(previous,current));
  Collections.swap(current,1,2);
  try{InventoryDeduplicator.overlap(previous,current);fail("reordered rows must not match");}catch(IllegalStateException expected){}
 }
 @Test public void stabilityAllowsOneAnimatedRowButRejectsScrolledPage(){
  List<long[]> a=Arrays.asList(row(0),row(-1),row(0xaaaaaaaaaaaaaaaaL),row(0x5555555555555555L));
  List<long[]> animated=Arrays.asList(row(0),row(-1),row(0xaaaaaaaaaaaaaaaaL),row(-1));
  assertTrue(InventoryDeduplicator.stableFrame(a,animated));
  assertFalse(InventoryDeduplicator.stableFrame(a,Arrays.asList(a.get(1),a.get(2),a.get(3),a.get(0))));
 }
}
