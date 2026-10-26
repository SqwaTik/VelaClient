#version 330
uniform sampler2D SceneTex;
uniform sampler2D HandTex;
layout(std140) uniform GlassConfig { vec4 Multiplier; vec4 GlassParams; };
in vec2 texCoord;
out vec4 fragColor;
void main() {
    float coverage = texture(HandTex, texCoord).a * Multiplier.a;
    if (coverage <= 0.001) discard;
    // Frosted transmission only inside the native hand silhouette.
    // Bilinear taps avoid a full-screen blur chain and depth-derived edge artifacts.
    vec2 d = 2.5 / vec2(textureSize(SceneTex, 0));
    vec3 original = texture(SceneTex, texCoord).rgb;
    vec3 blurred = original * 4.0;
    blurred += texture(SceneTex, texCoord + vec2(d.x, 0)).rgb * 2.0;
    blurred += texture(SceneTex, texCoord - vec2(d.x, 0)).rgb * 2.0;
    blurred += texture(SceneTex, texCoord + vec2(0, d.y)).rgb * 2.0;
    blurred += texture(SceneTex, texCoord - vec2(0, d.y)).rgb * 2.0;
    blurred += texture(SceneTex, texCoord + d).rgb;
    blurred += texture(SceneTex, texCoord - d).rgb;
    blurred += texture(SceneTex, texCoord + vec2(d.x, -d.y)).rgb;
    blurred += texture(SceneTex, texCoord + vec2(-d.x, d.y)).rgb;
    vec3 tint = mix(vec3(1.0), Multiplier.rgb, 0.4);
    fragColor = vec4(mix(original, blurred * (tint / 16.0), coverage), 1.0);
}
