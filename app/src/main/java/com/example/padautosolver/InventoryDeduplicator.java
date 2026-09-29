package com.example.padautosolver;
import java.util.*;
/** Align only adjacent page suffix/prefix rows. Identical monsters remain separate occurrences. */
public final class InventoryDeduplicator {
 public static boolean same(long[] a,long[] b){if(a.length!=b.length)return false;int distance=0;for(int i=0;i<a.length;i++){int d=Long.bitCount(a[i]^b[i]);if(d>16)return false;distance+=d;}return distance<=10*a.length;}
 public static int overlap(List<long[]> previous,List<long[]> next){
  int result=0,matches=0;
  for(int n=1;n<=Math.min(previous.size(),next.size());n++){
   boolean yes=true;for(int i=0;i<n;i++)yes&=same(previous.get(previous.size()-n+i),next.get(i));
   if(yes){result=n;matches++;}
  }
  if(matches>1)throw new IllegalStateException("重なりが複数候補です。同一個体と別個体を区別できないため停止");
  if(result==0)throw new IllegalStateException("ページの重なりを確認できません。スクロール距離を小さくしてください");
  return result;
 }
 public static boolean pageSame(List<long[]> a,List<long[]> b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++)if(!same(a.get(i),b.get(i)))return false;return true;}
 public static boolean endVerified(int unchanged,int observed,int expected,boolean top,boolean filters){return unchanged>=3&&expected>0&&observed==expected&&top&&filters;}
}
