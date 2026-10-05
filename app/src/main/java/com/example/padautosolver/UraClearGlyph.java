package com.example.padautosolver;
/** Calibrated CLEAR! lettering; cyan foreground and its holes must both match. */
final class UraClearGlyph {
    private static boolean ink(int p){return ((p>>16)&255)<110&&((p>>8)&255)>100&&(p&255)>170;}
    static boolean matches(int[] reference,int[] current){
        if(reference==null||current==null||reference.length!=840*200||current.length!=reference.length)return false;
        int errors=0,inkCount=0;
        for(int i=0;i<reference.length;i++){boolean a=ink(reference[i]),b=ink(current[i]);if(b)inkCount++;if(a!=b)errors++;}
        return inkCount>=25000&&inkCount<=50000&&errors/(double)reference.length<.025;
    }
}
