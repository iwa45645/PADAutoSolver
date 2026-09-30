package com.example.padautosolver;
import org.json.*;import org.junit.Test;import static org.junit.Assert.*;
public class InventorySearchTest {
 @Test public void fullWidthKeyboardNumbersFindExactCandidate()throws Exception{JSONObject item=new JSONObject("{monsterId:null,name:null,observedIdentity:{monsterId:12347,name:'メノア',readerVersion:2}}");assertTrue(InventorySearch.matches("1２３４７",355,item,null));assertFalse(InventorySearch.matches("2",355,item,null));assertFalse(InventorySearch.matches("123",355,item,null));}
 @Test public void indexedCopiesAreDistinct()throws Exception{JSONObject item=new JSONObject();assertTrue(InventorySearch.matches("＃３５５",355,item,null));assertFalse(InventorySearch.matches("＃３５５",356,item,null));}
 @Test public void searchesReviewedNameWithoutConfirming()throws Exception{JSONObject item=new JSONObject("{monsterId:null,name:null}");JSONObject review=new JSONObject("{monsterId:12347,name:'ロボット研究部・メノア'}");assertTrue(InventorySearch.matches("ﾒﾉｱ",355,item,review));assertTrue(item.isNull("monsterId"));assertFalse(InventorySearch.matches("未所持",355,item,review));}
}
