package com.example.padautosolver;
import java.util.regex.*;
/** Retry only an already verified adjacent-floor handoff; never authorizes an input. */
final class UraHandoffRetry {
    private String status="";
    private int attempts;
    boolean observeAgain(String next){
        if(!verifiedTransition(next)){reset();return false;}
        if(!next.equals(status)){status=next;attempts=0;}
        return ++attempts<=4;
    }
    static boolean verifiedTransition(String status){
        if("B1_CLEAR_VERIFIED_B2_CURRENT_INSTRUCTION_REQUIRED".equals(status))return true;
        if(status==null)return false;
        Matcher m=Pattern.compile("B([0-9]{1,2})_CLEAR_VERIFIED_B([0-9]{1,2})_CAPTURE_REQUIRED").matcher(status);
        if(!m.matches())return false;
        int floor=Integer.parseInt(m.group(1)),next=Integer.parseInt(m.group(2));
        return floor>=2&&floor<=21&&next==floor+1;
    }
    void reset(){status="";attempts=0;}
}
