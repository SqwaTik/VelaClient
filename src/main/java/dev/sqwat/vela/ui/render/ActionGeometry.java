package dev.sqwat.vela.ui.render;

/** A hover action keeps its leading edge aligned with its input. */
public final class ActionGeometry {
    private ActionGeometry(){}
    public static float growth(float progress){return 8*Math.max(0,Math.min(1,progress));}
    public static Rect hit(Rect base,float progress){return new Rect(base.x(),base.y(),base.width()+(int)Math.ceil(growth(progress)),base.height());}
    public static float textShift(float progress){return growth(progress)*.75f;}
}
