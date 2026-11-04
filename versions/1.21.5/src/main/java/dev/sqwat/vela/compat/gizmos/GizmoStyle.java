package dev.sqwat.vela.compat.gizmos;

/** 1.21.11's gizmo style: a stroke colour and width, and a fill colour (0 = none). */
public record GizmoStyle(int stroke,float strokeWidth,int fill) {
    public static GizmoStyle stroke(int color){return new GizmoStyle(color,2.5f,0);}
    public static GizmoStyle stroke(int color,float width){return new GizmoStyle(color,width,0);}
    public static GizmoStyle fill(int color){return new GizmoStyle(0,0,color);}
    public static GizmoStyle strokeAndFill(int stroke,float width,int fill){return new GizmoStyle(stroke,width,fill);}
    public boolean hasFill(){return fill!=0;}
    public boolean hasStroke(){return stroke!=0&&strokeWidth>0;}
}
