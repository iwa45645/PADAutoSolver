package com.example.padautosolver;
/** Compare the calibrated bar geometry and empty segments independently of attribute hue. */
final class UraEnemyHpBar {
    static double distance(int[] reference,int[] current) {
        if(reference==null||current==null||reference.length!=48*32||current.length!=reference.length)return 1;
        int[] a=new int[reference.length],b=new int[current.length];
        for(int i=0;i<a.length;i++){a[i]=neutral(reference[i]);b[i]=neutral(current[i]);}
        return TeamIconMatch.distance(a,b);
    }
    private static int neutral(int pixel) {
        int r=(pixel>>>16)&255,g=(pixel>>>8)&255,b=pixel&255;
        int saturation=Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b));
        return saturation>=50?0xff808080:pixel;
    }
}
