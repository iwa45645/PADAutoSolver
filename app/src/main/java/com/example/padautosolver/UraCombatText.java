package com.example.padautosolver;
import java.util.*;
import java.util.regex.*;
/** Observed menu/status evidence. No inferred floor or prospective skill effect. */
final class UraCombatText {
    static String joined(List<StagePolicy.Item> items){StringBuilder b=new StringBuilder();for(var i:items)b.append(i.text);return UraDialogPolicy.clean(b.toString());}
    static int floor(List<StagePolicy.Item> items){
        int found=-1;
        for(var item:items){Matcher m=Pattern.compile("(?:BATTLE|UFLOOR_)([0-9]{1,2})/22").matcher(item.text);if(m.find()){
            int n=Integer.parseInt(m.group(1));if(n<1||n>22||found>0&&found!=n)return -1;found=n;
        }}return found;
    }
    static boolean absorptionsTwoTurns(List<StagePolicy.Item> items){
        String s=joined(items);
        return s.contains("状況確認")&&s.contains(UraDialogPolicy.clean("2ターンの間敵の属性吸収を無効化"))&&s.contains(UraDialogPolicy.clean("2ターンの間敵のダメージ吸収を無効化"));
    }
    static StagePolicy.Item control(List<StagePolicy.Item> items,String name,int minY,int maxY){
        for(var i:items)if(i.text.equals(name)&&i.y>minY&&i.y<maxY)return i;return null;
    }
    static boolean blocked(List<StagePolicy.Item> items){
        String text=joined(items);
        for(String phrase:new String[]{"コンティニュー","ゲームオーバー","GAMEOVER","スタミナが足りません","スタミナを回復","魔法石を使用"})
            if(text.contains(UraDialogPolicy.clean(phrase)))return true;
        return false;
    }
}
