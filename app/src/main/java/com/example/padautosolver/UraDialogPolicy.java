package com.example.padautosolver;
import java.util.*;
import java.util.regex.*;
/** Modal evidence is tied to its first heading. Footer assist names never authorise a base skill. */
final class UraDialogPolicy {
    static final class Read {
        final int layer;final String header,all;final StagePolicy.Item activate,back;
        Read(int layer,String header,String all,StagePolicy.Item activate,StagePolicy.Item back){this.layer=layer;this.header=header;this.all=all;this.activate=activate;this.back=back;}
        boolean named(String name){return clean(header).contains(clean(name));}
    }
    static String clean(String text){return text.replaceAll("[^\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}A-Z0-9]","");}
    static Read read(List<StagePolicy.Item> lines,int height) {
        List<StagePolicy.Item> sorted=new ArrayList<>(lines);sorted.sort(Comparator.comparingDouble(i->i.y));
        StagePolicy.Item heading=null,activate=null,back=null;int layer=0;
        StringBuilder all=new StringBuilder();
        for(var line:sorted)if(line.y>height*.55f) {
            all.append(line.text).append('|');
            if(heading==null){Matcher m=Pattern.compile("^スキル([12])").matcher(clean(line.text));if(m.find()){heading=line;layer=Integer.parseInt(m.group(1));}}
            // The fixed team's assist-free Odin modal has a bare skill title.
            if(heading==null&&clean(line.text).equals(clean("神泉槍グングニール"))){heading=line;layer=1;}
            if(line.text.equals("発動"))activate=line;
            if(line.text.equals("戻る")&&line.y<height*.93f)back=line;
            if(line.text.equals("発動戻る")&&line.y<height*.93f){activate=line;back=line;}
        }
        if(heading==null||(activate==null&&back==null))return null;
        // Coordinates are subsequently checked against the actual back-button template.
        float buttonY=activate!=null?activate.y:back.y;
        back=new StagePolicy.Item("戻る",792,buttonY);
        activate=new StagePolicy.Item("発動",428,buttonY);
        StringBuilder title=new StringBuilder();
        for(var line:sorted)if(Math.abs(line.y-heading.y)<55)title.append(line.text);
        return new Read(layer,title.toString(),all.toString(),activate,back);
    }
    static double brightTextFraction(int[] pixels) {
        if(pixels==null||pixels.length==0)return 0;int bright=0;
        for(int p:pixels)if(((p>>16)&255)>220&&((p>>8)&255)>220&&(p&255)>220)bright++;
        return bright/(double)pixels.length;
    }
    static Integer assistRemaining(Read read) {
        Matcher m=Pattern.compile("スキル2使用可能まであと([0-9]{1,3})ターン").matcher(read.all);
        return m.find()?Integer.valueOf(m.group(1)):null;
    }
    static Integer baseRemaining(Read read) {
        Matcher m=Pattern.compile("あと([0-9]{1,3})ターンで使用可能").matcher(read.all);
        return m.find()?Integer.valueOf(m.group(1)):null;
    }
}
