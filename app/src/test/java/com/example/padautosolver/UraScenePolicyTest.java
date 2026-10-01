package com.example.padautosolver;
import org.junit.Test;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import static org.junit.Assert.*;
public class UraScenePolicyTest {
    @Test public void replaysActualPhoneOcrWithRegions()throws Exception {
        try(var in=getClass().getResourceAsStream("/preentry-ocr-20261002.json")) {
            var j=new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));var a=j.getJSONArray("items");
            var items=new ArrayList<StagePolicy.Item>();
            for(int i=0;i<a.length();i++){var o=a.getJSONObject(i);items.add(new StagePolicy.Item(o.getString("text"),(float)o.getDouble("x"),(float)o.getDouble("y")));}
            assertTrue(UraScenePolicy.preentry(items,j.getInt("height")));
        }
    }
    @Test public void acceptsObservedPhoneOcrWithoutParticleButRejectsOtherDungeon(){
        var h=new StagePolicy.Item("戻る潜入確認",248,584);
        var t=new StagePolicy.Item("裏魔門守護者",836,918);
        var e=new StagePolicy.Item("挑戦する",606,2159);
        assertTrue(UraScenePolicy.preentry(List.of(h,t,e),2712));
        assertFalse(UraScenePolicy.preentry(List.of(h,new StagePolicy.Item("魔門の守護者",836,918),e),2712));
    }
    @Test public void overlayAndDetailTextCannotBeAnEntryScreen(){
        assertFalse(UraScenePolicy.preentry(List.of(new StagePolicy.Item("潜入確認裏魔門の守護者挑戦する",400,170)),2712));
        assertFalse(UraScenePolicy.preentry(List.of(new StagePolicy.Item("潜入確認",248,584),
                new StagePolicy.Item("裏魔門の守護者",836,918),new StagePolicy.Item("挑戦する",606,200)),2712));
    }
}
