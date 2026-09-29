package com.example.padautosolver;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class InventoryDeduplicatorTest {
 long[] row(long n){return new long[]{n,n,n,n,n};}
 @Test public void overlapPreservesSeparateSameMonsters(){List<long[]> a=Arrays.asList(row(0),row(-1),row(0xaaaaaaaaaaaaaaaaL));List<long[]> b=Arrays.asList(row(-1),row(0xaaaaaaaaaaaaaaaaL),row(0));assertEquals(2,InventoryDeduplicator.overlap(a,b));}
 @Test(expected=IllegalStateException.class)public void repeatedIdenticalRowsAreAmbiguous(){InventoryDeduplicator.overlap(Arrays.asList(row(0),row(0)),Arrays.asList(row(0),row(0)));}
 @Test(expected=IllegalStateException.class)public void noOverlapStops(){InventoryDeduplicator.overlap(Arrays.asList(row(0)),Arrays.asList(row(-1)));}
 @Test public void endRequiresCountAndMultipleScrolls(){assertFalse(InventoryDeduplicator.endVerified(3,30,707,true,true));assertFalse(InventoryDeduplicator.endVerified(1,707,707,true,true));assertFalse(InventoryDeduplicator.endVerified(3,707,707,false,true));assertTrue(InventoryDeduplicator.endVerified(3,707,707,true,true));}
}
