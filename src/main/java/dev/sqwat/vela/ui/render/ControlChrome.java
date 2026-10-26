package dev.sqwat.vela.ui.render;

/** Stateless control painters. Animation state and input are owned separately. */
public final class ControlChrome {
    private ControlChrome(){}
    public static float disclosureAngle(float progress){return (float)Math.PI*Math.clamp(progress,0,1);}
    public static void checkbox(UiCanvas c,Rect r,float progress){
        float p=Math.clamp(progress,0,1);c.round(r.x(),r.y(),r.width(),r.height(),4,VelaTheme.mix(VelaTheme.BORDER,VelaTheme.PRIMARY,p));
        if(p>.001f){float size=(r.width()-4)*(.7f+.3f*p);c.image("check",r.x()+(r.width()-size)/2,r.y()+(r.height()-size)/2,size,(Math.round(p*255)<<24)|0xFFFFFF);}
    }
    public static void slider(UiCanvas c,Rect r,float value){
        float p=Math.clamp(value,0,1);c.round(r.x(),r.y(),r.width(),r.height(),3,VelaTheme.BORDER);c.round(r.x(),r.y(),Math.max(1,r.width()*p),r.height(),3,VelaTheme.PRIMARY);c.round(r.x()+(r.width()-3)*p,r.y()-2,3,r.height()+4,2,VelaTheme.TEXT);
    }
    public static Rect sliderHit(Rect track){return new Rect(track.x()-2,track.y()-5,track.width()+4,track.height()+10);}
    public static Rect verticalReveal(Rect thumb,float progress){
        int height=Math.round(thumb.height()*Math.clamp(progress,0,1));
        return new Rect(thumb.x(),thumb.y()+(thumb.height()-height)/2,thumb.width(),height);
    }
}
