package com.example.padautosolver;

/** Hard requirements are ranked before combo count; never infer a Lucifer instruction. */
public final class PuzzleGoal {
    public enum Type { MAX_COMBO, WATER_TWO_COMBOS, WATER_TWO_COMBOS_AND_HEAL,
        FULL_CLEAR, EXACT_COMBO, NO_COMBO, AVOID_MATCH, VDP, L_SHAPE, CROSS, STALL, SPECIAL_LUCIFER }
    public final Type type;
    public final int color, exactCombos;
    public final boolean requiresComboDrop;
    public PuzzleGoal(Type type, int color, int exactCombos) {
        this(type,color,exactCombos,false);
    }
    public PuzzleGoal(Type type, int color, int exactCombos, boolean requiresComboDrop) {
        if (type == null || color < 0 || color > 9 || exactCombos < 0 || exactCombos > 21)
            throw new IllegalArgumentException("Invalid goal");
        if (type == Type.SPECIAL_LUCIFER || type == Type.STALL)
            throw new IllegalArgumentException("Verified instruction/survival conditions required");
        this.type = type; this.color = color; this.exactCombos = exactCombos;
        this.requiresComboDrop=requiresComboDrop;
    }
    public static PuzzleGoal water() { return new PuzzleGoal(Type.WATER_TWO_COMBOS, 3, 0); }
    public static PuzzleGoal waterAndHeal() { return new PuzzleGoal(Type.WATER_TWO_COMBOS_AND_HEAL, 3, 0); }
    public static PuzzleGoal esperMion() { return new PuzzleGoal(Type.WATER_TWO_COMBOS,3,0,true); }
    boolean satisfied(PuzzleSolver.MatchStats s, int cells) {
        if(requiresComboDrop && s.comboDropsMatched==0)return false;
        switch (type) {
            case WATER_TWO_COMBOS: return s.colorCombos[3] >= 2;
            case WATER_TWO_COMBOS_AND_HEAL: return s.colorCombos[3] >= 2 && s.colorCombos[5] >= 1;
            case FULL_CLEAR: return s.matched == cells;
            case EXACT_COMBO: return s.combos == exactCombos;
            case NO_COMBO: return s.combos == 0;
            case AVOID_MATCH: return s.colorCombos[color] == 0;
            case VDP: return s.squares[color] > 0;
            case L_SHAPE: return s.lShapes[color] > 0;
            case CROSS: return s.crosses[color] > 0;
            default: return true;
        }
    }
    int progress(PuzzleSolver.MatchStats s) {
        switch (type) {
            case WATER_TWO_COMBOS: return Math.min(2, s.colorCombos[3]) * 10000;
            case WATER_TWO_COMBOS_AND_HEAL: return Math.min(2, s.colorCombos[3]) * 10000 + Math.min(1, s.colorCombos[5]) * 1000;
            case FULL_CLEAR: return s.matched * 500;
            case EXACT_COMBO: return -Math.abs(s.combos - exactCombos) * 10000;
            case NO_COMBO: return -s.combos * 10000;
            case AVOID_MATCH: return -s.colorCombos[color] * 10000;
            default: return 0;
        }
    }
    int score(PuzzleSolver.MatchStats s, int cells) {
        int preference = (type == Type.NO_COMBO || type == Type.EXACT_COMBO) ? -s.matched : s.combos * 100 + s.matched;
        return (satisfied(s, cells) ? 1000000 : 0) + progress(s) + preference + Math.min(1,s.colorCombos[5]) * 20
                + (requiresComboDrop ? Math.min(1,s.comboDropsMatched)*30000 : 0);
    }
}
