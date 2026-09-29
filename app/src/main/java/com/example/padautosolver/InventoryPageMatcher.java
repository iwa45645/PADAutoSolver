package com.example.padautosolver;

/** Contiguous page alignment preserves separate copies. Ambiguous pages are rejected. */
final class InventoryPageMatcher {
    static int locate(long[] reference,long[] page) {
        long[][] variants=new long[reference.length][];
        for(int i=0;i<reference.length;i++)variants[i]=new long[]{reference[i]};
        return locate(variants,page);
    }
    static int locate(long[][] reference,long[] page) {
        if(page.length<5||page.length>reference.length)return -1;
        int found=-1;
        for(int at=0;at<=reference.length-page.length;at++) {
            int total=0;boolean valid=true;
            for(int i=0;i<page.length;i++) {
                int distance=64;for(long variant:reference[at+i])distance=Math.min(distance,Long.bitCount(variant^page[i]));
                if(distance>16){valid=false;break;}total+=distance;
            }
            if(valid&&total<=page.length*10) {
                if(found>=0)return -1;
                found=at;
            }
        }
        return found;
    }
}
