#version 330
uniform sampler2D AfterSampler;
uniform sampler2D PrevTrailSampler;
layout(std140) uniform HandsFlameData {
 vec4 flameColor; vec4 params0; vec4 params1; vec4 screen;
 vec4 gradColor1; vec4 gradColor2; vec4 gradColor3; vec4 gradColor4; vec4 flameExtra;
};
in vec2 texCoord;
out vec4 fragColor;
void main() {
 float dt = clamp(flameColor.a, 0.0005, 0.05);
 float time = params1.y;
 vec2 px = screen.zw;
 // Sample below the output pixel: emission extends upward from the hand.
 float wave = sin(texCoord.y * 35.0 - time * 5.0) * sin(texCoord.x * 47.0 + time * 3.0);
 vec2 drift = vec2(wave * 20.0, -85.0) * px * dt;
 vec4 previous = texture(PrevTrailSampler, clamp(texCoord + drift, 0.0, 1.0));
 float seed = texture(AfterSampler, texCoord).a;
 seed = max(seed, texture(AfterSampler, clamp(texCoord - vec2(0, px.y * 12.0), 0.0, 1.0)).a * 0.8);
 seed = max(seed, texture(AfterSampler, clamp(texCoord - vec2(px.x * 7.0, px.y * 22.0), 0.0, 1.0)).a * 0.55);
 seed = max(seed, texture(AfterSampler, clamp(texCoord - vec2(-px.x * 7.0, px.y * 22.0), 0.0, 1.0)).a * 0.55);
 seed *= 0.65 + 0.25 * wave;
 float alpha = max(seed, previous.a * exp(-4.0 * dt));
 vec3 tint = mix(flameColor.rgb * 0.65, mix(flameColor.rgb, vec3(1.0), 0.6), smoothstep(0.15, 0.85, alpha));
 fragColor = vec4(tint, alpha);
}
