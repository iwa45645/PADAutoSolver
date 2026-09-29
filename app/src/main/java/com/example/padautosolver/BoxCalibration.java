package com.example.padautosolver;
import android.content.SharedPreferences;
public final class BoxCalibration {
 public final int columns, expected; public final float firstX, pitchX, top, bottom, pitchY, iconWidth;
 public final boolean topConfirmed, filtersConfirmed;
 BoxCalibration(SharedPreferences p){columns=p.getInt("box.columns",5);expected=p.getInt("box.expected",0);firstX=p.getFloat("box.firstX",.094f);pitchX=p.getFloat("box.pitchX",.183f);top=p.getFloat("box.top",.36f);bottom=p.getFloat("box.bottom",.875f);pitchY=p.getFloat("box.pitchY",.0874f);iconWidth=p.getFloat("box.iconWidth",.145f);topConfirmed=p.getBoolean("box.topConfirmed",false);filtersConfirmed=p.getBoolean("box.filtersConfirmed",false);}
 void validate(){if(columns<3||columns>8||expected<1||expected>20000||firstX<=0||pitchX<=0||firstX+(columns-1)*pitchX+iconWidth/2>1||top<.2||bottom>.9||top>=bottom||pitchY<=.025||pitchY>.2||iconWidth<.05||iconWidth>.25)throw new IllegalArgumentException("BOXキャリブレーションと所持数を確認してください");if(!topConfirmed||!filtersConfirmed)throw new IllegalArgumentException("先頭・ALL・絞り込みなしの確認が必要です");}
}
