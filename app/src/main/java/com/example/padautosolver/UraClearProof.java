package com.example.padautosolver;
import java.util.List;
/** Completion is an observed result of a pending final-floor attack, never elapsed time or TIPS alone. */
final class UraClearProof {
    private long sequence=-1;private int consecutive;
    static boolean evidence(int floor,boolean pending,String action,List<StagePolicy.Item> items){
        if(floor!=22||!pending||!"ATTACK".equals(action)||UraCombatText.blocked(items))return false;
        boolean clear=false,reward=false,coins=false,experience=false;
        for(var i:items){clear|=i.text.equals("CLEAR");reward|=i.text.equals("クリア報酬");coins|=i.text.contains("獲得コイン");experience|=i.text.contains("獲得経験値");}
        return clear||reward&&coins&&experience;
    }
    boolean observe(boolean evidence,long freshSequence){
        if(freshSequence<=sequence)return false;sequence=freshSequence;
        consecutive=evidence?consecutive+1:0;return consecutive>=2;
    }
}
