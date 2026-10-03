package com.example.padautosolver;
import java.util.*;
import java.util.regex.*;
/** Reviewed Leonis branch only. Unknown HP/boards cannot authorize a survival turn. */
final class UraB3Policy {
    static boolean allDark(byte[] board) {
        if(board==null||board.length!=30)return false;
        for(byte orb:board)if(orb!=4)return false;return true;
    }
    static int[] hp(List<StagePolicy.Item> text) {
        int[] found=null;
        for(var line:text) {
            Matcher m=Pattern.compile("(?<![0-9])([0-9]{1,8})/([0-9]{1,8})(?![0-9])").matcher(line.text.replace(",",""));
            if(!m.find())continue;
            int current=Integer.parseInt(m.group(1)),max=Integer.parseInt(m.group(2));
            if(max<100000||current>max)continue;
            if(found!=null&&(found[0]!=current||found[1]!=max))return null;
            found=new int[]{current,max};
        }return found;
    }
    static boolean recoveryVerified(int[] hp,Integer sekkaCooldown,Integer esperCooldown) {
        return hp!=null&&hp[1]==611045&&hp[0]>=230000
            &&Integer.valueOf(4).equals(sekkaCooldown)&&Integer.valueOf(5).equals(esperCooldown);
    }
    static int count(byte[] board,int color){int n=0;if(board!=null)for(byte b:board)if(b==color)n++;return n;}
    static boolean needsRuka(byte[] board){return count(board,5)<3&&count(board,0)+count(board,4)>=3;}
}
