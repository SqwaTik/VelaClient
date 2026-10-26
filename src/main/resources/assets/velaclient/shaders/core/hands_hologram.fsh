#version 330
uniform sampler2D SceneSampler;
layout(std140) uniform HandsHologramData {
    vec4 holoColor;
    vec4 params0;
    vec4 params1;
    vec4 screen;
};
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 hand = texture(SceneSampler, texCoord);
    vec2 px = screen.zw * 2.0;
    float mask = clamp(hand.a, 0.0, 1.0);
    float nearMask = max(max(texture(SceneSampler, texCoord + vec2(px.x, 0)).a,
                            texture(SceneSampler, texCoord - vec2(px.x, 0)).a),
                         max(texture(SceneSampler, texCoord + vec2(0, px.y)).a,
                             texture(SceneSampler, texCoord - vec2(0, px.y)).a));
    if (max(mask, nearMask) <= 0.001) discard;
    float scan = 0.75 + 0.25 * sin(gl_FragCoord.y * 0.8 - params0.x * 4.0);
    float shade = dot(hand.rgb, vec3(0.299, 0.587, 0.114));
    vec3 body = holoColor.rgb * (0.35 + shade) * scan;
    float edge = max(nearMask - mask, 0.0);
    float opacity = mask * holoColor.a;
    vec3 color = body * opacity + mix(holoColor.rgb, vec3(1.0), 0.4) * edge * 0.65;
    fragColor = vec4(color, opacity);
}
