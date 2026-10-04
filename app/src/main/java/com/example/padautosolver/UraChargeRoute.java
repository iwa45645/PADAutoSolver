package com.example.padautosolver;
import java.util.*;
/** One real, replayable first-wave match; no skyfall or zero-combo charge assumptions. */
final class UraChargeRoute {
    static List<Integer> find(byte[] source,int cols,int rows) {
        if(source==null||source.length!=cols*rows||!((cols==6&&rows==5)||(cols==7&&rows==6)))throw new IllegalArgumentException("Unsupported charge geometry");
        for(byte c:source)if(c<0||c>6)throw new IllegalArgumentException("Unknown or damaging orb");
        for(int depth=1;depth<=5;depth++)for(int start=0;start<source.length;start++){
            List<Integer> path=new ArrayList<>(List.of(start));
            if(search(source.clone(),cols,rows,path,depth))return Collections.unmodifiableList(path);
        }
        throw new IllegalArgumentException("No first-wave charge match");
    }
    private static boolean search(byte[] b,int cols,int rows,List<Integer> path,int depth) {
        if(path.size()>1&&triple(b,cols,rows))return true;
        if(depth==0)return false;int from=path.get(path.size()-1);
        for(int to:new int[]{from-1,from+1,from-cols,from+cols}){
            if(to<0||to>=b.length||Math.abs(from%cols-to%cols)+Math.abs(from/cols-to/cols)!=1)continue;
            byte tmp=b[from];b[from]=b[to];b[to]=tmp;path.add(to);
            if(search(b,cols,rows,path,depth-1))return true;
            path.remove(path.size()-1);tmp=b[from];b[from]=b[to];b[to]=tmp;
        }
        return false;
    }
    private static boolean triple(byte[] b,int cols,int rows){
        for(int i=0;i<b.length;i++)for(int d:new int[]{1,cols}){
            int k=i+2*d;if(k>=b.length||d==1&&i/cols!=k/cols)continue;
            if(b[i]==b[i+d]&&b[i]==b[k])return true;
        }
        return false;
    }
}
