package com.example.padautosolver;

import org.junit.Test;
import static org.junit.Assert.*;

public class PuzzleGoalTest {
    private byte[] board(String... rows){
        byte[] b=new byte[rows.length*rows[0].length()];int n=0;
        for(String r:rows)for(char c:r.toCharArray())b[n++]=(byte)(c-'0');return b;
    }
    @Test public void distinctWaterMatchesAndHealRankBeforeExtraCombos(){
        var s=PuzzleSolver.analyze(board("333012","012450","123333","501245","555120"),6,5);
        assertEquals(2,s.colorCombos[3]);assertTrue(PuzzleGoal.waterAndHeal().satisfied(s,30));
        var many=new PuzzleSolver.MatchStats();many.combos=10;many.matched=30;
        assertTrue(PuzzleGoal.water().score(s,30)>PuzzleGoal.water().score(many,30));
    }
    @Test public void adjoiningWaterRowsAreOneCombo(){
        var s=PuzzleSolver.analyze(board("333012","333450","012451","125012","450125"),6,5);
        assertEquals(1,s.colorCombos[3]);assertFalse(PuzzleGoal.water().satisfied(s,30));
    }
    @Test public void esperRequiresActualMatchedComboDrop(){
        byte[] b=board("333012","012450","123333","501245","555120");
        assertFalse(PuzzleGoal.esperMion().satisfied(PuzzleSolver.analyze(b,6,5),30));
        assertTrue(PuzzleGoal.esperMion().satisfied(PuzzleSolver.analyze(b,6,5,1L),30));
        assertFalse(PuzzleGoal.esperMion().satisfied(PuzzleSolver.analyze(b,6,5,1L<<5),30));
    }
    @Test public void solveTracksComboDropThroughSwapsAndReplaysRoute(){
        byte[] b=board("330312","012450","123333","501245","555120");
        long mask=1L<<3;
        var result=PuzzleSolver.solve(b,6,5,4,300,0,PuzzleGoal.esperMion(),mask);
        for(int i=1;i<result.path.size();i++) {
            int from=result.path.get(i-1),to=result.path.get(i);
            assertEquals(1,Math.abs(from%6-to%6)+Math.abs(from/6-to/6));
            byte tmp=b[from];b[from]=b[to];b[to]=tmp;
            if(((mask>>>from)&1)!=((mask>>>to)&1))mask^=(1L<<from)|(1L<<to);
        }
        var s=PuzzleSolver.analyze(b,6,5,mask);assertTrue(result.goalSatisfied);
        assertTrue(PuzzleGoal.esperMion().satisfied(s,30));assertEquals(s.combos,result.combos);
    }
    @Test public void unsupportedLuciferInstructionAndUnknownCellAbort(){
        assertThrows(IllegalArgumentException.class,()->new PuzzleGoal(PuzzleGoal.Type.SPECIAL_LUCIFER,3,0));
        byte[] b=board("012012","120120","201201","012012","120120");b[10]=-1;
        assertThrows(IllegalArgumentException.class,()->PuzzleSolver.solve(b,6,5,5,50,0,PuzzleGoal.water()));
    }
    @Test public void fullClearExactComboNoComboAndShapes(){
        byte[] all=board("000111","222333","444555","000111","222333");
        var s=PuzzleSolver.analyze(all,6,5);assertTrue(new PuzzleGoal(PuzzleGoal.Type.FULL_CLEAR,0,0).satisfied(s,30));
        assertTrue(new PuzzleGoal(PuzzleGoal.Type.EXACT_COMBO,0,10).satisfied(s,30));
        assertFalse(new PuzzleGoal(PuzzleGoal.Type.NO_COMBO,0,0).satisfied(s,30));
        var square=PuzzleSolver.analyze(board("333012","333120","333201","012012","120120"),6,5);
        assertTrue(new PuzzleGoal(PuzzleGoal.Type.VDP,3,0).satisfied(square,30));
        var l=PuzzleSolver.analyze(board("300012","312450","333201","012012","120120"),6,5);
        assertTrue(new PuzzleGoal(PuzzleGoal.Type.L_SHAPE,3,0).satisfied(l,30));
        var cross=PuzzleSolver.analyze(board("030012","333450","031201","012012","120120"),6,5);
        assertTrue(new PuzzleGoal(PuzzleGoal.Type.CROSS,3,0).satisfied(cross,30));
    }
}
