package com.example.padautosolver;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
public class UraHeldSkillInfoTest {
 @Test public void calibratedB10ColumnCanLoseFinalKanaWithoutInventingIdentity(){
  var info=UraHeldSkillInfo.readBase(List.of(new StagePolicy.Item("スキルT:7リリアントコンチェルト",365,923),new StagePolicy.Item("あと1ターン",1043,923),item("UCD1_停止"),item("UCD1_あと1ター"),item("UCD2_あと17ターン")));
  assertNotNull(info);assertEquals(Integer.valueOf(1),info.baseRemaining);assertFalse(info.baseNamed("ブリリアントコンチェルト"));
  assertNull(UraHeldSkillInfo.readBase(List.of(item("UCD1_あとTター"))));
  assertNull(UraHeldSkillInfo.readBase(List.of(item("UCD1_あと1ターンXX"))));
  assertNull(UraHeldSkillInfo.readBase(List.of(item("スキル1:ブリリアントコンチェルトあと1ターン"),item("UCD1_あと2ター"))));
 }
 @Test public void actualYukineFiveVersusFifteenConflictRequiresAnotherImageRead(){
  assertNull(UraHeldSkillInfo.read(List.of(item("スキル1:雪雲の一変使用可能"),item("スキル2:10連ガチャパワーあと5ターン"),item("UCD1_使用可能"),item("UCD2_あと15ターン"))));
  var fresh=UraHeldSkillInfo.read(List.of(item("スキル1:雪雲の一変使用可能"),item("スキル2:10連ガチャパワーあと15ターン"),item("UCD1_使用可能"),item("UCD2_あと15ターン")));
  assertEquals(Integer.valueOf(0),fresh.baseRemaining);assertEquals(Integer.valueOf(15),fresh.assistRemaining);
 }
 @Test public void independentlyReadBaseHeaderCanProvideExactIdentityWithoutRepairingTheTooltip(){
  var info=UraHeldSkillInfo.read(List.of(item("スキル1:ダブル防御館勢・水あと5ターン"),item("USH1_スキルダブル防御態勢・水")));
  assertTrue(info.baseNamed("ダブル防御態勢・水"));
  assertTrue(info.baseName.contains("館勢"));assertEquals(Integer.valueOf(5),info.baseRemaining);
 }
 @Test public void contradictoryActualCooldownReadingsStop(){
  assertNull(UraHeldSkillInfo.read(List.of(item("スキル1:雪花の氷乱あと3ターン"),item("UCD1_あと2ターン"))));
 }
 @Test public void calibratedColumnCanLoseUnitKanaButNeverRepairsDigits(){
  var info=UraHeldSkillInfo.read(List.of(item("UCD2_あと21ーン")));
  assertEquals(Integer.valueOf(21),info.assistRemaining);
  assertNull(UraHeldSkillInfo.read(List.of(item("UCD2_あとZ1ーン"))));
 }
 @Test public void splitNamesBindCooldownsOnlyWithinTheirOwnRows(){
  var info=UraHeldSkillInfo.read(List.of(new StagePolicy.Item("スキル1:雪花の氷乱",385,946),new StagePolicy.Item("あと3ターン",914,946),new StagePolicy.Item("スキル2:10連ガチャパワー",455,992),new StagePolicy.Item("あと21ターン",922,993),item("UCD1_あとうターン")));
  assertEquals(Integer.valueOf(3),info.baseRemaining);assertEquals(Integer.valueOf(21),info.assistRemaining);
  assertTrue(info.baseNamed("雪花の氷乱"));
 }
 @Test public void unreadableTaggedRowDoesNotCrashOrBecomeReady() {
  assertNull(UraHeldSkillInfo.read(List.of(item("UCD1_水を15個生成"),item("UCD2_スキル説明"))));
  var info=UraHeldSkillInfo.read(List.of(item("UCD1_水を15個生成"),item("UCD2_あと19ターン")));
  assertNull(info.baseRemaining);assertEquals(Integer.valueOf(19),info.assistRemaining);
 }
 private StagePolicy.Item item(String s){return new StagePolicy.Item(s,600,960);}
 @Test public void actualYukineStillHasOneTurnBeforeOpeningHaste() {
  var info=UraHeldSkillInfo.read(List.of(item("スキル1:雪雲の一変 あと1ターン"),item("スキル2:10連ガチャパワー あと19ターン")));
  assertTrue(info.baseNamed("雪雲の一変"));assertEquals(Integer.valueOf(1),info.baseRemaining);assertEquals(Integer.valueOf(19),info.assistRemaining);
 }
 @Test public void assistReadyAndBaseReadyAreDifferentLayers() {
  var info=UraHeldSkillInfo.read(List.of(item("スキル1:ダブル防御態勢・水 使用可能"),item("スキル2:かつての水柱 使用可能")));
  assertEquals(Integer.valueOf(0),info.assistRemaining);assertTrue(info.assistNamed("かつての水柱"));
 }
 @Test public void prospectiveCooldownAndConflictingRowsCannotAuthoriseASkill() {
  assertNull(UraHeldSkillInfo.read(List.of(item("次回使用可能まで5ターン"))));
  assertNull(UraHeldSkillInfo.read(List.of(item("スキル1:雪花の氷乱あと3ターン"),item("スキル1:雪花の氷乱あと2ターン"))));
 }
 @Test public void splitCooldownColumnAndReadableActorBindTheTwoRows() {
  var info=UraHeldSkillInfo.read(List.of(new StagePolicy.Item("雪女の妖人・ユキネ",400,625),new StagePolicy.Item("UCD_あと1ターン",960,950),new StagePolicy.Item("UCD_あと19ターン",960,992)));
  assertTrue(info.actorNamed("雪女の妖人ユキネ"));assertEquals(Integer.valueOf(1),info.baseRemaining);assertEquals(Integer.valueOf(19),info.assistRemaining);
 }
 @Test public void overchargedBaseFutureValueIsExplicitlyUnknown() {
  var info=UraHeldSkillInfo.read(List.of(new StagePolicy.Item("UCD_使用後5ターン",960,950),new StagePolicy.Item("UCD_使用可能",960,992)));
  assertEquals(Integer.valueOf(-1),info.baseRemaining);assertEquals(Integer.valueOf(0),info.assistRemaining);
 }
}
