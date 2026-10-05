package com.example.padautosolver;
/** Reviewed maximum-HP glyph shape, independent of its green/yellow game color. */
final class UraHpGlyph {
    private static boolean ink(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return b<150&&(r>200&&g>145||g>180&&r<150);}
    static boolean matches(int[] reference,int[] current,int width,int height){
        if(reference==null||current==null||reference.length!=width*height||current.length!=reference.length)return false;
        boolean[] a=new boolean[reference.length],b=new boolean[current.length];
        for(int i=0;i<a.length;i++){a[i]=ink(reference[i]);b[i]=ink(current[i]);}
        return UraCounterGlyph.matches(a,b,width,height);
    }
}
