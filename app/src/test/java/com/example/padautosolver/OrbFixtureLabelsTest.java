package com.example.padautosolver;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;
import java.io.DataInputStream;
import java.util.zip.GZIPInputStream;
import java.security.MessageDigest;
import static org.junit.Assert.*;

/** Independent image pairs catch conflicting labels that would make a known orb UNKNOWN. */
public class OrbFixtureLabelsTest {
    private int[] pixels(File root,String name)throws Exception {
        byte[] png=Files.readAllBytes(new File(root,name).toPath());
        StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(png))hash.append(String.format("%02x",b&255));
        try(var input=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/orb-label-audit-"+name+".argb.gz")))) {
            assertEquals("Golden pixels must describe the current PNG",input.readUTF(),hash.toString());
            int[] pixels=new int[input.readInt()];for(int i=0;i<pixels.length;i++)pixels[i]=input.readInt();return pixels;
        }
    }
    @Test public void reviewedMatchingImagesHaveConsistentColorLabels()throws Exception {
        File root=new File("src/main/assets/ura-shura");
        if(!root.isDirectory())root=new File("app/src/main/assets/ura-shura");
        String manifest=new String(Files.readAllBytes(new File(root,"normal-orbs.json").toPath()),java.nio.charset.StandardCharsets.UTF_8);
        Matcher matcher=Pattern.compile("\"file\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"color\"\\s*:\\s*(\\d+)").matcher(manifest);
        Map<String,Integer> labels=new HashMap<>();
        while(matcher.find())assertNull("Duplicate fixture",labels.put(matcher.group(1),Integer.valueOf(matcher.group(2))));
        assertTrue(labels.size()>100);
        Map<String,Integer> identicalImages=new HashMap<>();
        for(var entry:labels.entrySet()){
            byte[] bytes=Files.readAllBytes(new File(root,entry.getKey()).toPath());
            String hash=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes));
            Integer previous=identicalImages.putIfAbsent(hash,entry.getValue());
            if(previous!=null)assertEquals("Identical image has conflicting color: "+entry.getKey(),previous,entry.getValue());
        }
        String[][] pairs={
            {"b2-light-water-20261003-19.png","normal-run2-1-19.png"},
            {"b2-light-water-20261003-22.png","b2-all-20261003-0-22.png"},
            {"b2-light-water-20261003-26.png","b2-all-20261003-0-22.png"},
            {"b2-light-water-20261003-29.png","b1-entry-20261003-29.png"},
            {"b2-first-20261003-15.png","b2-orb-17.png"}};
        for(String[] pair:pairs) {
            int[] left=pixels(root,pair[0]),right=pixels(root,pair[1]);
            // This bounds differences between two reviewed animation poses;
            // runtime acceptance retains its stricter distance and color margin.
            assertTrue(pair[0],TeamIconMatch.distance(left,right)<.12);
            assertNotNull(labels.get(pair[0]));assertEquals(pair[0],labels.get(pair[1]),labels.get(pair[0]));
        }
    }
}
