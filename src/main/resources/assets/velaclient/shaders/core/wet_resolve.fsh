#version 330
uniform sampler2D Scene;
uniform sampler2D DepthSampler;
uniform sampler2D Reflection;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(Scene, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    if (depth <= 0.0) { fragColor = scene; return; }
    ivec2 size = textureSize(Reflection, 0);
    vec2 pixel = texCoord * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(pixel));
    vec2 fraction = fract(pixel);
    vec3 delta = vec3(0.0);
    float total = 0.0;
    // Closest-depth sample: the fallback when every neighbour is rejected. On distant ground at a grazing angle
    // neighbouring reflection texels differ by more than the tolerance, so all four were dropped and whole rows
    // came out unshaded - the horizontal stripes across a wet floor.
    vec3 closest = vec3(0.0);
    float closestDepth = 1e9;
    for (int y=0; y<2; y++) for (int x=0; x<2; x++) {
        vec4 sampleValue = texelFetch(Reflection, clamp(base+ivec2(x,y),ivec2(0),size-1),0);
        vec2 weight = mix(1.0-fraction,fraction,vec2(x,y));
        float relativeDepth = abs(sampleValue.a-depth) / max(depth,0.000001);
        if (relativeDepth < closestDepth) { closestDepth = relativeDepth; closest = sampleValue.rgb; }
        float w = weight.x*weight.y*(1.0-smoothstep(0.03,0.12,relativeDepth));
        delta += sampleValue.rgb*w;
        total += w;
    }
    vec3 lighting = total > 0.001 ? delta/total : closest;
    fragColor = vec4(clamp(scene.rgb+lighting,0.0,1.0),scene.a);
}
