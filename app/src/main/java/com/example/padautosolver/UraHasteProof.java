package com.example.padautosolver;

/** A fresh base modal can independently identify a poorly read held tooltip. */
final class UraHasteProof {
    static boolean modalIdentity(UraDialogPolicy.Read read,String expected,boolean backVerified) {
        return read!=null&&read.layer==1&&read.named(expected)&&backVerified;
    }
    static boolean matches(Integer before,Integer after,int amount,boolean heldIdentity,
            UraDialogPolicy.Read read,String expected,boolean backVerified) {
        return (heldIdentity||modalIdentity(read,expected,backVerified))
                &&before!=null&&after!=null&&amount>0&&before>=amount&&after==before-amount;
    }
}
