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
    for (int y=0; y<2; y++) for (int x=0; x<2; x++) {
        vec4 sampleValue = texelFetch(Reflection, clamp(base+ivec2(x,y),ivec2(0),size-1),0);
        vec2 weight = mix(1.0-fraction,fraction,vec2(x,y));
        float relativeDepth = abs(sampleValue.a-depth) / max(depth,0.000001);
        float w = weight.x*weight.y*(1.0-smoothstep(0.01,0.04,relativeDepth));
        delta += sampleValue.rgb*w;
        total += w;
    }
    fragColor = vec4(clamp(scene.rgb+delta/max(total,0.00001),0.0,1.0),scene.a);
}
