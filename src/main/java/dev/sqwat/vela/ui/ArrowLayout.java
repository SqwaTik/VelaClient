package dev.sqwat.vela.ui;

/** Circular isotonic spacing. Fixed buffers, one radius, stable angular order. */
public final class ArrowLayout {
    private static final double TAU=Math.PI*2;
    private final double[] sorted,unwrapped,means;
    private final int[] order,rotated,weights,starts;
    private final float[] result;
    public ArrowLayout(int capacity){sorted=new double[capacity];unwrapped=new double[capacity];means=new double[capacity];order=new int[capacity];rotated=new int[capacity];weights=new int[capacity];starts=new int[capacity];result=new float[capacity];}
    public float[] place(float[] angles,int count,float radius,float diameter){
        if(count<0||count>result.length||count>angles.length)throw new IllegalArgumentException("Arrow count");
        if(count==0)return result;
        for(int i=0;i<count;i++){
            double a=angles[i]%TAU;if(a<0)a+=TAU;
            int j=i;while(j>0&&sorted[j-1]>a){sorted[j]=sorted[j-1];order[j]=order[j-1];j--;}
            sorted[j]=a;order[j]=i;
        }
        int first=0;double largest=-1;
        for(int i=0;i<count;i++){double gap=(i+1<count?sorted[i+1]:sorted[0]+TAU)-sorted[i];if(gap>largest){largest=gap;first=(i+1)%count;}}
        double gap=Math.min(TAU/count,2*Math.asin(Math.min(1,diameter/(2*Math.max(1,radius)))));
        int blocks=0;
        for(int i=0;i<count;i++){
            int k=(first+i)%count;rotated[i]=order[k];unwrapped[i]=sorted[k]+(k<first?TAU:0);
            means[blocks]=unwrapped[i]-i*gap;weights[blocks]=1;starts[blocks]=i;blocks++;
            while(blocks>1&&means[blocks-2]>means[blocks-1]){
                int a=blocks-2,b=blocks-1;means[a]=(means[a]*weights[a]+means[b]*weights[b])/(weights[a]+weights[b]);weights[a]+=weights[b];blocks--;
            }
        }
        double range=Math.max(0,TAU-count*gap),mid=(means[0]+means[blocks-1])*.5;
        for(int b=0;b<blocks;b++)for(int i=starts[b];i<starts[b]+weights[b];i++)result[rotated[i]]=(float)(Math.clamp(means[b],mid-range*.5,mid+range*.5)+i*gap);
        return result;
    }
}
