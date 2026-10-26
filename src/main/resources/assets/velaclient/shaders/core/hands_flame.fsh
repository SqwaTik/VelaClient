#version 330
uniform sampler2D AfterSampler;
uniform sampler2D TrailSampler;
layout(std140) uniform HandsFlameData {
 vec4 flameColor; vec4 params0; vec4 params1; vec4 screen;
 vec4 gradColor1; vec4 gradColor2; vec4 gradColor3; vec4 gradColor4; vec4 flameExtra;
};
in vec2 texCoord;
out vec4 fragColor;
void main() {
 vec4 hand = texture(AfterSampler, texCoord);
 vec4 flame = texture(TrailSampler, texCoord);
 float cover = clamp(hand.a, 0.0, 1.0);
 float fire = smoothstep(0.025, 0.65, flame.a) * (1.0 - cover * 0.90);
 if (cover <= 0.001 && fire <= 0.001) discard;
 vec3 color = hand.rgb * (1.0 - fire * 0.45) + flame.rgb * fire * 1.3;
 float opacity = 1.0 - (1.0 - cover) * (1.0 - fire * 0.45);
 fragColor = vec4(color, opacity);
}
