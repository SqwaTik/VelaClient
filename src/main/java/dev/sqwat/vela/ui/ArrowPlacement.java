package dev.sqwat.vela.ui;

/** Bounded radial lanes reserve the arrow and both label lines together. */
public final class ArrowPlacement {
    private final float[] left=new float[24],top=new float[24],right=new float[24],bottom=new float[24];
    private int count;
    public float x,y;
    public void reset(){count=0;}
    public boolean place(float angle,float width,float height,float scale,int viewportWidth,int viewportHeight){
        if(count==24)return false;
        for(int lane=0;lane<10;lane++){
            float radius=(58+lane*30)*scale;
            float px=viewportWidth/2f+(float)Math.cos(angle)*radius;
            float py=viewportHeight/2f+(float)Math.sin(angle)*radius;
            float l=px-width/2,t=py-12*scale,r=px+width/2,b=py+height-12*scale;
            if(l<3||t<3||r>viewportWidth-3||b>viewportHeight-3)continue;
            boolean collision=false;
            for(int i=0;i<count;i++)if(l<right[i]+4&&r>left[i]-4&&t<bottom[i]+4&&b>top[i]-4){collision=true;break;}
            if(collision)continue;
            left[count]=l;top[count]=t;right[count]=r;bottom[count++]=b;x=px;y=py;return true;
        }
        return false;
    }
}
