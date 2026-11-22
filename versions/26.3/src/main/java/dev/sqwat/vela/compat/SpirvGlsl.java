package dev.sqwat.vela.compat;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 26.3 compiles every shader to SPIR-V: each stage input and output needs an explicit location and the vertex index is
 * {@code gl_VertexIndex}. Vela's shaders are written for plain GLSL 330, so they are rewritten just before compiling.
 */
public final class SpirvGlsl {
    // Varyings get one location per name, so any vertex shader pairs with any fragment shader that reads the same names.
    // texCoord is 0 like vanilla screenquad.vsh, which some Vela fragment shaders are paired with.
    private static final Map<String, Integer> VARYINGS = Map.of(
        "texCoord", 0, "sceneUv", 1, "localUv", 2, "vertexColor", 3, "skinUv", 4, "parameters", 5, "waveUv", 6);
    private static final int FIRST_UNKNOWN_VARYING = 8;
    private static final Pattern VERSION = Pattern.compile("^#version[^\\n]*\\n", Pattern.MULTILINE);
    private static final Pattern DECLARATION = Pattern.compile(
        "^(\\s*)((?:flat|smooth|noperspective)\\s+)?(in|out)(\\s+\\w+\\s+)(\\w+)(\\s*(?:\\[[^\\]]*\\])?\\s*;)", Pattern.MULTILINE);

    private SpirvGlsl() {}

    /** Only Vela's own shaders; vanilla and other mods already target SPIR-V. */
    public static boolean ours(String name) {
        return name != null && (name.startsWith("velaclient:") || name.startsWith("velaopt:"));
    }

    public static String port(String source, boolean vertex) {
        if (source.contains("GL_ARB_separate_shader_objects")) return source;
        String text = source.replaceAll("\\bgl_VertexID\\b", "gl_VertexIndex").replaceAll("\\bgl_InstanceID\\b", "gl_InstanceIndex");
        Matcher m = DECLARATION.matcher(text);
        var out = new StringBuilder();
        int attributes = 0, fragmentOutputs = 0, unknown = FIRST_UNKNOWN_VARYING;
        while (m.find()) {
            boolean input = m.group(3).equals("in");
            int location;
            if (vertex && input) location = attributes++;
            else if (!vertex && !input) location = fragmentOutputs++;
            else location = VARYINGS.getOrDefault(m.group(5), -1);
            if (location < 0) location = unknown++;
            String qualifier = m.group(2) == null ? "" : m.group(2);
            m.appendReplacement(out, Matcher.quoteReplacement(m.group(1) + "layout(location = " + location + ") " + qualifier
                + m.group(3) + m.group(4) + m.group(5) + m.group(6)));
        }
        m.appendTail(out);
        text = out.toString();
        Matcher version = VERSION.matcher(text);
        if (version.find())
            return text.substring(0, version.end()) + "#extension GL_ARB_separate_shader_objects : require\n" + text.substring(version.end());
        return "#version 330\n#extension GL_ARB_separate_shader_objects : require\n" + text;
    }
}
