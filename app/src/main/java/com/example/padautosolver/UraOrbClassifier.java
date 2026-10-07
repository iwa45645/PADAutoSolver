package com.example.padautosolver;
import java.util.Arrays;
/** Shared pixel classifier used by native recognition and saved-frame replay tests. */
final class UraOrbClassifier {
    static final double MAX_DISTANCE=.07,MIN_MARGIN=.035;
    static final class Match {
        final int color;final double distance,margin;
        Match(int color,double distance,double margin){this.color=color;this.distance=distance;this.margin=margin;}
    }
    static Match classify(int[] live,int[][] references,int[] colors,int colorCount) {
        if(live==null||references==null||colors==null||references.length!=colors.length||(colorCount!=6&&colorCount!=10))throw new IllegalArgumentException("Invalid orb evidence");
        double[] scores=new double[colorCount];Arrays.fill(scores,1);
        for(int i=0;i<references.length;i++) {
            int color=colors[i];if(color<0||color>9||references[i]==null||references[i].length!=live.length)throw new IllegalArgumentException("Invalid reference");
            if(color<colorCount)scores[color]=Math.min(scores[color],TeamIconMatch.distance(live,references[i]));
        }
        int best=0;for(int color=1;color<colorCount;color++)if(scores[color]<scores[best])best=color;
        double other=1;for(int color=0;color<colorCount;color++)if(color!=best)other=Math.min(other,scores[color]);
        double margin=other-scores[best];return new Match(scores[best]<=MAX_DISTANCE&&margin>=MIN_MARGIN?best:-1,scores[best],margin);
    }
}
