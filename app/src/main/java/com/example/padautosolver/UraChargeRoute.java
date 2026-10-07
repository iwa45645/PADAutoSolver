package com.example.padautosolver;
import java.util.*;
/** One real, replayable first-wave match; no skyfall or zero-combo charge assumptions. */
final class UraChargeRoute {
    static List<Integer> find(byte[] source,int cols,int rows) {
        return find(source,cols,rows,0);
    }
    static List<Integer> find(byte[] source,int cols,int rows,long mask) {
        return find(source,cols,rows,mask,false);
    }
    static List<Integer> find(byte[] source,int cols,int rows,long mask,boolean quiet) {
        return find(source,cols,rows,mask,quiet,false);
    }
    static List<Integer> findHealing(byte[] source) {
        return find(source,6,5,0,true,true);
    }
    private static List<Integer> find(byte[] source,int cols,int rows,long mask,boolean quiet,boolean healing) {
        if(source==null||source.length!=cols*rows||!((cols==6&&rows==5)||(cols==7&&rows==6)))throw new IllegalArgumentException("Unsupported charge geometry");
        if(mask<0||(mask>>>source.length)!=0)throw new IllegalArgumentException("Invalid roulette mask");
        if(quiet&&Long.bitCount(mask)>2)throw new IllegalArgumentException("Too many unverified charge phases");
        for(int i=0;i<source.length;i++)if((mask&(1L<<i))==0&&(source[i]<0||source[i]>6))throw new IllegalArgumentException("Unknown or damaging orb");
        for(int depth=1;depth<=5;depth++)for(int start=0;start<source.length;start++)if((mask&(1L<<start))==0){
            List<Integer> path=new ArrayList<>(List.of(start));
            if(search(source.clone(),cols,rows,path,depth,mask,quiet,healing))return Collections.unmodifiableList(path);
        }
        throw new IllegalArgumentException("No first-wave charge match");
    }
    private static boolean search(byte[] b,int cols,int rows,List<Integer> path,int depth,long mask,boolean quiet,boolean healing) {
        if(path.size()>1&&triple(b,cols,rows,mask)&&(!quiet||quiet(b,cols,rows,mask))&&(!healing||PuzzleSolver.firstWave(b,cols,rows).colorCombos[5]>0))return true;
        if(depth==0)return false;int from=path.get(path.size()-1);
        for(int to:new int[]{from-1,from+1,from-cols,from+cols}){
            if(to<0||to>=b.length||(mask&(1L<<to))!=0||Math.abs(from%cols-to%cols)+Math.abs(from/cols-to/cols)!=1)continue;
            byte tmp=b[from];b[from]=b[to];b[to]=tmp;path.add(to);
            if(search(b,cols,rows,path,depth-1,mask,quiet,healing))return true;
            path.remove(path.size()-1);tmp=b[from];b[from]=b[to];b[to]=tmp;
        }
        return false;
    }
    private static boolean quiet(byte[] b,int cols,int rows,long mask) {
        byte[] copy=b.clone();int first=Long.numberOfTrailingZeros(mask),second=Long.numberOfTrailingZeros(mask&(mask-1));
        for(byte a=0;a<(mask==0?1:10);a++)for(byte c=0;c<(Long.bitCount(mask)<2?1:10);c++) {
            if(first<copy.length)copy[first]=a;if(second<copy.length)copy[second]=c;
            PuzzleSolver.MatchStats stats=PuzzleSolver.firstWave(copy,cols,rows);
            if(stats.firstTShapes[3]>0||stats.colorCombos[3]>=2)return false;
        }return true;
    }
    private static boolean triple(byte[] b,int cols,int rows,long mask){
        for(int i=0;i<b.length;i++)for(int d:new int[]{1,cols}){
            int k=i+2*d;if(k>=b.length||d==1&&i/cols!=k/cols)continue;
            if((mask&((1L<<i)|(1L<<(i+d))|(1L<<k)))==0&&b[i]==b[i+d]&&b[i]==b[k])return true;
        }
        return false;
    }
}
