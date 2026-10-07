package com.example.padautosolver;

/** Exact menu numeral pixels supplement a dropped OCR digit; no expected-floor inference. */
final class UraMenuFloorProof {
    static boolean matches(boolean[] reference, boolean[] current) {
        if(reference==null||current==null||reference.length!=175*70||current.length!=reference.length)return false;
        int errors=0,white=0;
        for(int i=0;i<reference.length;i++){if(reference[i])white++;if(reference[i]!=current[i])errors++;}
        return white>200&&errors<30;
    }
    static int resolvedEleven(int literal,int currentFloor,boolean menu,boolean glyphEleven) {
        if(menu&&glyphEleven&&(currentFloor==10||currentFloor==11)&&(literal==-1||literal==1||literal==11))return 11;
        return literal;
    }
}
