package com.example.padautosolver;
import java.util.List;

/** OCR must identify the header, target title and entry control in their separate regions. */
final class UraScenePolicy {
    static boolean preentry(List<StagePolicy.Item> items,int height) {
        boolean header=false,title=false,entry=false;
        for(var item:items) {
            float y=item.y/height;
            if(y>=.20f&&y<=.24f&&item.text.contains("潜入確認"))header=true;
            if(y>=.30f&&y<=.37f&&(item.text.contains("裏魔門の守護者")||item.text.contains("裏魔門守護者")))title=true;
            if(y>=.77f&&y<=.84f&&item.text.equals("挑戦する"))entry=true;
        }
        return header&&title&&entry;
    }
}
