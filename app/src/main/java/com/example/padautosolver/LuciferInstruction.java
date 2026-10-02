package com.example.padautosolver;
import java.util.*;
/** Only literal, observed instructions authorize a goal. Trial order alone never does. */
enum LuciferInstruction {
    POISON, ZERO, ALL;
    static LuciferInstruction read(List<StagePolicy.Item> lines) {
        LuciferInstruction found=null;
        for(var line:lines) {
            if(line.y<570||line.y>800)continue;
            String text=UraDialogPolicy.clean(line.text);LuciferInstruction next=null;
            if(text.contains(UraDialogPolicy.clean("全ての毒を消してみるがいい")))next=POISON;
            if(text.contains(UraDialogPolicy.clean("次はコンボ禁止だ")))next=ZERO;
            if(text.contains(UraDialogPolicy.clean("全てのドロップを消してみるがいい")))next=ALL;
            if(text.contains(UraDialogPolicy.clean("水、光を全て消すがいい")))next=ALL;
            if(next!=null){if(found!=null&&found!=next)return null;found=next;}
        }return found;
    }
    PuzzleGoal goal(byte[] board) {
        if(this==ZERO)return new PuzzleGoal(PuzzleGoal.Type.NO_COMBO,0,0);
        if(this==ALL)return new PuzzleGoal(PuzzleGoal.Type.FULL_CLEAR,0,0);
        int count=0;for(byte cell:board)if(cell==7)count++;
        return PuzzleGoal.clearColor(7,count);
    }
}
