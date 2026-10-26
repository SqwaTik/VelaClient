package dev.sqwat.vela.ui;

public final class HudSnap {
    private HudSnap() {}
    public static double origin(double value,double origin,double unitsPerPixel,boolean bypass){
        if(bypass||!Double.isFinite(unitsPerPixel)||unitsPerPixel<=0)return value;
        return Math.abs(value-origin)<=12*unitsPerPixel?origin:value;
    }
    public static int grid(int value){return Math.round(value/8f)*8;}
    public static int align(int value,int size,int start,int end){
        int best=value,delta=9;
        for(int target:new int[]{start,end,(start+end)/2})for(int offset:new int[]{0,size,size/2}){
            int candidate=target-offset,d=Math.abs(candidate-value);if(d<delta){delta=d;best=candidate;}
        }
        return best;
    }
}
