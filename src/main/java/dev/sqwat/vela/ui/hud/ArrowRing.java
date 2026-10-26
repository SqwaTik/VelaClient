package dev.sqwat.vela.ui.hud;

/** Packs nearby bearings on one circle; icon size and radius never differ by player. */
public final class ArrowRing {
    private ArrowRing(){}
    public static float radius(int count,float footprint){return Math.max(64,count*(footprint+8)/(float)(Math.PI*2));}
    public static void spread(float[] angles,int count,float gap,int[] order,float[] packed){
        if(count<2)return;
        float turn=(float)(Math.PI*2);gap=Math.min(gap,turn/count);
        for(int i=0;i<count;i++){
            int j=i;while(j>0&&angles[order[j-1]]>angles[i]){order[j]=order[j-1];j--;}
            order[j]=i;
        }
        int start=0;float largest=-1;
        for(int i=0;i<count;i++){
            float distance=angles[order[(i+1)%count]]-angles[order[i]]+(i==count-1?turn:0);
            if(distance>largest){largest=distance;start=(i+1)%count;}
        }
        float base=angles[order[start]],sum=0;
        for(int i=0;i<count;i++){
            float original=angles[order[(start+i)%count]]+(start+i>=count?turn:0);
            sum+=original;packed[i]=i==0?original:Math.max(original,packed[i-1]+gap);
        }
        packed[count-1]=Math.min(packed[count-1],base+turn-gap);
        for(int i=count-2;i>=0;i--)packed[i]=Math.min(packed[i],packed[i+1]-gap);
        float packedSum=0;for(int i=0;i<count;i++)packedSum+=packed[i];
        float shift=(sum-packedSum)/count;
        for(int i=0;i<count;i++)angles[order[(start+i)%count]]=packed[i]+shift;
    }
}
