package dev.sqwat.vela.render.shader;

/** 1.21.5: the hand shaders are written against 26.x's GPU API, so the hand is drawn by vanilla. */
public final class HandShaderRenderer {
    private HandShaderRenderer(){}
    public static void initialize(){}
    public static void updateLifecycle(){}
    public static void render(Runnable nativeDraw){nativeDraw.run();}
    public static void close(){}
}
