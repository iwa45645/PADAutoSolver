package com.example.padautosolver;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
public class UraDialogPolicyTest {
    @Test public void assistFreeOdinUsesItsBareTitleAndStillRequiresControls(){
        var read=UraDialogPolicy.read(List.of(item("神泉槍グングニール",1794),item("戻る",2298)),2712);
        assertNotNull(read);assertEquals(1,read.layer);assertTrue(read.named("神泉槍グングニール"));
        assertNull(UraDialogPolicy.read(List.of(item("神泉槍グングニール",1794)),2712));
    }
    private StagePolicy.Item item(String text,int y){return new StagePolicy.Item(text,610,y);}
    @Test public void footerDoesNotAuthoriseAssistInsteadOfBase() {
        var read=UraDialogPolicy.read(List.of(item("スキル1:ダブル防御態勢・水",1700),item("戻る",2190),item("スキル2:かつての水柱",2390)),2712);
        assertEquals(1,read.layer);assertFalse(read.named("かつての水柱"));assertTrue(read.named("ダブル防御態勢水"));
    }
    @Test public void remainingChargeIsNotProspectiveCooldown() {
        var read=UraDialogPolicy.read(List.of(item("スキル1:エース・オブ・【スペード】",1700),item("次回使用可能まで5ターン",1950),item("戻る",2190),item("スキル2使用可能まであと18ターン",2450)),2712);
        assertEquals(Integer.valueOf(18),UraDialogPolicy.assistRemaining(read));assertNull(UraDialogPolicy.baseRemaining(read));
        assertTrue(read.named("エースオブスペード"));
    }
    @Test public void missingDialogControlsAbort() {
        assertNull(UraDialogPolicy.read(List.of(item("スキル1:神泉槍グングニール",1800)),2712));
    }
    @Test public void disabledGreyTextIsNotReadyWhiteText() {
        int[] grey=new int[100];java.util.Arrays.fill(grey,0xff999999);assertEquals(0,UraDialogPolicy.brightTextFraction(grey),0);
        for(int i=0;i<20;i++)grey[i]=0xffffffff;assertEquals(.2,UraDialogPolicy.brightTextFraction(grey),0);
    }
    @Test public void liveSekkaMergedButtonsRetainTheirPairedCoordinates() {
        var read=UraDialogPolicy.read(List.of(item("スキル1:月華咲乱・ユキノシタ",1755),item("発動戻る",2165),item("スキル2使用可能まであと20ターン",2374)),2712);
        assertNotNull(read);assertEquals(428,read.activate.x,0);assertEquals(792,read.back.x,0);
        assertEquals(Integer.valueOf(20),UraDialogPolicy.assistRemaining(read));
    }
}
