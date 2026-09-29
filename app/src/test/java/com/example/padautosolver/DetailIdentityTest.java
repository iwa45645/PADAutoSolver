package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class DetailIdentityTest {
 @Test public void readsOnlyNumberedHeader(){DetailIdentity d=DetailIdentity.parse(Arrays.asList(new StagePolicy.Item("No.6576",200,560),new StagePolicy.Item("煉獄杏寿郎",250,605)),2712);assertNotNull(d);assertEquals(6576,d.id);assertEquals("煉獄杏寿郎",d.name);}
 @Test public void refusesBoxOrBodyNumber(){assertNull(DetailIdentity.parse(Arrays.asList(new StagePolicy.Item("No.6576",200,1600),new StagePolicy.Item("煉獄杏寿郎",250,605)),2712));}
 @Test public void conflictingNumbersAreUnknown(){assertNull(DetailIdentity.parse(Arrays.asList(new StagePolicy.Item("No.6576",200,560),new StagePolicy.Item("No.6577",200,561),new StagePolicy.Item("煉獄杏寿郎",250,605)),2712));}
 @Test public void refusesPartiallyReadNumber(){assertNull(DetailIdentity.parse(Arrays.asList(new StagePolicy.Item("No.19S4",200,560),new StagePolicy.Item("覚醒シヴァ",250,605)),2712));}
 @Test public void preservesUnknownName(){DetailIdentity d=DetailIdentity.parse(Arrays.asList(new StagePolicy.Item("No.3690",200,560)),2712);assertNotNull(d);assertEquals(3690,d.id);assertNull(d.name);}
 @Test public void unresolvedDigitsStayUnknown(){List<StagePolicy.Item> lines=Arrays.asList(new StagePolicy.Item("No.B00",200,560));assertTrue(DetailIdentity.hasHeader(lines,2712));assertNull(DetailIdentity.parse(lines,2712));}
 @Test public void requiresNameAgreement(){assertFalse(new DetailIdentity(1,"オーガ").same(new DetailIdentity(1,"アイスオーガ")));}
 @Test public void doesNotNormalizeNamePunctuation(){DetailIdentity d=DetailIdentity.parse(Arrays.asList(new StagePolicy.Item("No.1",200,560),new StagePolicy.Item("テスト！",250,605)),2712);assertEquals("テスト！",d.name);}
}
