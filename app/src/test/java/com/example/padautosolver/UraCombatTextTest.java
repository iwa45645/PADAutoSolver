package com.example.padautosolver;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
public class UraCombatTextTest {
 @Test public void staminaOrContinuePromptsStopInsteadOfTappingTheirControls(){
  assertTrue(UraCombatText.blocked(List.of(i("スタミナが足りません"),i("はい"))));
  assertTrue(UraCombatText.blocked(List.of(i("コンティニューしますか?"))));
  assertFalse(UraCombatText.blocked(List.of(i("5ターンの間、最大HPの40%分のHPを回復"))));
 }
 private StagePolicy.Item i(String s){return new StagePolicy.Item(s,610,1375);}
 @Test public void onlyCurrentBothAbsorptionsAtTwoTurnsPass(){
  var good=List.of(i("状況確認"),i("2ターンの間、敵の属性吸収を無効化"),i("2ターンの間、敵のダメージ吸収を無効化"));
  assertTrue(UraCombatText.absorptionsTwoTurns(good));
  assertFalse(UraCombatText.absorptionsTwoTurns(List.of(good.get(0),good.get(1),i("1ターンの間、敵のダメージ吸収を無効化"))));
  assertFalse(UraCombatText.absorptionsTwoTurns(List.of(i("スキルを使用しますか?"),good.get(1),good.get(2))));
 }
 @Test public void literalFloorAndBoundedControlOnly(){
  assertEquals(2,UraCombatText.floor(List.of(i("Battle 2/22"))));
  assertEquals(-1,UraCombatText.floor(List.of(i("Battle Z/22"))));
  assertEquals(-1,UraCombatText.floor(List.of(i("Battle 2/22"),i("UFLOOR_1/22"))));
  assertEquals(-1,UraCombatText.floor(List.of(i("Battle 23/22"))));
  assertNull(UraCombatText.control(List.of(i("戻る")),"戻る",2080,2220));
 }
}
