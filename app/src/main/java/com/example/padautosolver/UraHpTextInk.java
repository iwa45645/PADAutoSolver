package com.example.padautosolver;
/** Literal HP text colors, excluding the pink fill behind those digits. No HP values are inferred. */
final class UraHpTextInk {
    static boolean isInk(int p){
        int r=(p>>16)&255,g=(p>>8)&255,b=p&255;
        return Math.min(r,Math.min(g,b))>190||r>200&&g>180&&b<130
            ||g>190&&r<180&&b<180&&g-r>70&&g-b>70;
    }
    private UraHpTextInk(){}
}
