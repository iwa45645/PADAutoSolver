package com.example.padautosolver;
import java.util.List;
/** Completion is an observed result of a pending final-floor attack, never elapsed time or TIPS alone. */
final class UraClearProof {
    private long sequence=-1;private int consecutive;
    static boolean evidence(int floor,boolean pending,String action,List<StagePolicy.Item> items){
        if(floor!=22||!pending||!"ATTACK".equals(action)||UraCombatText.blocked(items))return false;
        boolean clear=false;
        for(var i:items)clear|=i.text.equals("CLEAR");
        return clear||reward(items);
    }
    static boolean terminalResume(int floor,int operation,boolean awaiting,String action,String dispatch,boolean clearLogo){
        return floor==22&&operation==5&&awaiting&&"ATTACK".equals(action)&&("ACKNOWLEDGED".equals(dispatch)||"DISPATCH_INTENT".equals(dispatch))&&clearLogo;
    }
    static boolean completedResume(int phase,String dispatch,boolean actualResultScene){
        return phase>=23&&phase<=25&&"VERIFIED".equals(dispatch)&&actualResultScene;
    }
    static boolean reward(List<StagePolicy.Item> items){
        if(UraCombatText.blocked(items))return false;
        String text=UraCombatText.joined(items);
        if(!text.contains("クリア報酬")||!text.contains("獲得コイン"))return false;
        if(text.contains("獲得経験値")||text.contains("獲得EXP"))return true;
        // Actual reward font reads EHP. Accept only this dungeon's complete, aligned numeric reward rows.
        return text.contains("裏魔門の守護者")&&text.contains("経験値ストック")&&
            amount(items,"獲得コイン")>0&&amount(items,"経験値ストック")>=0&&amount(items,"獲得EHP")>0;
    }
    static long amount(List<StagePolicy.Item> items,String label){
        for(var row:items)if(row.text.equals(label))for(var value:items)
            if(value.x>row.x+200&&value.x>700&&Math.abs(value.y-row.y)<30&&value.text.matches("[0-9]+(,[0-9]{3})*"))
                try{return Long.parseLong(value.text.replace(",",""));}catch(NumberFormatException ignored){}
        return -1;
    }
    static boolean finalOutcomePhase(int phase){return phase==0||phase==1||phase==10;}
    boolean observe(boolean evidence,long freshSequence){
        if(freshSequence<=sequence)return false;sequence=freshSequence;
        consecutive=evidence?consecutive+1:0;return consecutive>=2;
    }
}
