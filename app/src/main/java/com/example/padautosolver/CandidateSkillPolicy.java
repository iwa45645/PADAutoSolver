package com.example.padautosolver;

import org.json.*;
import java.util.List;
import java.util.regex.*;

/** OCR hints only. Never silently fills a team's skill schedule. */
final class CandidateSkillPolicy {
    static StagePolicy.Item skillHeading(List<StagePolicy.Item> lines, int height) {
        StagePolicy.Item found = null;
        for (StagePolicy.Item line : lines) {
            if (line.y < height * .60f || line.y > height * .87f
                    || !line.text.contains("スキル") || line.text.contains("リーダー")) continue;
            if (line.text.indexOf("スキル") > 3) continue;
            boolean cooldown = false;
            for (StagePolicy.Item part : lines)
                if (Math.abs(part.y - line.y) <= height * .018f
                        && (Pattern.compile("(?:ターン|タ[-ー])[:：][0-9]").matcher(part.text).find()
                            ||part.text.contains("LV.")||part.text.contains("LU."))) cooldown = true;
            if (!cooldown) continue;
            if (found != null && Math.abs(found.y - line.y) > height * .018f) return null;
            found = line;
        }
        return found;
    }
    static boolean isEvolving(List<StagePolicy.Item> lines) {
        for (StagePolicy.Item line : lines) if (line.text.contains("進化スキル")
                || line.text.contains("スキルが進化")) return true;
        return false;
    }
    static StagePolicy.Item cooldownColumn(List<StagePolicy.Item> lines,int width,int height) {
        StagePolicy.Item found=null;
        for(StagePolicy.Item line:lines) {
            if(line.x<width*.65f||line.y<height*.76f||line.y>height*.85f)continue;
            if(!Pattern.compile("(?:ターン|ター|タ[-ー])[:：][0-9]{1,3}").matcher(line.text).find())continue;
            if(found!=null&&Math.abs(found.y-line.y)>height*.018f)return null;
            found=line;
        }
        return found;
    }
    static JSONObject observation(List<StagePolicy.Item> lines) throws JSONException {
        boolean stage = false, finalStage = false, explanation = false;
        Float finalY = null;
        Integer finalCooldown = null;
        for (StagePolicy.Item line : lines) {
            if (line.text.contains("1段階目") || line.text.contains("１段階目")) stage = true;
            if (line.text.contains("スキルを使用すると") && line.text.contains("進化")) explanation = true;
            if (line.text.contains("最終段階")) {
                finalStage = true;
                finalY = line.y;
                Matcher m = Pattern.compile("ターン[:：]([0-9]{1,3})(?![0-9])").matcher(line.text);
                if (m.find()) finalCooldown = Integer.parseInt(m.group(1));
            }
        }
        if(finalY!=null&&finalCooldown==null)for(StagePolicy.Item line:lines)if(Math.abs(line.y-finalY)<=40) {
            Matcher m=Pattern.compile("ターン[:：]([0-9]{1,3})(?![0-9])").matcher(line.text);
            if(m.find())finalCooldown=Integer.parseInt(m.group(1));
        }
        return new JSONObject().put("status", "ocr_unverified")
                .put("evolutionTooltipDetected", (stage || explanation) && finalStage)
                .put("finalCooldownCandidate", finalCooldown == null ? JSONObject.NULL : finalCooldown)
                .put("effects", JSONObject.NULL).put("userConfirmed", false);
    }
}
