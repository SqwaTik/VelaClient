package dev.sqwat.vela.render.shader;

/** 1.21.5: no shader sky (see SkyShaderRenderer). The editor still checks the source, and accepts what passes. */
public final class CustomSkyProgram {
    private static String accepted;
    private CustomSkyProgram(){}
    public static boolean accepted(String source){return source.equals(accepted);}
    public static String apply(String source){
        try{CustomSkySource.fragment(source);}catch(IllegalArgumentException error){return error.getMessage();}
        accepted=source;return null;
    }
}
