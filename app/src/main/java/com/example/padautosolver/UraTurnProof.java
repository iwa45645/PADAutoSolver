package com.example.padautosolver;

/** A gesture callback is not a game turn. Require a fresh, named cooldown or actual next floor. */
final class UraTurnProof {
    static boolean cooldown(Integer before,Integer after,boolean named,long dispatchedSequence,long observedSequence) {
        return named&&before!=null&&before>0&&after!=null&&after==before-1&&observedSequence>dispatchedSequence;
    }
    static boolean nextFloor(int floor,int observed,boolean awaitingTurn) {
        return awaitingTurn&&floor>=1&&observed==floor+1;
    }
    static final class FloorConfirmation {
        private int candidate=-1,count;
        void reset(){candidate=-1;count=0;}
        boolean observe(int expected,int observed){
            if(observed!=expected){reset();return false;}
            if(candidate!=observed){candidate=observed;count=1;return false;}
            return ++count>=2;
        }
    }
    private UraTurnProof(){}
}
