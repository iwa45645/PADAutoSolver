package com.example.padautosolver;
public enum AppMode {
 NORMAL_SOLVER, FARM, SALE, BOX_SCAN, TEAM_ANALYSIS, URA_SHURA_DRY_RUN, URA_SHURA_FULL_AUTO;
 static AppMode from(String value) { if("farm".equals(value))return FARM;if("sale".equals(value))return SALE;try{return valueOf(value);}catch(Exception e){return NORMAL_SOLVER;} }
}
