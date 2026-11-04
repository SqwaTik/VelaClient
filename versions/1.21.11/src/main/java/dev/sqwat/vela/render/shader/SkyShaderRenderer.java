package dev.sqwat.vela.render.shader;

/** 1.21.11: the shader sky is written against 26.x's render states and GPU API, so vanilla's sky is drawn. */
public final class SkyShaderRenderer {
    private SkyShaderRenderer(){}
    public static void initialize(){}
    public static boolean rendered(){return false;}
    /** Bounds fragment work to 960x540, while retaining the screen's aspect ratio. */
    public static int resolutionDivisor(int width,int height){
        return Math.max(2,Math.max((width-1)/960+1,(height-1)/540+1));
    }
    /** Sky quality: low draws at a quarter of medium's pixels, high at full resolution up to 1080p. */
    public static int resolutionDivisor(int width,int height,String quality){
        int base=Math.max((width-1)/960+1,(height-1)/540+1);
        return switch(quality){case "Низкое"->Math.max(4,base*2);case "Высокое"->Math.max(1,base/2);default->Math.max(2,base);};
    }
    public static void close(){}
}
