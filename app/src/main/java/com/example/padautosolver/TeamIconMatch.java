package com.example.padautosolver;

/** Empirical template distance, not a probability or a substitute for detail inspection. */
final class TeamIconMatch {
    static double distance(int[] a,int[] b) {
        if(a==null||b==null||a.length==0||a.length!=b.length)return 1;
        long error=0;
        for(int i=0;i<a.length;i++)for(int shift:new int[]{16,8,0})error+=Math.abs(((a[i]>>>shift)&255)-((b[i]>>>shift)&255));
        return error/(a.length*3.0*255);
    }
    static boolean matches(int[] live,int[][] references,int slot) {
        if(slot<0||slot>=6||references==null||references.length!=6)return false;
        double same=distance(live,references[slot]),alternative=1;
        for(int i=0;i<6;i++)if(i!=slot)alternative=Math.min(alternative,distance(live,references[i]));
        return same<=.04&&alternative-same>=.05;
    }
}
