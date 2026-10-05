package com.example.padautosolver;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class StageNavigator {
    private final TextRecognizer recognizer = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
    final StagePolicy policy = new StagePolicy();
    final AutoSaleController sales = new AutoSaleController();
    StagePolicy.Decision inspect(Bitmap frame) throws Exception {
        return policy.inspect(readItems(frame));
    }
    StagePolicy.Decision inspectSale(Bitmap frame) throws Exception {
        return sales.inspect(frame, readItems(frame));
    }
    List<StagePolicy.Item> readItems(Bitmap frame) throws Exception {
        android.util.Log.i("PADSolver", "frameSize=" + frame.getWidth() + "x" + frame.getHeight());
        float scale = Math.min(1f, 900f / frame.getWidth());
        Bitmap copy = Bitmap.createScaledBitmap(frame, Math.round(frame.getWidth() * scale), Math.round(frame.getHeight() * scale), true);
        if (copy == frame) copy = frame.copy(Bitmap.Config.ARGB_8888, false);
        final Bitmap owned = copy;
        Task<Text> task = recognizer.process(InputImage.fromBitmap(owned, 0));
        task.addOnCompleteListener(Runnable::run, ignored -> owned.recycle());
        Text result = Tasks.await(task, 5, TimeUnit.SECONDS);
        List<StagePolicy.Item> items = new ArrayList<>();
        for (Text.TextBlock block : result.getTextBlocks()) for (Text.Line line : block.getLines()) {
            Rect box = line.getBoundingBox();
            if (box != null) items.add(new StagePolicy.Item(line.getText(), box.exactCenterX() / scale, box.exactCenterY() / scale));
        }
        if (StagePolicy.normalize(result.getText()).contains("合計コイン")
                && StagePolicy.normalize(result.getText()).contains("合計MP")) {
            // The colored inventory digits are too small for full-screen OCR. Read a magnified footer.
            items.addAll(readCrop(frame, .01f, .82f, .99f, .895f));
            items.removeIf(item -> item.y > frame.getHeight() * .82f && item.y < frame.getHeight() * .842f);
            for (StagePolicy.Item item : readCrop(frame, .62f, .82f, .995f, .842f)) {
                java.util.regex.Matcher count = java.util.regex.Pattern.compile("([0-9]{3,4}/[0-9]{3,4})").matcher(item.text);
                items.add(count.find() ? new StagePolicy.Item("所持数" + count.group(1), item.x, item.y) : item);
            }
            items.addAll(readCrop(frame, .64f, .846f, .76f, .88f));
            if (result.getText().contains("まとめて売却")) {
                items.addAll(readCrop(frame, .32f, .45f, .68f, .625f));
                items.addAll(readCrop(frame, .26f, .60f, .78f, .685f));
            }
        }
        if (result.getText().contains("No.") || result.getText().contains("NO.")) {
            items.addAll(readCrop(frame, .15f, .196f, .8f, .237f));
            items.add(new StagePolicy.Item("戻る", frame.getWidth() * .075f, frame.getHeight() * .217f));
        }
        for (StagePolicy.Item item : new ArrayList<>(items)) if (item.text.startsWith("戻る") && !item.text.equals("戻る"))
            items.add(new StagePolicy.Item("戻る", frame.getWidth() * .075f, item.y));
        String normalized = StagePolicy.normalize(result.getText());
        if (normalized.contains("TIPS")) items.addAll(readCrop(frame, .32f, .80f, .68f, .88f));
        if (frame.getWidth() == 1220 && frame.getHeight() == 2712 && normalized.contains("チーム編成")
                && normalized.contains("進化") && normalized.contains("アシスト"))
            items.add(new StagePolicy.Item("売却後ダンジョンへ", frame.getWidth() * .08f, frame.getHeight() * .93f));
        android.util.Log.i("PADSolver", "stageText=" + items.stream().map(i -> i.text).collect(java.util.stream.Collectors.joining("|")));
        return items;
    }
    List<StagePolicy.Item> readDetailHeader(Bitmap frame) throws Exception {
        List<StagePolicy.Item> header=readCrop(frame, .155f, .196f, .80f, .237f, true, true);
        DetailIdentity identity=DetailIdentity.parse(header,frame.getHeight());
        if(identity!=null && identity.name==null) {
            List<StagePolicy.Item> raw=readCrop(frame,.155f,.196f,.80f,.237f,false,false);
            DetailIdentity alternative=DetailIdentity.parse(raw,frame.getHeight());
            if(alternative!=null&&alternative.id==identity.id&&alternative.name!=null)return raw;
        }
        return header;
    }
    List<StagePolicy.Item> readDetailSkillHeadings(Bitmap frame) throws Exception {
        return readCrop(frame,.01f,.60f,.99f,.87f,false,false,2);
    }
    List<StagePolicy.Item> readUraCooldowns(Bitmap frame)throws Exception {
        return readCrop(frame,.82f,.805f,.99f,.874f,true,false,4);
    }
    List<StagePolicy.Item> readUraPreentry(Bitmap frame)throws Exception {
        List<StagePolicy.Item> lines=readItems(frame);
        lines.addAll(readCrop(frame,.15f,.20f,.65f,.24f,false,false,3));
        // Re-read only the actual entry control at full resolution; do not repair its wording.
        lines.addAll(readCrop(frame,.25f,.77f,.75f,.84f,false,false,3));
        return lines;
    }
    List<StagePolicy.Item> readUraDialog(Bitmap frame)throws Exception {
        List<StagePolicy.Item> lines=readCrop(frame,.03f,.55f,.97f,.94f,false,false,2);
        UraDialogPolicy.Read modal=UraDialogPolicy.read(lines,frame.getHeight());
        if(modal!=null) {
            // Re-read the actual heading, including small kana, at higher resolution.
            // The footer is excluded and cannot authorize the top skill.
            float headingY=0;
            for(var line:lines)if(line.y>frame.getHeight()*.55f&&(UraDialogPolicy.clean(line.text).startsWith("スキル"+modal.layer)
                    ||UraDialogPolicy.clean(line.text).equals("神泉槍グングニール"))) {headingY=line.y;break;}
            if(headingY>0) {
                float top=(headingY-42)/frame.getHeight(),bottom=(headingY+42)/frame.getHeight();
                lines.addAll(readCrop(frame,.16f,top,.86f,bottom,false,false,3));
                lines.addAll(readCrop(frame,.16f,top,.86f,bottom,true,true,3));
            }
        }
        return lines;
    }
    List<StagePolicy.Item> readUraCombat(Bitmap frame)throws Exception {
        List<StagePolicy.Item> lines=readCrop(frame,.01f,.18f,.99f,.81f,false,false,2);
        // Separate Menu from the adjacent coin/chest digits.
        lines.addAll(readCrop(frame,.86f,.19f,.998f,.22f,false,false,3));
        if(UraCombatText.joined(lines).contains("裏魔門の守護者")) {
            for(var item:readCrop(frame,.50f,.354f,.66f,.390f,true,true,4))
                lines.add(new StagePolicy.Item("UFLOOR_"+item.rawText,item.x,item.y));
            if(UraCombatText.floor(lines)<0)
                lines.addAll(readCrop(frame,.33f,.354f,.66f,.390f,false,false,3));
            lines.addAll(readCrop(frame,.37f,.766f,.63f,.818f,false,false,3));
        }
        return lines;
    }
    List<StagePolicy.Item> readUraResult(Bitmap frame)throws Exception {
        return readCrop(frame,.01f,.18f,.99f,.97f,false,false,2);
    }
    List<StagePolicy.Item> readUraHp(Bitmap frame)throws Exception {
        // HP has a heavy colored outline; isolate its yellow/white digits and slash.
        return readCrop(frame,.60f,.574f,.978f,.593f,true,false,4,true);
    }
    List<StagePolicy.Item> readUraMenuControl(Bitmap frame)throws Exception {
        return readCrop(frame,.86f,.19f,.998f,.22f,false,false,3);
    }
    List<StagePolicy.Item> readLuciferInstruction(Bitmap frame)throws Exception {
        List<StagePolicy.Item> out=readCrop(frame,.15f,.225f,.85f,.29f,false,false,3);
        out.addAll(readCrop(frame,.15f,.225f,.85f,.29f,true,true,3));
        return out;
    }
    List<StagePolicy.Item> readLuciferStrip(Bitmap strip)throws Exception {
        List<StagePolicy.Item> out=new ArrayList<>();
        for(var item:readCrop(strip,0,0,1,1,false,false,2))out.add(new StagePolicy.Item(item.rawText,item.x+180,item.y+620));
        if(LuciferInstruction.read(out)==null)
            for(var item:readCrop(strip,0,0,1,1,true,true,2))out.add(new StagePolicy.Item(item.rawText,item.x+180,item.y+620));
        return out;
    }
    List<StagePolicy.Item> readUraHeldSkill(Bitmap frame)throws Exception {
        List<StagePolicy.Item> out=readCrop(frame,.02f,.20f,.97f,.40f,false,false,3);
        for(var item:readCrop(frame,.002f,.083f,.74f,.110f,false,false,3))
            out.add(new StagePolicy.Item("USH1_"+item.rawText,item.x,item.y));
        // The same cooldowns are repeated in much larger text at the top-right during a hold.
        for(var item:readCrop(frame,.64f,.063f,.995f,.112f,true,false,4,true))
            out.add(new StagePolicy.Item("UCD1_"+item.rawText,item.x,item.y));
        for(var item:readCrop(frame,.64f,.115f,.995f,.165f,true,false,4,true))
            out.add(new StagePolicy.Item("UCD2_"+item.rawText,item.x,item.y));
        return out;
    }
    DetailIdentity readCandidateIdentity(Bitmap frame, int expectedId) throws Exception {
        DetailIdentity header=DetailIdentity.parse(readDetailHeader(frame),frame.getHeight());
        if(header!=null&&header.id==expectedId)return header;
        // Isolate only the number row. Stars and the Japanese name can merge with its digits.
        for(boolean white:new boolean[]{true,false}) {
            List<StagePolicy.Item> number=readCrop(frame,.155f,.196f,.43f,.219f,white,white,4);
            DetailIdentity seen=DetailIdentity.parse(number,frame.getHeight());
            if(seen!=null&&seen.id==expectedId)return new DetailIdentity(seen.id,header==null?null:header.name);
        }
        return null;
    }
    private List<StagePolicy.Item> readCrop(Bitmap frame, float l, float t, float r, float b) throws Exception {
        return readCrop(frame,l,t,r,b,true,false);
    }
    private List<StagePolicy.Item> readCrop(Bitmap frame,float l,float t,float r,float b,boolean threshold,boolean whiteOnly) throws Exception {
        return readCrop(frame,l,t,r,b,threshold,whiteOnly,2);
    }
    private List<StagePolicy.Item> readCrop(Bitmap frame,float l,float t,float r,float b,boolean threshold,boolean whiteOnly,int zoom) throws Exception {
        return readCrop(frame,l,t,r,b,threshold,whiteOnly,zoom,false);
    }
    private List<StagePolicy.Item> readCrop(Bitmap frame,float l,float t,float r,float b,boolean threshold,boolean whiteOnly,int zoom,boolean whiteYellow) throws Exception {
        int left = Math.round(frame.getWidth() * l), top = Math.round(frame.getHeight() * t);
        Bitmap crop = Bitmap.createBitmap(frame, left, top, Math.round(frame.getWidth() * (r-l)), Math.round(frame.getHeight() * (b-t)));
        Bitmap enlarged = Bitmap.createScaledBitmap(crop, crop.getWidth() * zoom, crop.getHeight() * zoom, true);
        crop.recycle();
        // Isolate bright game text from colored outlines and the dark textured background.
        if(threshold) {
        int[] pixels = new int[enlarged.getWidth() * enlarged.getHeight()];
        enlarged.getPixels(pixels, 0, enlarged.getWidth(), 0, 0, enlarged.getWidth(), enlarged.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            int red = (pixels[i] >> 16) & 255, green = (pixels[i] >> 8) & 255, blue = pixels[i] & 255;
            int max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));
            boolean ink=whiteYellow ? min>180||(red>200&&green>180&&blue<130)
                :whiteOnly ? min > 190 : max > 155 && (min > 105 || max - min > 85);
            pixels[i] = ink ? 0xff000000 : 0xffffffff;
        }
        enlarged.setPixels(pixels, 0, enlarged.getWidth(), 0, 0, enlarged.getWidth(), enlarged.getHeight());
        }
        Task<Text> task = recognizer.process(InputImage.fromBitmap(enlarged, 0));
        task.addOnCompleteListener(Runnable::run, ignored -> enlarged.recycle());
        Text result = Tasks.await(task, 5, TimeUnit.SECONDS);
        List<StagePolicy.Item> out = new ArrayList<>();
        for (Text.TextBlock block : result.getTextBlocks()) for (Text.Line line : block.getLines()) {
            Rect box = line.getBoundingBox();
            if (box != null) out.add(new StagePolicy.Item(line.getText(), left + box.exactCenterX()/zoom, top + box.exactCenterY()/zoom));
        }
        if(whiteOnly)android.util.Log.i("PADSolver","detailHeader="+result.getText());
        return out;
    }
    void close() { recognizer.close(); }
}
