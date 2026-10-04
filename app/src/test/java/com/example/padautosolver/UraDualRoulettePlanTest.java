package com.example.padautosolver;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class UraDualRoulettePlanTest {
    private byte[] stable(){byte[] b=new byte[30];for(int p:new int[]{1,2,3,8,14,24,25,26})b[p]=3;for(int p:new int[]{21,22,23})b[p]=5;b[12]=-1;b[17]=-1;return b;}
    @Test public void EveryColorPairPreservesFirstWaveTWaterAndHeal(){
        byte[] b=stable();UraDualRoulettePlan p=new UraDualRoulettePlan(b,UraDualRoulettePlan.MASK,List.of(0,6,0),100);
        for(byte a=0;a<10;a++)for(byte c=0;c<10;c++){
            b[12]=a;b[17]=c;assertTrue(p.current(b,UraDualRoulettePlan.MASK,101));
            byte[] result=RoulettePlan.replay(b,UraDualRoulettePlan.MASK,p.path);PuzzleSolver.MatchStats s=PuzzleSolver.firstWave(result,6,5);
            assertTrue(s.firstTShapes[3]>0&&s.colorCombos[3]>=2&&s.colorCombos[5]>0);
        }
        b[0]=1;assertFalse(p.current(b,UraDualRoulettePlan.MASK,101));assertFalse(p.current(p.source,1L<<12,101));assertFalse(p.current(p.source,UraDualRoulettePlan.MASK,15100));
    }
    @Test public void JoinedTAndVisitedSpinnerAreRejected(){
        byte[] b=stable();b[13]=3;
        try{new UraDualRoulettePlan(b,UraDualRoulettePlan.MASK,List.of(0,6,0),0);fail();}catch(IllegalArgumentException expected){}
        try{new UraDualRoulettePlan(stable(),UraDualRoulettePlan.MASK,List.of(6,12),0);fail();}catch(IllegalArgumentException expected){}
        b=stable();b[0]=-1;try{UraDualRoulettePlan.validate(b,UraDualRoulettePlan.MASK);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void ReviewedEntryAfterMionCanBeSolvedWithoutTouchingEitherSpinner(){
        byte[] b={0,3,3,5,4,1,2,4,5,5,0,1,-1,0,4,2,0,-1,1,3,3,2,4,3,5,5,3,5,3,2};
        for(int i=0;i<30;i++)if(b[i]==0||b[i]==1)b[i]=3;
        UraDualRoulettePlan p=UraDualRoulettePlan.solve(b,UraDualRoulettePlan.MASK,44,350,8000,0);
        assertFalse(p.path.contains(12));assertFalse(p.path.contains(17));assertTrue(p.path.size()<=45);
    }
    @Test public void ChargeNeedsLiveShieldAndKnownStableOrbs(){
        byte[] b=stable();assertTrue(UraProgressPolicy.safeB17Charge(b,UraDualRoulettePlan.MASK,230000,17,27,27,27,1));
        assertFalse(UraProgressPolicy.safeB17Charge(b,UraDualRoulettePlan.MASK,229999,17,27,27,27,1));
        assertFalse(UraProgressPolicy.safeB17Charge(b,1L<<12,230000,17,27,27,27,1));
        assertFalse(UraProgressPolicy.safeB17Charge(b,UraDualRoulettePlan.MASK,230000,17,31,27,31,1));
        b[0]=7;assertFalse(UraProgressPolicy.safeB17Charge(b,UraDualRoulettePlan.MASK,230000,17,27,27,27,1));
    }
    @Test public void RealBoardAfterChargingAndUnlockingAlsoHasAProvenRoute(){
        byte[] b={1,5,5,2,4,1,0,0,4,5,4,1,-1,3,3,5,0,-1,2,4,5,2,0,3,1,0,4,2,4,2};
        for(int i=0;i<30;i++)if(b[i]==0||b[i]==1)b[i]=3;
        UraDualRoulettePlan p=UraDualRoulettePlan.solve(b,UraDualRoulettePlan.MASK,44,350,8000,0);
        assertFalse(p.path.contains(12));assertFalse(p.path.contains(17));
    }
    @Test public void B18BoardRequiresKeepingASeparateWaterMatchWhileMakingT(){
        byte[] b={3,2,3,2,4,3,3,4,3,5,5,3,-1,3,2,3,2,-1,2,3,5,3,4,3,3,4,5,2,5,3};
        UraDualRoulettePlan p=UraDualRoulettePlan.solve(b,UraDualRoulettePlan.MASK,44,350,8000,0);
        assertFalse(p.path.contains(12));assertFalse(p.path.contains(17));
    }
    @Test public void B19HalfHpBoardCanFinishWithoutChargingOrVisitingSpinners(){
        byte[] b={3,3,2,3,1,4,2,4,1,1,3,2,-1,2,0,5,1,-1,2,4,5,2,2,4,2,4,2,5,4,2};
        for(int i=0;i<30;i++)if(b[i]==0||b[i]==1)b[i]=3;
        UraDualRoulettePlan p=UraDualRoulettePlan.solve(b,UraDualRoulettePlan.MASK,44,2000,8000,0);
        assertFalse(p.path.contains(12));assertFalse(p.path.contains(17));
    }
}
