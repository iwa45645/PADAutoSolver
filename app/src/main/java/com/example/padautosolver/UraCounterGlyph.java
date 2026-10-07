package com.example.padautosolver;

/** Compares the interior and surrounding space of a reviewed digit, ignoring its one-pixel glow. */
final class UraCounterGlyph {
    static boolean counterInk(int p){
        int r=(p>>16)&255,g=(p>>8)&255,b=p&255;
        return Math.min(r,Math.min(g,b))>235||r>235&&g<150&&b<150&&Math.abs(g-b)<20;
    }
    static boolean matches(boolean[] reference,boolean[] current,int width,int height) {
        if(reference==null||current==null||width<3||height<3||reference.length!=width*height||current.length!=reference.length)return false;
        int errors=0,core=0;
        for(int y=1;y<height-1;y++)for(int x=1;x<width-1;x++) {
            boolean all=true,any=false;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){boolean white=reference[(y+dy)*width+x+dx];all&=white;any|=white;}
            if(all){core++;if(!current[y*width+x])errors++;}
            else if(!any&&current[y*width+x])errors++;
        }
        return core>=100&&errors<10;
    }
}
