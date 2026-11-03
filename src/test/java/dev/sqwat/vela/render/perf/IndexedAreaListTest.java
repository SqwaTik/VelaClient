package dev.sqwat.vela.render.perf;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexedAreaListTest {
    @Test void answersExactlyLikeALinearScan(){
        Random random=new Random(7);
        for(int round=0;round<40;round++){
            IndexedAreaList<?> list=new IndexedAreaList<>();
            int[][] rects=new int[random.nextInt(300)][];
            for(int i=0;i<rects.length;i++){
                int big=random.nextInt(10)==0?600:40;
                int l=random.nextInt(900)-100,t=random.nextInt(600)-100;
                rects[i]=new int[]{l,t,l+random.nextInt(big),t+random.nextInt(big)};
                list.index(rects[i][0],rects[i][1],rects[i][2],rects[i][3]);
                for(int q=0;q<8;q++){
                    int qs=random.nextInt(4)==0?700:30,ql=random.nextInt(900)-100,qt=random.nextInt(600)-100;
                    int qr=ql+random.nextInt(qs),qb=qt+random.nextInt(qs);
                    boolean expected=false;
                    for(int j=0;j<=i;j++){int[] r=rects[j];if(r[0]<qr&&r[2]>ql&&r[1]<qb&&r[3]>qt){expected=true;break;}}
                    assertEquals(expected,list.intersects(ql,qt,qr,qb));
                }
            }
        }
    }
}
