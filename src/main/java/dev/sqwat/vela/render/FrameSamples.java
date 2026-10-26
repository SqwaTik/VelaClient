package dev.sqwat.vela.render;

import java.util.Arrays;

/** Bounded frame-interval samples. No allocation while recording a frame. */
public final class FrameSamples {
    private final double[] values;
    private int cursor,count;
    public FrameSamples(int capacity){if(capacity<1)throw new IllegalArgumentException();values=new double[capacity];}
    public void add(double milliseconds){
        if(!Double.isFinite(milliseconds)||milliseconds<=0)return;
        values[cursor]=milliseconds;cursor=(cursor+1)%values.length;count=Math.min(count+1,values.length);
    }
    public record Summary(int frames,double averageMs,double p95Ms,double p99Ms,double maxMs,int over16Ms){}
    public Summary summary(){
        if(count==0)return new Summary(0,0,0,0,0,0);
        double[] copy=Arrays.copyOf(values,count);Arrays.sort(copy);double sum=0;int stalls=0;for(double value:copy){sum+=value;if(value>16.67)stalls++;}
        return new Summary(count,sum/count,copy[(int)Math.ceil(count*.95)-1],copy[(int)Math.ceil(count*.99)-1],copy[count-1],stalls);
    }
}
