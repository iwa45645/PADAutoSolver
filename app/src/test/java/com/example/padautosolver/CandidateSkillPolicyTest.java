package com.example.padautosolver;
import org.junit.Test;
import org.json.JSONObject;
import java.util.*;
import static org.junit.Assert.*;

public class CandidateSkillPolicyTest {
    @Test public void choosesSkillHeadingInsteadOfLeaderOrBody() {
        StagePolicy.Item skill=new StagePolicy.Item("進化スキル メノアエンジン Lv.最大 ターン:34",600,2205);
        assertSame(skill,CandidateSkillPolicy.skillHeading(Arrays.asList(
                new StagePolicy.Item("リーダースキル",300,2430),
                new StagePolicy.Item("自分以外のスキルが1ターン溜まる",500,2320),skill),2712));
    }
    @Test public void refusesAmbiguousOrOutOfRangeHeadings() {
        assertNull(CandidateSkillPolicy.skillHeading(Arrays.asList(
                new StagePolicy.Item("スキル Lv最大 ターン:3",500,2205),
                new StagePolicy.Item("スキル Lv最大 ターン:5",500,1850)),2712));
        assertNull(CandidateSkillPolicy.skillHeading(Arrays.asList(
                new StagePolicy.Item("スキル Lv最大 ターン:3",500,500)),2712));
    }
    @Test public void joinsSplitHeadingAndCooldownButNotEffectText() {
        StagePolicy.Item heading=new StagePolicy.Item("(スキル)あたしの爆撃は防げないのよ",430,2205);
        assertSame(heading,CandidateSkillPolicy.skillHeading(Arrays.asList(heading,
                new StagePolicy.Item("LU.1 ターン:5",1040,2210),
                new StagePolicy.Item("1ターンの間、自分の攻撃力が2倍",500,2300)),2712));
    }
    @Test public void acceptsRealAthenaHeadingWithUnreadCooldownLabel() {
        StagePolicy.Item heading=new StagePolicy.Item("化スキル バッションスイートアイギス LU.最大 タ-:25",600,2205);
        assertSame(heading,CandidateSkillPolicy.skillHeading(Arrays.asList(heading),2712));
    }
    @Test public void evolutionRequiresBothStageHeadings() throws Exception {
        JSONObject o=CandidateSkillPolicy.observation(Arrays.asList(
                new StagePolicy.Item("1段階目:アクセラレート",500,1760),
                new StagePolicy.Item("最終段階:フルスロットル ターン:1",500,1970)));
        assertTrue(o.getBoolean("evolutionTooltipDetected"));
        assertEquals(1,o.getInt("finalCooldownCandidate"));
        assertFalse(o.getBoolean("userConfirmed"));
        assertTrue(o.isNull("effects"));
        assertFalse(CandidateSkillPolicy.observation(Arrays.asList(
                new StagePolicy.Item("スキルが進化",500,1970))).getBoolean("evolutionTooltipDetected"));
    }
    @Test public void unreadCooldownRemainsNull() throws Exception {
        JSONObject o=CandidateSkillPolicy.observation(Arrays.asList(
                new StagePolicy.Item("1段階目",500,1760),new StagePolicy.Item("最終段階 ターン:不明",500,1970)));
        assertTrue(o.isNull("finalCooldownCandidate"));
    }
    @Test public void recognizesActualTooltipWithMisreadFirstHeadingAndSplitFinalCooldown()throws Exception {
        JSONObject o=CandidateSkillPolicy.observation(Arrays.asList(
                new StagePolicy.Item("スキルを使用すると下記の順番で進化します。",500,1700),
                new StagePolicy.Item("T段階目:バッションスイートアイギス",500,1770),
                new StagePolicy.Item("最終段階:バッションスイートブースト",500,1990),
                new StagePolicy.Item("ターン:7",1030,1995)));
        assertTrue(o.getBoolean("evolutionTooltipDetected"));
        assertEquals(7,o.getInt("finalCooldownCandidate"));
        assertFalse(o.getBoolean("userConfirmed"));
    }
    @Test public void findsNikeCooldownColumnWhenSkillLabelIsUnreadableAndRejectsBody() {
        StagePolicy.Item column=new StagePolicy.Item("LU.最大 ター:34",1050,2205);
        assertSame(column,CandidateSkillPolicy.cooldownColumn(Arrays.asList(
                new StagePolicy.Item("(化スキし)ビクトリーアシスト",400,2205),column),1220,2712));
        assertNull(CandidateSkillPolicy.cooldownColumn(Arrays.asList(
                new StagePolicy.Item("ターン:3",500,2300)),1220,2712));
    }
}
