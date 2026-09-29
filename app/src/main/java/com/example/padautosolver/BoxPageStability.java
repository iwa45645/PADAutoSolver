package com.example.padautosolver;

/** Compare a shared inventory row, even when the detector omits the first visible row. */
final class BoxPageStability {
    static boolean samePosition(int previousStart,float previousTop,int previousCount,
                                int start,float top,int count,int columns,float pitch) {
        if(previousStart<0||start<0||columns<1||pitch<=0)return false;
        int shared=Math.min(previousStart+previousCount,start+count)-Math.max(previousStart,start);
        if(shared<columns||(start-previousStart)%columns!=0)return false;
        float expectedDelta=(start-previousStart)/columns*pitch;
        return Math.abs((top-previousTop)-expectedDelta)<=3f;
    }
}
