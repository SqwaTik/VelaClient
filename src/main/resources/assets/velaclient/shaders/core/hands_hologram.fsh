#version 330
uniform sampler2D SceneSampler;
// holoColor: rgb, body opacity. params0: time (s), scan strength, flicker, edge glow. screen: size, 1/size.
layout(std140) uniform HandsHologramData {
    vec4 holoColor;
    vec4 params0;
    vec4 params1;
    vec4 screen;
};
in vec2 texCoord;
out vec4 fragColor;

// The ring directions are constants: no cos/sin per tap.
const vec2 DIRS[8] = vec2[](vec2(1.0, 0.0), vec2(0.7071, 0.7071), vec2(0.0, 1.0), vec2(-0.7071, 0.7071),
                            vec2(-1.0, 0.0), vec2(-0.7071, -0.7071), vec2(0.0, -1.0), vec2(0.7071, -0.7071));

float coverageAt(vec2 uv) { return texture(SceneSampler, uv).a; }

void main() {
    float time = params0.x;
    // Thin horizontal glitch bands shift the image now and then.
    float band = step(0.985, fract(sin(floor(gl_FragCoord.y / 6.0) * 12.9898 + floor(time * 9.0) * 78.233) * 43758.5453));
    vec2 uv = texCoord + vec2(band * screen.z * 6.0, 0.0);

    vec4 hand = texture(SceneSampler, uv);
    float mask = clamp(hand.a, 0.0, 1.0);
    // Most of the screen is far from the hand: a 4-tap probe skips the full halo ring there (full-screen pass).
    if (mask <= 0.001) {
        vec2 o = screen.zw * 5.0;
        float probe = coverageAt(uv + vec2(o.x, 0.0)) + coverageAt(uv - vec2(o.x, 0.0))
                    + coverageAt(uv + vec2(0.0, o.y)) + coverageAt(uv - vec2(0.0, o.y));
        if (probe <= 0.001) discard;
    }
    // Glow halo: the silhouette sampled in a ring, wider than the item.
    float near = 0.0, far = 0.0;
    vec2 nearStep = screen.zw * 2.0, farStep = screen.zw * 5.0;
    for (int i = 0; i < 8; i++) {
        near = max(near, coverageAt(uv + DIRS[i] * nearStep));
        far += coverageAt(uv + DIRS[i] * farStep);
    }
    far /= 8.0;
    if (max(mask, max(near, far)) <= 0.001) discard;

    float shade = dot(hand.rgb, vec3(0.299, 0.587, 0.114));
    float scan = 1.0 - params0.y * 0.5 * (0.5 + 0.5 * sin(gl_FragCoord.y * 1.4 - time * 6.0));
    float flicker = 1.0 - params0.z * 0.25 * step(0.92, fract(sin(floor(time * 24.0) * 91.17) * 437.5));
    // Translucent body: the item's shading in the hologram colour, brighter on the inner edge.
    float inner = clamp(near - mask * 0.6, 0.0, 1.0);
    vec3 body = holoColor.rgb * (0.7 + shade * 1.1) * scan * flicker;
    float opacity = mask * 0.62 * holoColor.a;
    vec3 rim = mix(holoColor.rgb, vec3(1.0), 0.3) * max(near - mask, 0.0) * 1.1;
    vec3 halo = holoColor.rgb * far * (1.0 - mask) * params0.w * 1.1;
    vec3 color = body * opacity + rim + halo + holoColor.rgb * inner * mask * 0.25;
    // Premultiplied: the rim and halo add light on top of the scene.
    fragColor = vec4(color, opacity);
}
