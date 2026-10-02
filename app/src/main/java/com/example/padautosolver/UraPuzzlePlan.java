package com.example.padautosolver;
import java.util.*;
/** Plans are immutable and replayed against their source board before dispatch. */
final class UraPuzzlePlan {
    private final byte[] board;
    final List<Integer> path;
    final PuzzleSolver.MatchStats stats;
    final PuzzleGoal goal;
    final long plannedAt;
    UraPuzzlePlan(byte[] source,PuzzleSolver.Result result,PuzzleGoal goal,long plannedAt) {
        if(source==null||source.length!=30||result==null||goal==null)throw new IllegalArgumentException("Invalid plan input");
        for(byte orb:source)if(orb<0||orb>9)throw new IllegalArgumentException("Unknown orb");
        this.board=source.clone();this.path=Collections.unmodifiableList(new ArrayList<>(result.path));this.goal=goal;this.plannedAt=plannedAt;
        if(goal.type==PuzzleGoal.Type.CLEAR_COLOR) {
            int count=0;for(byte orb:source)if(orb==goal.color)count++;
            if(count!=goal.exactCombos)throw new IllegalArgumentException("Target count differs from source board");
        }
        byte[] replay=source.clone();
        if(path.isEmpty())throw new IllegalArgumentException("Empty route");
        for(int i=0;i<path.size();i++) {
            int to=path.get(i);if(to<0||to>=30)throw new IllegalArgumentException("Invalid cell");
            if(i==0)continue;int from=path.get(i-1);
            if(Math.abs(from%6-to%6)+Math.abs(from/6-to/6)!=1)throw new IllegalArgumentException("Discontinuous route");
            byte swap=replay[from];replay[from]=replay[to];replay[to]=swap;
        }
        stats=PuzzleSolver.analyze(replay,6,5);
        if(!result.goalSatisfied||!goal.satisfied(stats,30))throw new IllegalArgumentException("Puzzle goal unsatisfied");
    }
    boolean current(byte[] live,long now){return now>=plannedAt&&now-plannedAt<=15000&&Arrays.equals(board,live);}
    byte[] previewBoard(){return board.clone();}
}
