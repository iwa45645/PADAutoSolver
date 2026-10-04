package com.example.padautosolver;
import java.util.*;

/** Castor's two fixed spinners are never visited; all 100 color pairs prove the first-wave goal. */
final class UraDualRoulettePlan {
    static final long MASK=(1L<<12)|(1L<<17);
    private static final long[][] SHAPES=shapes();
    final byte[] source;final List<Integer> path;final long plannedAt;
    UraDualRoulettePlan(byte[] board,long mask,List<Integer> path,long now){
        validate(board,mask);this.source=board.clone();this.path=Collections.unmodifiableList(new ArrayList<>(path));plannedAt=now;
        if(path.size()<2)throw new IllegalArgumentException("No drag");
        byte[] replay=RoulettePlan.replay(board,mask,path);
        for(byte a=0;a<10;a++)for(byte b=0;b<10;b++){
            replay[12]=a;replay[17]=b;
            PuzzleSolver.MatchStats s=PuzzleSolver.firstWave(replay,6,5);
            if(!goal(s))throw new IllegalArgumentException("Spinner pair "+a+","+b+" breaks T/water/heal: "+s.firstTShapes[3]+"/"+s.colorCombos[3]+"/"+s.colorCombos[5]);
        }
    }
    private static boolean goal(PuzzleSolver.MatchStats s){return s.firstTShapes[3]>=1&&s.colorCombos[3]>=2&&s.colorCombos[5]>=1;}
    static void validate(byte[] board,long mask){
        if(mask!=MASK||board==null||board.length!=30)throw new IllegalArgumentException("Reviewed Castor spinners required");
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0&&(board[i]<0||board[i]>6))throw new IllegalArgumentException("Unknown or hazardous stable orb");
    }
    boolean current(byte[] board,long mask,long now){
        if(mask!=MASK||board==null||board.length!=30||now<plannedAt||now-plannedAt>=15000)return false;
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0&&source[i]!=board[i])return false;return true;
    }
    private static int score(byte[] board){
        byte[] b=board.clone();int water=30,t=30,heal=30,combos=30;
        // For water/T only membership in WATER matters; for heal only membership in HEAL.
        // Other colors cannot join those components. The final constructor still checks all 100 pairs.
        for(int mode=0;mode<2;mode++)for(int pair=0;pair<4;pair++){
            byte color=(byte)(mode==0?3:5);
            b[12]=(pair&1)!=0?color:(byte)0;b[17]=(pair&2)!=0?color:(byte)0;
            PuzzleSolver.MatchStats s=PuzzleSolver.firstWave(b,6,5);
            if(mode==0){water=Math.min(water,s.colorCombos[3]);t=Math.min(t,s.firstTShapes[3]);}
            else heal=Math.min(heal,s.colorCombos[5]);
            combos=Math.min(combos,s.combos);
        }
        int bestShape=0;
        for(long[] shape:SHAPES){
            int value=0;long cells=shape[0],perimeter=shape[1];
            while(cells!=0){int p=Long.numberOfTrailingZeros(cells);cells&=cells-1;if(board[p]==3)value+=150;}
            while(perimeter!=0){int p=Long.numberOfTrailingZeros(perimeter);perimeter&=perimeter-1;if(board[p]!=3)value+=60;}
            bestShape=Math.max(bestShape,value);
        }
        return (water>=2&&t>=1&&heal>=1?1000000:0)+Math.min(2,water)*10000+Math.min(1,t)*30000+Math.min(1,heal)*5000+combos*100+bestShape;
    }
    private static long[][] shapes(){
        List<long[]> shapes=new ArrayList<>();
        for(int p=0;p<30;p++)for(int axis=0;axis<2;axis++)for(int sign:new int[]{-1,1}){
            int x=p%6,y=p/6;
            if(axis==0&&(x<1||x>4||y+2*sign<0||y+2*sign>4)||axis==1&&(y<1||y>3||x+2*sign<0||x+2*sign>5))continue;
            int side=axis==0?1:6,stem=axis==0?6:1;
            int[] cells={p,p-side,p+side,p+sign*stem,p+2*sign*stem};long shape=0;boolean allowed=true;
            for(int cell:cells){shape|=1L<<cell;if((MASK&(1L<<cell))!=0)allowed=false;}
            if(!allowed)continue;
            long perimeter=0;
            for(int cell:cells)for(int n:new int[]{cell-1,cell+1,cell-6,cell+6})if(n>=0&&n<30&&Math.abs(cell%6-n%6)+Math.abs(cell/6-n/6)==1&&(shape&(1L<<n))==0)perimeter|=1L<<n;
            if((perimeter&MASK)!=0)continue;
            shapes.add(new long[]{shape,perimeter});
        }
        return shapes.toArray(new long[0][]);
    }
    private static final class Node {
        final byte[] board;final int pos,prior,depth,score;final Node parent;
        Node(byte[] b,int p,int prior,int depth,int score,Node parent){board=b;pos=p;this.prior=prior;this.depth=depth;this.score=score;this.parent=parent;}
    }
    static UraDualRoulettePlan solve(byte[] source,long mask,int steps,int width,long budget,long now){
        validate(source,mask);long started=System.nanoTime(),deadline=started+budget*1000000L;
        List<Node> beam=new ArrayList<>();Node best=null;int initial=score(source);
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0){Node n=new Node(source.clone(),i,-1,0,initial,null);beam.add(n);best=n;}
        outer:for(int depth=1;depth<=steps;depth++){
            PriorityQueue<Node> top=new PriorityQueue<>(Comparator.comparingInt(n->n.score));Set<Long> seen=new HashSet<>();
            for(Node n:beam){
                if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
                if(System.nanoTime()>deadline)break outer;
                for(int p:new int[]{n.pos-1,n.pos+1,n.pos-6,n.pos+6}){
                    if(p<0||p>=30||p==n.prior||Math.abs(p%6-n.pos%6)+Math.abs(p/6-n.pos/6)!=1||(mask&(1L<<p))!=0)continue;
                    byte[] b=n.board.clone();byte v=b[p];b[p]=b[n.pos];b[n.pos]=v;
                    long hash=Arrays.hashCode(b)*961L+p*31L+n.pos;if(!seen.add(hash))continue;
                    Node next=new Node(b,p,n.pos,depth,score(b),n);
                    if(next.score>best.score||best.depth==0&&next.score==best.score)best=next;
                    if(top.size()<width)top.add(next);else if(next.score>top.peek().score){top.poll();top.add(next);}
                }
            }if(top.isEmpty())break;beam=new ArrayList<>(top);if(best.score>=1000000)break;
        }
        List<Integer> path=new ArrayList<>();for(Node n=best;n!=null;n=n.parent)path.add(n.pos);Collections.reverse(path);
        return new UraDualRoulettePlan(source,mask,path,now+(System.nanoTime()-started)/1000000L);
    }
}
