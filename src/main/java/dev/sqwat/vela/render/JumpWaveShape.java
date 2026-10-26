package dev.sqwat.vela.render;

/** Time-based envelope shared by the world ring and its refractive mesh. */
public final class JumpWaveShape {
    public static final int MAX_WAVES=4;
    private JumpWaveShape(){}
    public static double radius(double age,double maximum){
        double t=Math.clamp(age,0,1);return maximum*(.8*t+.2*Math.sin(t*Math.PI*.5));
    }
    public static double opacity(double age){
        double t=Math.clamp(age,0,1),appear=Math.min(1,t/.08);
        return appear*appear*(3-2*appear)*(1-t*t*t);
    }
}
