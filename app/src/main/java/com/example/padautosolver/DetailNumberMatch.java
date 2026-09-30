package com.example.padautosolver;

import android.graphics.Bitmap;
import java.util.*;

/** Compare every digit against the same instance's saved image. No OCR substitutions. */
final class DetailNumberMatch {
    static boolean matches(Bitmap live, Bitmap saved, int expected) {
        if(live.getWidth()!=1220||live.getHeight()!=2712||saved.getWidth()!=1220||saved.getHeight()<65)return false;
        int[] current=new int[171*47], reference=new int[171*47];
        live.getPixels(current,0,171,269,Math.round(live.getHeight()*.19f)+18,171,47);
        saved.getPixels(reference,0,171,269,18,171,47);
        return sameDigits(glyphs(current),glyphs(reference),Integer.toString(expected).length());
    }
    static boolean sameDigits(List<boolean[]> live,List<boolean[]> reference,int digits) {
        if(digits<1||digits>5||live.size()!=digits||reference.size()!=digits)return false;
        for(int i=0;i<digits;i++) {
            boolean[] a=live.get(i),b=reference.get(i);
            if(a.length!=24*36||b.length!=a.length)return false;
            int different=0;for(int j=0;j<a.length;j++)if(a[j]!=b[j])different++;
            if(different/(double)a.length>.04)return false;
        }
        return true;
    }
    static List<boolean[]> glyphs(int[] pixels) {
        final int width=171,height=47;
        List<boolean[]> result=new ArrayList<>();
        if(pixels.length!=width*height)return result;
        boolean[] white=new boolean[pixels.length],seen=new boolean[pixels.length];
        for(int i=0;i<pixels.length;i++) {
            int p=pixels[i];white[i]=((p>>16)&255)>210&&((p>>8)&255)>210&&(p&255)>210;
        }
        List<int[]> boxes=new ArrayList<>();int[] queue=new int[pixels.length];
        for(int first=0;first<pixels.length;first++)if(white[first]&&!seen[first]) {
            int head=0,tail=1;queue[0]=first;seen[first]=true;
            int left=width,right=0,top=height,bottom=0;
            while(head<tail) {
                int at=queue[head++],x=at%width,y=at/width;
                left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
                for(int next:new int[]{x>0?at-1:-1,x+1<width?at+1:-1,y>0?at-width:-1,y+1<height?at+width:-1})
                    if(next>=0&&white[next]&&!seen[next]){seen[next]=true;queue[tail++]=next;}
            }
            if(tail>20)boxes.add(new int[]{left,top,right-left+1,bottom-top+1});
        }
        if(boxes.isEmpty()||boxes.size()>5)return result;
        boxes.sort(Comparator.comparingInt(b->b[0]));
        for(int[] b:boxes) {
            if(b[2]<8||b[2]>28||b[3]<28||b[3]>37)return Collections.emptyList();
            boolean[] glyph=new boolean[24*36];
            for(int y=0;y<36;y++)for(int x=0;x<24;x++)
                glyph[y*24+x]=white[(b[1]+Math.min(b[3]-1,y*b[3]/36))*width+b[0]+Math.min(b[2]-1,x*b[2]/24)];
            result.add(glyph);
        }
        return result;
    }
}
