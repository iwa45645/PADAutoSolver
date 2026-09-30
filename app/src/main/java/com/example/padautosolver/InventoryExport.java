package com.example.padautosolver;

import android.content.Context;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.io.*;
import java.util.zip.*;

/** Explicit local export; never uploads inventory to a remote service. */
final class InventoryExport {
    static String saveJson(Context context) throws Exception {
        if(Build.VERSION.SDK_INT<29)throw new IOException("Android 10以降で利用できます");
        String name="PADAutoSolver-inventory-"+System.currentTimeMillis()+".json";
        ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,name);values.put(MediaStore.Downloads.MIME_TYPE,"application/json");values.put(MediaStore.Downloads.IS_PENDING,1);
        Uri uri=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        if(uri==null)throw new IOException("保存先を作成できません");
        try {
            try(OutputStream output=context.getContentResolver().openOutputStream(uri);InputStream input=new FileInputStream(new File(context.getFilesDir(),"inventory/box_inventory.json"))){byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);}
            values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);context.getContentResolver().update(uri,values,null,null);return "Download/"+name;
        }catch(Exception e){context.getContentResolver().delete(uri,null,null);throw e;}
    }
    static String save(Context context) throws Exception {
        if (Build.VERSION.SDK_INT < 29) throw new IOException("この保存方法はAndroid 10以降に対応しています");
        String name = "PADAutoSolver-inventory-" + System.currentTimeMillis() + ".zip";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, name);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("保存先を作成できません");
        try {
            try (OutputStream output = context.getContentResolver().openOutputStream(uri);
                 ZipOutputStream zip = new ZipOutputStream(output)) {
                File root = new File(context.getFilesDir(), "inventory");
                append(zip, root, root);
            }
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0);
            context.getContentResolver().update(uri, values, null, null);
            return "Download/" + name;
        } catch (Exception e) {
            context.getContentResolver().delete(uri, null, null);
            throw e;
        }
    }

    static String saveCandidateDetails(Context context) throws Exception {
        if(Build.VERSION.SDK_INT<29)throw new IOException("Android 10以降で利用できます");
        InventoryRepository repo=new InventoryRepository(context);
        java.util.Set<String> files=new java.util.LinkedHashSet<>();
        if(new File(repo.root,"identity_review.json").isFile())files.add("identity_review.json");
        for(int i=0;i<repo.items.length();i++) {
            org.json.JSONArray images=repo.items.getJSONObject(i).optJSONArray("detailEvidence");
            if(images==null)continue;
            for(int j=0;j<images.length();j++) {
                org.json.JSONObject evidence=images.getJSONObject(j);
                if(evidence.optString("source").startsWith("candidate-"))files.add(evidence.getString("image"));
            }
        }
        String name="PADAutoSolver-candidate-details-"+System.currentTimeMillis()+".zip";
        ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,name);
        values.put(MediaStore.Downloads.MIME_TYPE,"application/zip");values.put(MediaStore.Downloads.IS_PENDING,1);
        Uri uri=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        if(uri==null)throw new IOException("保存先を作成できません");
        try {
            try(OutputStream output=context.getContentResolver().openOutputStream(uri);
                    ZipOutputStream zip=new ZipOutputStream(output)) {
                zip.putNextEntry(new ZipEntry("box_inventory.json"));
                zip.write(repo.inventory.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
                for(String relative:files) {
                    File file=new File(repo.root,relative).getCanonicalFile();
                    if(!file.toPath().startsWith(repo.root.getCanonicalFile().toPath())||!file.isFile())
                        throw new IOException("候補画像の参照が不正です");
                    append(zip,repo.root,file);
                }
            }
            values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);
            context.getContentResolver().update(uri,values,null,null);
            return "Download/"+name;
        }catch(Exception e){context.getContentResolver().delete(uri,null,null);throw e;}
    }

    private static void append(ZipOutputStream zip, File root, File file) throws IOException {
        String relative=InventoryArchivePath.relative(root,file);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) append(zip, root, child);
        } else if (!file.getName().endsWith(".tmp")) {
            zip.putNextEntry(new ZipEntry(relative));
            try (InputStream input = new FileInputStream(file)) { byte[] buffer = new byte[32768]; int n; while ((n = input.read(buffer)) != -1) zip.write(buffer, 0, n); }
            zip.closeEntry();
        }
    }
}
