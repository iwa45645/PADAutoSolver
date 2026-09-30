package com.example.padautosolver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.*;
import java.io.*;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Supplementary observations bound to the instance located in the original BOX order. */
final class CandidateDetailProbe {
    private final InventoryRepository repo;
    private final int index, expectedId;
    private final JSONObject review;
    private int step;
    private StagePolicy.Decision pending;

    CandidateDetailProbe(InventoryRepository repo, int index, JSONObject review) throws JSONException {
        this.repo = repo;
        this.index = index;
        this.review = review;
        this.expectedId = review.getInt("monsterId");
    }

    StagePolicy.Decision inspect(Bitmap frame, StageNavigator navigator,
            List<StagePolicy.Item> lines, DetailIdentity identity) throws Exception {
        if (identity == null || identity.id != expectedId)
            throw new IOException("追加検査の番号が照合案と一致しません #" + (index + 1));
        if (pending != null) return pending;
        if (step == 0) {
            save(frame, lines, "candidate-base", null);
            step = 1;
        }
        if (step == 1) {
            StagePolicy.Item skill = CandidateSkillPolicy.skillHeading(lines, frame.getHeight());
            if(skill==null) {
                List<StagePolicy.Item> crop=navigator.readDetailSkillHeadings(frame);
                skill=CandidateSkillPolicy.skillHeading(crop,frame.getHeight());
                if(skill==null)skill=CandidateSkillPolicy.cooldownColumn(crop,frame.getWidth(),frame.getHeight());
                if(skill==null)skill=CandidateSkillPolicy.cooldownColumn(lines,frame.getWidth(),frame.getHeight());
            }
            if (skill == null) throw new IOException("スキル見出しの位置を確認できません #" + (index + 1));
            StagePolicy.Decision d = new StagePolicy.Decision(
                    new StagePolicy.Item("BOX_CANDIDATE_SKILL", frame.getWidth() * .42f, skill.y),
                    "スキルを長押しして表示中に保存 #" + (index + 1), false);
            d.holdMs = 3000;
            d.heldFrame = (held, current) -> {
                DetailIdentity seen = readIdentity(repo, held, navigator, review);
                if (seen == null || seen.id != expectedId)
                    throw new IOException("長押し中の個体番号を確認できません");
                List<StagePolicy.Item> text = navigator.readItems(held);
                boolean evolving = CandidateSkillPolicy.isEvolving(lines);
                JSONObject observation = CandidateSkillPolicy.observation(text);
                if (!current.getAsBoolean()) throw new IOException("追加検査は停止済みです");
                save(held, text, "candidate-skill-held", observation);
                if (evolving && !observation.getBoolean("evolutionTooltipDetected"))
                    throw new IOException("進化後スキルの表示を確認できません #" + (index + 1));
            };
            d.completed = () -> { step = 2; pending = null; };
            pending = d;
            return d;
        }
        JSONObject item = repo.items.getJSONObject(index);
        item.put("candidateDetailCapture", new JSONObject().put("readerVersion", 1)
                .put("status", "base_and_skill_observed_unverified")
                .put("observedMonsterId", expectedId).put("userConfirmed", false)
                .put("awakeningDetails", JSONObject.NULL).put("latentDetails", JSONObject.NULL)
                .put("assistDetails", JSONObject.NULL).put("transformedForm", JSONObject.NULL));
        repo.save();
        return null;
    }

    static DetailIdentity readIdentity(InventoryRepository repo, Bitmap live, StageNavigator navigator,
            JSONObject review) throws Exception {
        int expected = review.getInt("monsterId");
        DetailIdentity seen = navigator.readCandidateIdentity(live, expected);
        if(seen != null)return seen;
        File file = new File(repo.root, review.getString("evidence")).getCanonicalFile();
        if(!file.toPath().startsWith(repo.root.getCanonicalFile().toPath()))return null;
        Bitmap reference = BitmapFactory.decodeFile(file.getPath());
        if(reference == null)return null;
        try {
            if(DetailNumberMatch.matches(live, reference, expected)) {
                android.util.Log.i("PADSolver", "candidateNumberReferenceMatch id=" + expected);
                return new DetailIdentity(expected, null);
            }
        } finally {reference.recycle();}
        return null;
    }

    private void save(Bitmap frame, List<StagePolicy.Item> lines, String source,
            JSONObject observation) throws Exception {
        File dir = new File(repo.session(), "details");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("詳細画像の保存先を作れません");
        String filename = source + "-" + index + "-" + System.currentTimeMillis() + ".png";
        File image = new File(dir, filename);
        Bitmap crop = Bitmap.createBitmap(frame, 0, Math.round(frame.getHeight() * .19f),
                frame.getWidth(), Math.round(frame.getHeight() * .67f));
        try (FileOutputStream out = new FileOutputStream(image)) {
            if (!crop.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IOException("画像保存失敗");
        } finally { crop.recycle(); }
        StringBuilder ocr = new StringBuilder();
        for (StagePolicy.Item line : lines) if (line.y > frame.getHeight() * .19f)
            ocr.append(line.rawText).append('\n');
        JSONObject evidence = new JSONObject().put("image", "scan_sessions/"
                + repo.inventory.getString("scanSessionId") + "/details/" + filename)
                .put("ocr", ocr.toString()).put("source", source)
                .put("observedMonsterId", expectedId).put("userConfirmed", false);
        if (observation != null) evidence.put("skillObservation", observation);
        JSONObject item = repo.items.getJSONObject(index);
        JSONArray images = item.optJSONArray("detailEvidence");
        if (images == null) images = new JSONArray();
        images.put(evidence);
        item.put("detailEvidence", images);
        repo.save();
        android.util.Log.i("PADSolver", "candidateEvidence index=" + index + " source=" + source);
    }
}
