package com.example.padautosolver;
import java.util.*;
/** One overwritten roulette, excluded from the drag; every possible color is proved.
 * This deliberately avoids timing a color transition across capture/dispatch latency. */
final class RoulettePlan {
    final byte[] source;
    final long mask,plannedAt;
    final List<Integer> path;
    final int water,heal,combos;
    RoulettePlan(byte[] source,long mask,List<Integer> path,long now) {
        validate(source,mask);this.source=source.clone();this.mask=mask;plannedAt=now;
        this.path=Collections.unmodifiableList(new ArrayList<>(path));
        byte[] replay=replay(source,mask,path);
        int[] worst=worst(replay,mask);water=worst[0];heal=worst[1];combos=worst[2];
        if(path.size()<2||water<2||heal<1)throw new IllegalArgumentException("No phase-independent water2/heal1");
    }
    static void validate(byte[] b,long mask) {
        if(b==null||b.length!=30||Long.bitCount(mask)!=1||(mask>>>30)!=0||mask<0)throw new IllegalArgumentException("One verified roulette required");
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0&&(b[i]<0||b[i]>9))throw new IllegalArgumentException("Unknown stable orb");
    }
    static byte[] replay(byte[] source,long mask,List<Integer> path) {
        byte[] b=source.clone();int prior=-1;
        for(int p:path) {
            if(p<0||p>=30||(mask&(1L<<p))!=0)throw new IllegalArgumentException("Route visits roulette/outside board");
            if(prior>=0){if(Math.abs(p%6-prior%6)+Math.abs(p/6-prior/6)!=1)throw new IllegalArgumentException("Nonadjacent route");byte v=b[p];b[p]=b[prior];b[prior]=v;}
            prior=p;
        }return b;
    }
    /** Charging requires a real combo: zero combos do not charge PAD skills.
     * A triple formed entirely by stable cells survives every spinner phase. */
    static List<Integer> chargeRoute(byte[] source,long mask) {
        if(source==null||source.length!=30||mask<0||(mask>>>30)!=0)throw new IllegalArgumentException("Invalid charge board");
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0&&(source[i]<0||source[i]>6))throw new IllegalArgumentException("Unknown or damaging charge orb");
        for(int depth=1;depth<=5;depth++)for(int start=0;start<30;start++)if((mask&(1L<<start))==0) {
            List<Integer> path=new ArrayList<>(List.of(start));
            if(chargeSearch(source.clone(),mask,path,depth))return path;
        }throw new IllegalArgumentException("No guaranteed charging combo");
    }
    private static boolean chargeSearch(byte[] b,long mask,List<Integer> path,int remaining) {
        if(path.size()>1&&stableTriple(b,mask))return true;
        if(remaining==0)return false;int from=path.get(path.size()-1);
        for(int to:new int[]{from-1,from+1,from-6,from+6}) {
            if(to<0||to>=30||(mask&(1L<<to))!=0||Math.abs(from%6-to%6)+Math.abs(from/6-to/6)!=1)continue;
            byte v=b[from];b[from]=b[to];b[to]=v;path.add(to);
            if(chargeSearch(b,mask,path,remaining-1))return true;
            path.remove(path.size()-1);v=b[from];b[from]=b[to];b[to]=v;
        }return false;
    }
    static boolean stableTriple(byte[] b,long mask) {
        for(int i=0;i<30;i++)for(int d:new int[]{1,6}) {
            int j=i+d,k=i+2*d;if(k>=30||d==1&&i/6!=k/6||(mask&((1L<<i)|(1L<<j)|(1L<<k)))!=0)continue;
            if(b[i]>=0&&b[i]<=6&&b[i]==b[j]&&b[i]==b[k])return true;
        }return false;
    }
    static int[] worst(byte[] b,long mask) {
        int p=Long.numberOfTrailingZeros(mask),water=30,heal=30,combos=30;
        // All ten recognized orb types, a superset of Yukine's four-color cycle.
        // Includes water that can join two matched components into one.
        byte[] copy=b.clone();
        for(byte color=0;color<10;color++) {
            copy[p]=color;PuzzleSolver.MatchStats s=PuzzleSolver.firstWave(copy,6,5);
            water=Math.min(water,s.colorCombos[3]);heal=Math.min(heal,s.colorCombos[5]);combos=Math.min(combos,s.combos);
        }return new int[]{water,heal,combos};
    }
    boolean current(byte[] live,long liveMask,long now) {
        if(live==null||live.length!=30||mask!=liveMask||now<plannedAt||now-plannedAt>15000)return false;
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0&&source[i]!=live[i])return false;
        return true;
    }
    private static final class Node {
        byte[] b;int p,prev,depth,score;Node parent;
        Node(byte[] b,int p,int prev,int depth,int score,Node parent){this.b=b;this.p=p;this.prev=prev;this.depth=depth;this.score=score;this.parent=parent;}
    }
    static RoulettePlan solve(byte[] source,long mask,int steps,int width,long budget,long now) {
        validate(source,mask);long deadline=System.nanoTime()+budget*1000000L;List<Node> beam=new ArrayList<>();Node best=null;
        int score=score(source,mask);for(int i=0;i<30;i++)if((mask&(1L<<i))==0){Node n=new Node(source.clone(),i,-1,0,score,null);beam.add(n);best=n;}
        outer:for(int depth=1;depth<=steps;depth++) {
            PriorityQueue<Node> top=new PriorityQueue<>(Comparator.comparingInt(n->n.score));Set<Long> seen=new HashSet<>();
            for(Node n:beam) {
                if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
                if(System.nanoTime()>deadline)break outer;
                for(int p:new int[]{n.p-1,n.p+1,n.p-6,n.p+6}) {
                    if(p<0||p>=30||p==n.prev||Math.abs(p%6-n.p%6)+Math.abs(p/6-n.p/6)!=1||(mask&(1L<<p))!=0)continue;
                    byte[] b=n.b.clone();byte v=b[p];b[p]=b[n.p];b[n.p]=v;
                    long hash=Arrays.hashCode(b)*961L+p*31L+n.p;if(!seen.add(hash))continue;
                    Node c=new Node(b,p,n.p,depth,score(b,mask),n);
                    if(c.score>best.score||best.depth==0&&c.score==best.score)best=c;
                    if(top.size()<width)top.add(c);else if(c.score>top.peek().score){top.poll();top.add(c);}
                }
            }if(top.isEmpty())break;beam=new ArrayList<>(top);
            // Retain a short route as soon as the strict worst-phase goal is achieved.
            if(best.score>=1000000)break;
        }
        List<Integer> path=new ArrayList<>();for(Node n=best;n!=null;n=n.parent)path.add(n.p);Collections.reverse(path);
        return new RoulettePlan(source,mask,path,now);
    }
    private static int score(byte[] b,long mask) {
        int[] w=worst(b,mask);int score=(w[0]>=2&&w[1]>=1?1000000:0)+Math.min(2,w[0])*10000+Math.min(1,w[1])*5000+w[2]*100;
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0)for(int d:new int[]{1,6}) {
            int j=i+d,k=i+2*d;if(k>=30||d==1&&i/6!=k/6||(mask&((1L<<j)|(1L<<k)))!=0)continue;
            if(b[i]==3||b[i]==5){if(b[i]==b[j])score+=30;if(b[i]==b[k])score+=30;}
        }return score;
    }
}
