package com.example.padautosolver;
import java.util.*;
/** Align only adjacent page suffix/prefix rows. Identical monsters remain separate occurrences. */
public final class InventoryDeduplicator {
 public static boolean same(long[] a,long[] b){if(a.length!=b.length)return false;int distance=0;for(int i=0;i<a.length;i++){int d=Long.bitCount(a[i]^b[i]);if(d>16)return false;distance+=d;}return distance<=10*a.length;}
 public static int overlap(List<long[]> previous,List<long[]> next){
  int result=0,matches=0;
  for(int n=1;n<=Math.min(previous.size(),next.size());n++){
   boolean yes=true;int distance=0,cells=0,strongRows=0;
   for(int i=0;i<n;i++){
    long[] a=previous.get(previous.size()-n+i),b=next.get(i);
    if(a.length!=b.length){yes=false;break;}
    if(same(a,b))strongRows++;
    for(int j=0;j<a.length;j++){int d=Long.bitCount(a[j]^b[j]);if(d>16)yes=false;distance+=d;cells++;}
   }
   // A moving badge can alter one row. Accept it only with several independent matching rows.
   yes &= distance<=10*cells && (strongRows==n || (n>=3 && strongRows>n/2));
   if(yes){result=n;matches++;}
  }
  if(matches>1)throw new IllegalStateException("重なりが複数候補です。同一個体と別個体を区別できないため停止");
  if(result==0)throw new IllegalStateException("ページの重なりを確認できません。スクロール距離を小さくしてください");
  return result;
 }
 public static boolean pageSame(List<long[]> a,List<long[]> b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++)if(!same(a.get(i),b.get(i)))return false;return true;}
 public static boolean stableFrame(List<long[]> a,List<long[]> b){
  if(a.size()!=b.size()||a.isEmpty())return false;int matching=0;
  for(int i=0;i<a.size();i++){if(a.get(i).length!=b.get(i).length)return false;if(same(a.get(i),b.get(i)))matching++;}
  return matching==a.size() || (a.size()>=3&&matching>=Math.ceil(a.size()*.6));
 }
 public static boolean endVerified(int unchanged,int observed,int expected,boolean top,boolean filters){return unchanged>=3&&expected>0&&observed==expected&&top&&filters;}
}
