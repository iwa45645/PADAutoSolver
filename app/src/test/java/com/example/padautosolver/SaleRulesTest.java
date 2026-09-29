package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class SaleRulesTest {
    @Test public void exactUntrainedIdentitiesOnly() {
        assertTrue(SaleRules.allowedIdentity("炎のアイスオーガ",312,8,true,false));
        assertTrue(SaleRules.allowedIdentity("木の機神兵・アースガル",318,3,true,false));
        assertFalse(SaleRules.allowedIdentity("木の機神兵・アースガル",318,4,true,false));
        assertFalse(SaleRules.allowedIdentity("炎のアイスオーガ",313,8,true,false));
        assertFalse(SaleRules.allowedIdentity("炎のアイスオーガ",312,8,false,false));
        assertFalse(SaleRules.allowedIdentity("炎のアイスオーガ",312,8,true,true));
        assertFalse(SaleRules.allowedIdentity("氷の機神兵・ミズガルズ",317,3,true,false));
    }
    @Test public void confirmationRequiresAllEvidence() {
        String good = "これらのモンスターをまとめて売却します。MPは1PTです。プラスポイントは0です。";
        assertTrue(SaleRules.singleConfirmation(good,1,true));
        assertFalse(SaleRules.singleConfirmation(good,2,true));
        assertFalse(SaleRules.singleConfirmation(good,1,false));
        assertFalse(SaleRules.singleConfirmation(good.replace("1PT","10PT"),1,true));
        assertFalse(SaleRules.singleConfirmation(good.replace("は0","は297"),1,true));
        assertFalse(SaleRules.singleConfirmation("はい",1,true));
    }
    @Test public void batchMustMatchExactApprovedCountAndNoPlus() {
        String ten="これらのモンスターをまとめて売却します。MPは10PTです。+ポイントは0です。";
        assertTrue(SaleRules.batchConfirmation(ten,10,true));
        assertFalse(SaleRules.batchConfirmation(ten,9,true));
        assertFalse(SaleRules.batchConfirmation(ten,10,false));
        assertFalse(SaleRules.batchConfirmation(ten.replace("は0","は297"),10,true));
        assertFalse(SaleRules.batchConfirmation(ten,0,true));
        assertFalse(SaleRules.batchConfirmation(ten,30,true));
    }
}
