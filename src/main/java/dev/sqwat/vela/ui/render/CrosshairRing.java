package dev.sqwat.vela.ui.render;

/** Fixed-radius ring with a swing-driven sweep and feathered edges. No per-vertex objects. */
public final class CrosshairRing {
    private static final float[] COS=new float[65],SIN=new float[65];
    static{
        for(int i=0;i<=64;i++){
            double angle=(float)i/64*Math.PI*2;
            COS[i]=(float)Math.cos(angle);SIN[i]=(float)Math.sin(angle);
        }
    }
    private CrosshairRing(){}
    @FunctionalInterface public interface Vertex {void emit(float x,float y,int color);}
    public static void emit(Vertex sink,float x,float y,float radius,float width,float sweep,int color,float density){
        ring(sink,x,y,radius,width,1,0xC0080A10,density,false);
        if(sweep>0)ring(sink,x,y,radius,width,Math.clamp(sweep,0,1),color,density,true);
    }
    private static void ring(Vertex sink,float x,float y,float radius,float width,float sweep,int color,float density,boolean gradient){
        float inner=Math.max(.1f,radius-width/2),outer=radius+width/2,feather=Math.min(inner,Math.min(width/2,1/Math.max(1,density)));
        int segments=Math.max(1,(int)Math.ceil(64*sweep));
        float c0=1,s0=0;
        int col0=color;
        for(int i=0;i<segments;i++){
            float t1=(float)(i+1)/segments;
            double a1=t1*sweep*Math.PI*2;
            float c1=sweep==1?COS[i+1]:(float)Math.cos(a1),s1=sweep==1?SIN[i+1]:(float)Math.sin(a1);
            int col1=gradient?VelaTheme.mix(color,0xFFFFFFFF,.3f*(1-Math.abs(1-2*t1))):color;
            strip(sink,x,y,c0,s0,c1,s1,inner-feather,inner,col0&0xFFFFFF,col1&0xFFFFFF,col0,col1);
            strip(sink,x,y,c0,s0,c1,s1,inner,outer,col0,col1,col0,col1);
            strip(sink,x,y,c0,s0,c1,s1,outer,outer+feather,col0,col1,col0&0xFFFFFF,col1&0xFFFFFF);
            c0=c1;s0=s1;col0=col1;
        }
    }
    private static void strip(Vertex v,float x,float y,float c0,float s0,float c1,float s1,float r0,float r1,int in0,int in1,int out0,int out1){
        v.emit(x+c0*r0,y+s0*r0,in0);v.emit(x+c1*r0,y+s1*r0,in1);
        v.emit(x+c1*r1,y+s1*r1,out1);v.emit(x+c0*r1,y+s0*r1,out0);
    }
}
