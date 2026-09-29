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
    private List<StagePolicy.Item> readCrop(Bitmap frame, float l, float t, float r, float b) throws Exception {
        int left = Math.round(frame.getWidth() * l), top = Math.round(frame.getHeight() * t);
        Bitmap crop = Bitmap.createBitmap(frame, left, top, Math.round(frame.getWidth() * (r-l)), Math.round(frame.getHeight() * (b-t)));
        Bitmap enlarged = Bitmap.createScaledBitmap(crop, crop.getWidth() * 2, crop.getHeight() * 2, true);
        crop.recycle();
        // Isolate bright game text from colored outlines and the dark textured background.
        int[] pixels = new int[enlarged.getWidth() * enlarged.getHeight()];
        enlarged.getPixels(pixels, 0, enlarged.getWidth(), 0, 0, enlarged.getWidth(), enlarged.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            int red = (pixels[i] >> 16) & 255, green = (pixels[i] >> 8) & 255, blue = pixels[i] & 255;
            int max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));
            pixels[i] = max > 155 && (min > 105 || max - min > 85) ? 0xff000000 : 0xffffffff;
        }
        enlarged.setPixels(pixels, 0, enlarged.getWidth(), 0, 0, enlarged.getWidth(), enlarged.getHeight());
        Task<Text> task = recognizer.process(InputImage.fromBitmap(enlarged, 0));
        task.addOnCompleteListener(Runnable::run, ignored -> enlarged.recycle());
        Text result = Tasks.await(task, 5, TimeUnit.SECONDS);
        List<StagePolicy.Item> out = new ArrayList<>();
        for (Text.TextBlock block : result.getTextBlocks()) for (Text.Line line : block.getLines()) {
            Rect box = line.getBoundingBox();
            if (box != null) out.add(new StagePolicy.Item(line.getText(), left + box.exactCenterX()/2, top + box.exactCenterY()/2));
        }
        return out;
    }
    void close() { recognizer.close(); }
}
