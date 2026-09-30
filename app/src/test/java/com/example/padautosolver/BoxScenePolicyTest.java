package com.example.padautosolver;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class BoxScenePolicyTest {
 @org.junit.Test public void acceptsObservedMissingKanaOnlyWithAllAndRejectsSale(){
  org.junit.Assert.assertTrue(BoxScenePolicy.isBox(java.util.Arrays.asList(new StagePolicy.Item("戻るモスターBOH",400,585),new StagePolicy.Item("ALL未設定",100,725)),2712));
  org.junit.Assert.assertFalse(BoxScenePolicy.isBox(java.util.Arrays.asList(new StagePolicy.Item("戻るモスターBOH",400,585)),2712));
  org.junit.Assert.assertFalse(BoxScenePolicy.isBox(java.util.Arrays.asList(new StagePolicy.Item("売却モスターBOH",400,585),new StagePolicy.Item("ALL",100,725)),2712));
 }
    private StagePolicy.Item item(String text, float y) { return new StagePolicy.Item(text, 100, y); }
    @Test public void acceptsObservedHeaderOcrVariants() {
        for (String title : new String[]{"モンスターBOX", "戻るモンスターBOR", "モンスターBOH"})
            assertTrue(BoxScenePolicy.isBox(List.of(item(title, 580), item("ALL", 750)), 2712));
    }
    @Test public void ownOverlayCannotMakeAnotherScreenLookLikeBox() {
        assertFalse(BoxScenePolicy.isBox(List.of(item("通常のモンスターBOX（ALL）以外のため停止", 250), item("ALL",750)),2712));
    }
    @Test public void acceptsAdjacentTabMergedByOcr() {
        assertTrue(BoxScenePolicy.isBox(List.of(item("モンスターBOX",580),item("ALL未設定",750)),2712));
    }
    @Test public void rejectsSaleAndMissingAll() {
        assertFalse(BoxScenePolicy.isBox(List.of(item("モンスター売却",580),item("ALL",750)),2712));
        assertFalse(BoxScenePolicy.isBox(List.of(item("モンスターBOX",580)),2712));
    }
}
