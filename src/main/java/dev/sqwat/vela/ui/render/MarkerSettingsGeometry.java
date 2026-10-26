package dev.sqwat.vela.ui.render;

public final class MarkerSettingsGeometry {
    private MarkerSettingsGeometry(){}
    public record Groups(Rect death,Rect limit,Rect quick){public int bottom(){return quick.y()+quick.height();}}
    public static Groups place(Rect area,float death,float limit,float quick){
        int x=area.x()+3,y=area.y()+3,w=area.width()-6;
        Rect d=new Rect(x,y,w,35+Math.round(25*Math.clamp(death,0,1)));
        Rect l=new Rect(x,d.y()+d.height()+5,w,35+Math.round(30*Math.clamp(limit,0,1)));
        Rect q=new Rect(x,l.y()+l.height()+5,w,35+Math.round(28*Math.clamp(quick,0,1)));
        return new Groups(d,l,q);
    }
}
