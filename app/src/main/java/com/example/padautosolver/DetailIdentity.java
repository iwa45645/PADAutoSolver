package com.example.padautosolver;

import java.util.List;
import java.util.regex.*;

/** Header-only OCR result. This is an observation, never a human confirmation. */
final class DetailIdentity {
    final int id;
    final String name;
    DetailIdentity(int id, String name) { this.id=id; this.name=name; }
    static DetailIdentity parse(List<StagePolicy.Item> lines, int height) {
        Pattern number=Pattern.compile("(?i)NO[.．:：]?\\s*([0-9]{1,6})(?![0-9A-Za-z])");
        Integer id=null; String name=null; float numberY=0;
        for(StagePolicy.Item line:lines) {
            if(line.y<height*.195f||line.y>height*.238f)continue;
            Matcher m=number.matcher(line.rawText);
            if(m.find()) {
                int found=Integer.parseInt(m.group(1));
                if(found<1 || (id!=null && id!=found))return null;
                id=found;numberY=line.y;
                String rest=line.rawText.substring(m.end()).trim();
                if(validName(rest))name=rest;
            }
        }
        if(id==null)return null;
        if(name==null)for(StagePolicy.Item line:lines) {
            if(line.y>numberY && line.y<height*.24f && validName(line.rawText)) {
                if(name==null)name=line.rawText.trim();
                else if(!name.equals(line.rawText.trim()))return null;
            }
        }
        return new DetailIdentity(id,name);
    }
    static boolean hasHeader(List<StagePolicy.Item> lines,int height) {
        for(StagePolicy.Item line:lines)if(line.y>height*.195f&&line.y<height*.218f&&line.text.matches("NO[.．:].+"))return true;
        return false;
    }
    private static boolean validName(String s) {
        return s.length()>=2 && s.matches(".*[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}].*")
                && !s.contains("戻る") && !s.contains("スキル") && !s.contains("モンスターBOX");
    }
    boolean same(DetailIdentity other){return other!=null&&id==other.id&&java.util.Objects.equals(name,other.name);}
}
