package com.example.padautosolver;
import java.util.*;
import java.util.regex.*;
/** Actual battle cooldown rows, as shown while the portrait is held down. */
final class UraHeldSkillInfo {
    final String baseName,assistName;
    final Integer baseRemaining,assistRemaining;
    String actorName="";
    String headerName="";
    UraHeldSkillInfo(String base,String assist,Integer baseTurns,Integer assistTurns){baseName=base;assistName=assist;baseRemaining=baseTurns;assistRemaining=assistTurns;}
    boolean baseNamed(String name){String wanted=UraDialogPolicy.clean(name);return UraDialogPolicy.clean(baseName).contains(wanted)||UraDialogPolicy.clean(headerName).contains(wanted);}
    boolean assistNamed(String name){return assistName!=null&&UraDialogPolicy.clean(assistName).contains(UraDialogPolicy.clean(name));}
    boolean actorNamed(String name){return UraDialogPolicy.clean(actorName).contains(UraDialogPolicy.clean(name));}
    static UraHeldSkillInfo read(List<StagePolicy.Item> lines) {
        String[] names=new String[2];Integer[] turns=new Integer[2];
        for(var line:lines) {
            String text=line.text.replace('１','1').replace('２','2');
            Matcher row=Pattern.compile("(?:ス)?キル([12])[:：](.+)").matcher(text);
            if(!row.find())continue;
            int index=Integer.parseInt(row.group(1))-1;String tail=row.group(2);
            if(!tail.matches(".*(?:あと[0-9]{1,3}ターン|使用可能|使用後[0-9]{1,3}ターン).*$")) {
                String aligned=null;
                for(var part:lines)if(!part.text.startsWith("UCD")&&part.x>800&&Math.abs(part.y-line.y)<12&&part.text.matches("あと[0-9]{1,3}ターン|使用可能|使用後[0-9]{1,3}ターン")) {
                    if(aligned!=null&&!aligned.equals(part.text))return null;aligned=part.text;
                }
                if(aligned!=null)tail+=aligned;
            }
            Matcher remain=Pattern.compile("あと([0-9]{1,3})(?![0-9A-Z])ターン").matcher(tail);
            int value;String name;
            if(remain.find()){value=Integer.parseInt(remain.group(1));name=tail.substring(0,remain.start());}
            else if(tail.endsWith("使用可能")){value=0;name=tail.substring(0,tail.length()-4);}
            else {
                // Overcharged base rows say '使用後5ターン'. This is not a readiness proof.
                Matcher after=Pattern.compile("使用後([0-9]{1,3})ターン").matcher(tail);
                if(!after.find())continue;value=-1;name=tail.substring(0,after.start());
            }
            if(names[index]!=null&&(!UraDialogPolicy.clean(names[index]).equals(UraDialogPolicy.clean(name))||turns[index]!=value))return null;
            names[index]=name;turns[index]=value;
        }
        List<StagePolicy.Item> values=new ArrayList<>();
        for(var line:lines)if(line.text.startsWith("UCD_")&&line.text.matches("UCD_(?:あと[0-9]{1,3}ターン|使用可能|使用後[0-9]{1,3}ターン)"))values.add(line);
        values.sort(Comparator.comparingDouble(i->i.y));
        if(!values.isEmpty()) {
            List<StagePolicy.Item> unique=new ArrayList<>();
            for(var value:values) {
                if(!unique.isEmpty()&&Math.abs(value.y-unique.get(unique.size()-1).y)<12) {
                    if(!value.text.equals(unique.get(unique.size()-1).text))return null;
                }else unique.add(value);
            }
            if(unique.size()>2)return null;
            for(int i=0;i<unique.size();i++) {
                String value=unique.get(i).text.substring(4);
                Matcher m=Pattern.compile("あと([0-9]{1,3})ターン").matcher(value);
                int actual=m.matches()?Integer.parseInt(m.group(1)):value.equals("使用可能")?0:-1;
                turns[i]=actual;if(names[i]==null)names[i]="";
            }
        }
        Integer[] column=new Integer[2];
        for(var line:lines)if(line.text.startsWith("UCD1_")||line.text.startsWith("UCD2_")) {
            int index=line.text.charAt(3)-'1';String tail=line.text.substring(5);
            // The calibrated cooldown column sometimes omits the kana ター; digits remain literal.
            Matcher m=Pattern.compile("^あと([0-9]{1,3})(?:ターン|ーン)$").matcher(tail);
            Integer n=null;
            if(m.find())n=Integer.valueOf(m.group(1));
            else if(tail.equals("使用可能"))n=Integer.valueOf(0);
            else if(tail.matches("使用後[0-9]{1,3}ターン"))n=Integer.valueOf(-1);
            if(n==null)continue;
            if(column[index]!=null&&!column[index].equals(n))return null;column[index]=n;
        }
        for(int i=0;i<2;i++)if(column[i]!=null) {
            if(turns[i]!=null&&!turns[i].equals(column[i]))return null;
            turns[i]=column[i];
        }
        if(turns[0]==null&&turns[1]==null)return null;
        if(names[0]==null)names[0]="";
        UraHeldSkillInfo info=new UraHeldSkillInfo(names[0],names[1],turns[0],turns[1]);
        for(var line:lines)if(line.text.startsWith("USH1_"))info.headerName+=line.text.substring(5);
        for(var line:lines)if(line.y<900&&!line.text.startsWith("UCD")&&!line.text.startsWith("USH"))info.actorName+=line.text;
        return info;
    }
}
