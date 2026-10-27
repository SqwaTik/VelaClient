#version 330
uniform sampler2D SceneTex;
uniform sampler2D HandTex;
// Multiplier: tint rgb, opacity. GlassParams: brightness, rim strength, refraction (UV units), blur mix.
layout(std140) uniform GlassConfig { vec4 Multiplier; vec4 GlassParams; };
in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }
float coverageAt(vec2 uv) { return texture(HandTex, uv).a; }

void main() {
    vec4 hand = texture(HandTex, texCoord);
    float coverage = hand.a * Multiplier.a;
    if (coverage <= 0.001) discard;
    vec2 texel = 1.0 / vec2(textureSize(HandTex, 0));

    // Bevel normal from the silhouette: alpha falls off towards the edge, so its gradient points outwards.
    vec2 r = texel * 4.0;
    vec2 bevel = vec2(coverageAt(texCoord - vec2(r.x, 0)) - coverageAt(texCoord + vec2(r.x, 0)),
                      coverageAt(texCoord - vec2(0, r.y)) - coverageAt(texCoord + vec2(0, r.y)));
    // Surface detail of the item (pixel art edges) bends light a little too.
    vec2 d = texel * 1.5;
    vec2 detail = vec2(luma(texture(HandTex, texCoord + vec2(d.x, 0)).rgb) - luma(texture(HandTex, texCoord - vec2(d.x, 0)).rgb),
                       luma(texture(HandTex, texCoord + vec2(0, d.y)).rgb) - luma(texture(HandTex, texCoord - vec2(0, d.y)).rgb));
    vec2 offset = (bevel * 0.9 + detail * 0.35) * GlassParams.z * 3.0;

    // Refracted scene with a slight chromatic split, softly blurred.
    vec2 b = texel * 2.0;
    vec3 scene;
    scene.r = texture(SceneTex, texCoord + offset * 1.06).r;
    scene.g = texture(SceneTex, texCoord + offset).g;
    scene.b = texture(SceneTex, texCoord + offset * 0.94).b;
    vec3 blur = (texture(SceneTex, texCoord + offset + vec2(b.x, b.y)).rgb + texture(SceneTex, texCoord + offset - vec2(b.x, b.y)).rgb
               + texture(SceneTex, texCoord + offset + vec2(b.x, -b.y)).rgb + texture(SceneTex, texCoord + offset - vec2(b.x, -b.y)).rgb) * 0.25;
    scene = mix(scene, blur, GlassParams.w);

    vec3 tint = mix(vec3(1.0), Multiplier.rgb, 0.35);
    vec3 body = scene * tint * GlassParams.x;
    // Keep a ghost of the item's shading so its shape reads through the glass.
    body += (luma(hand.rgb) - 0.45) * 0.18;

    // Lit rim where the bevel is steep, and a diagonal sheen across the item.
    float rim = clamp(length(bevel) * 1.4, 0.0, 1.0);
    float diagonal = fract((gl_FragCoord.x + gl_FragCoord.y) * texel.x * 0.35);
    float sheen = smoothstep(0.0, 0.08, diagonal) * (1.0 - smoothstep(0.08, 0.2, diagonal)) * 0.18;
    vec3 highlight = mix(vec3(1.0), Multiplier.rgb, 0.25) * (rim * GlassParams.y * 2.2 + sheen);

    vec3 original = texture(SceneTex, texCoord).rgb;
    fragColor = vec4(mix(original, body + highlight, coverage), 1.0);
}
