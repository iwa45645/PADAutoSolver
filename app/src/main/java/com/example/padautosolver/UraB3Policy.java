package com.example.padautosolver;
import java.util.*;
import java.util.regex.*;
/** Reviewed B3 branches. Unknown HP/boards cannot authorize a survival turn. */
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
    static boolean recoveryVerified(int[] hp,int verifiedMaximum,Integer sekkaCooldown,Integer esperCooldown) {
        return hp!=null&&verifiedMaximum>=100000&&hp[1]==verifiedMaximum&&hp[0]>=230000&&hp[0]<=hp[1]
            &&Integer.valueOf(4).equals(sekkaCooldown)&&Integer.valueOf(5).equals(esperCooldown);
    }
    static int count(byte[] board,int color){int n=0;if(board!=null)for(byte b:board)if(b==color)n++;return n;}
    static boolean needsRuka(byte[] board){return count(board,5)<3&&count(board,0)+count(board,4)>=3;}
    static boolean factRefreshBoard(byte[] board) {
        if(board==null||board.length!=30||count(board,4)<3)return false;
        for(byte orb:board)if(orb!=1&&orb!=2&&orb!=4)return false;
        return count(board,1)>0&&count(board,2)>0;
    }
    static int hpFillLowerBound(int[] scan,int verifiedMaximum) {
        if(scan==null||scan.length!=1090*3||verifiedMaximum<100000)return 0;
        int filledColumns=0;
        for(int x=0;x<1090;x++) {
            int filled=0;
            for(int y=0;y<3;y++) {
                int p=scan[y*1090+x],r=(p>>16)&255,g=(p>>8)&255,b=p&255;
                if(r>170&&b>100&&r-g>50)filled++;
            }
            if(filled<2)break;filledColumns++;
        }
        return (int)((long)Math.max(0,filledColumns-9)*verifiedMaximum/1090);
    }
}
