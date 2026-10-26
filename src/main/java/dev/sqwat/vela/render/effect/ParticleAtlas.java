package dev.sqwat.vela.render.effect;
public final class ParticleAtlas {
    public static final int COUNT=11,SIZE=128,CELL=132,WIDTH=528,HEIGHT=396;
    private ParticleAtlas(){}
    public static float u(int index){return (index%4*CELL+2f)/WIDTH;}
    public static float v(int index){return (index/4*CELL+2f)/HEIGHT;}
    public static float u1(int index){return u(index)+(float)SIZE/WIDTH;}
    public static float v1(int index){return v(index)+(float)SIZE/HEIGHT;}
}
