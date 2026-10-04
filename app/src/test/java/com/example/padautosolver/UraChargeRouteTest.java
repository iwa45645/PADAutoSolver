package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraChargeRouteTest {
    @Test public void sevenColumnChargeIsAdjacentAndProducesARealFirstWaveMatch(){
        byte[] board=new byte[42];for(int y=0;y<6;y++)for(int x=0;x<7;x++)board[y*7+x]=(byte)((x+2*y)%6);
        board[0]=3;board[1]=3;board[9]=3;byte[] before=board.clone();
        var path=UraChargeRoute.find(board,7,6);assertArrayEquals(before,board);assertTrue(path.size()>1);
        for(int i=1;i<path.size();i++){
            int from=path.get(i-1),to=path.get(i);assertEquals(1,Math.abs(from%7-to%7)+Math.abs(from/7-to/7));
            byte tmp=board[from];board[from]=board[to];board[to]=tmp;
        }
        assertTrue(PuzzleSolver.firstWave(board,7,6).combos>0);
    }
    @Test public void hazardsAndMismatchedGeometryNeverProduceChargeRoutes(){
        byte[] board=new byte[42];board[0]=8;
        assertThrows(IllegalArgumentException.class,()->UraChargeRoute.find(board,7,6));
        assertThrows(IllegalArgumentException.class,()->UraChargeRoute.find(new byte[30],7,6));
        assertThrows(IllegalArgumentException.class,()->UraChargeRoute.find(new byte[35],7,5));
    }
}
