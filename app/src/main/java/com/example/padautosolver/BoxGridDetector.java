package com.example.padautosolver;
import android.graphics.*;import java.util.*;
final class BoxGridDetector {
 static final class Row {final float y;final List<Rect> cells=new ArrayList<>();final List<Long> hashes=new ArrayList<>();Row(float y){this.y=y;}long[] signature(){long[] r=new long[hashes.size()];for(int i=0;i<r.length;i++)r[i]=hashes.get(i);return r;}}
 static List<Row> detect(Bitmap b,List<StagePolicy.Item> items,BoxCalibration c){
  int pitch=Math.round(c.pitchY*b.getHeight()),half=Math.round(c.iconWidth*b.getWidth()/2);
  int min=Math.round(c.top*b.getHeight()),max=Math.round(c.bottom*b.getHeight());
  int bestY=-1;double best=0;
  for(int y=min;y<min+pitch;y++){
   double sum=0;int count=0;
   for(int yy=y;yy+half*2<max;yy+=pitch)for(int col=0;col<c.columns;col++){
    int cx=Math.round(b.getWidth()*(c.firstX+col*c.pitchX));double[] mean=new double[3],square=new double[3],above=new double[3];
    for(int k=0;k<13;k++){int x=cx+Math.round((-5+k*5)*b.getWidth()/1220f);int pixel=b.getPixel(x,yy),upper=b.getPixel(x,Math.max(0,yy-4));for(int ch=0;ch<3;ch++){int v=pixel>>(ch*8)&255;mean[ch]+=v/13.0;square[ch]+=v*v/13.0;above[ch]+=(upper>>(ch*8)&255)/13.0;}}
    double variance=0,change=0;for(int ch=0;ch<3;ch++){variance+=Math.sqrt(Math.max(0,square[ch]-mean[ch]*mean[ch]))/3;change+=Math.abs(mean[ch]-above[ch])/3;}
    sum+=Math.max(0,30-variance)/30*Math.min(change/30,1);count++;
   }
   if(count>0&&sum/count>best){best=sum/count;bestY=y;}
  }
  if(bestY<0||best<.55)return Collections.emptyList();
  List<Row> rows=new ArrayList<>();
  for(int top=bestY;top+half*2<max;top+=pitch){Row row=new Row(top+half+b.getWidth()*.075f);
   for(int col=0;col<c.columns;col++){int cx=Math.round(b.getWidth()*(c.firstX+col*c.pitchX));Rect rect=new Rect(cx-half,top,cx+half,top+half*2);if(!occupied(b,rect))continue;row.cells.add(rect);row.hashes.add(hash(b,rect));}
   if(!row.cells.isEmpty())rows.add(row);
  }return rows;
 }
 static boolean occupied(Bitmap b,Rect r){float[] hsv=new float[3];int color=0;for(int y=0;y<5;y++)for(int x=0;x<5;x++){android.graphics.Color.colorToHSV(b.getPixel(r.left+5+x*4,r.top+5+y*4),hsv);if(hsv[1]>.5f&&hsv[2]>.35f)color++;}if(color>=8)return true;
  double[] mean=new double[3],square=new double[3],above=new double[3];
  for(int k=0;k<13;k++){int x=r.centerX()+Math.round((-5+k*5)*b.getWidth()/1220f);int p=b.getPixel(x,r.top),q=b.getPixel(x,Math.max(0,r.top-4));for(int ch=0;ch<3;ch++){int v=p>>(ch*8)&255;mean[ch]+=v/13.0;square[ch]+=v*v/13.0;above[ch]+=(q>>(ch*8)&255)/13.0;}}
  double variance=0,change=0;for(int ch=0;ch<3;ch++){variance+=Math.sqrt(Math.max(0,square[ch]-mean[ch]*mean[ch]))/3;change+=Math.abs(mean[ch]-above[ch])/3;}
  return variance<30&&change>15;
 }
 static long hash(Bitmap b,Rect r){int[] v=new int[64];int sum=0;for(int y=0;y<8;y++)for(int x=0;x<8;x++){// upper and lower bands exclude animated New and selection center
  float fy=y<4?.25f+y*.04f:.65f+(y-4)*.04f;int px=r.left+Math.round((.10f+x*.10f)*r.width()),py=r.top+Math.round(fy*r.height());int total=0;for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++){int p=b.getPixel(px+dx,py+dy);total+=((p>>16&255)*3+(p>>8&255)*6+(p&255))/10;}int value=total/49;v[y*8+x]=value;sum+=value;}
  long h=0;for(int i=0;i<64;i++)if(v[i]*64>sum)h|=1L<<i;return h;
 }
}
