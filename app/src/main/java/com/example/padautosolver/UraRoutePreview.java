package com.example.padautosolver;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Small dry-run diagram; never paints over the board used for recognition. */
final class UraRoutePreview extends View {
 private final byte[] board;private final java.util.List<Integer> route;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private static final int[] COLORS={0xffe95535,0xffe2bd30,0xff49aa63,0xff368bdd,0xff965db2,0xffef83b0,0xff46627a,0xffac6ac2,0xff333333,0xff442244};
 UraRoutePreview(Context context,UraPuzzlePlan plan){super(context);board=plan.previewBoard();route=plan.path;}
 @Override protected void onDraw(Canvas canvas){
  float cell=getWidth()/6f;canvas.drawColor(0xff182847);
  for(int i=0;i<30;i++){paint.setColor(COLORS[board[i]]);canvas.drawCircle((i%6+.5f)*cell,(i/6+.5f)*cell,cell*.32f,paint);}
  paint.setColor(Color.WHITE);paint.setStrokeWidth(cell*.055f);paint.setStyle(Paint.Style.STROKE);
  Path path=new Path();for(int i=0;i<route.size();i++){int p=route.get(i);float x=(p%6+.5f)*cell,y=(p/6+.5f)*cell;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}canvas.drawPath(path,paint);
  paint.setStyle(Paint.Style.FILL);paint.setColor(Color.BLACK);paint.setTextSize(cell*.30f);paint.setTextAlign(Paint.Align.CENTER);
  int p=route.get(0);canvas.drawText("S",(p%6+.5f)*cell,(p/6+.6f)*cell,paint);
  p=route.get(route.size()-1);canvas.drawText("E",(p%6+.5f)*cell,(p/6+.6f)*cell,paint);
 }
}
