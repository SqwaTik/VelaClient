#version 330
uniform sampler2D SceneTex;
uniform sampler2D HandTex;
// Multiplier: tint rgb, opacity. GlassParams: brightness, rim strength, refraction (UV units), blur mix.
layout(std140) uniform GlassConfig { vec4 Multiplier; vec4 GlassParams; };
in vec2 texCoord;
out vec4 fragColor;

const vec2 DIRS[8] = vec2[](vec2(1.0, 0.0), vec2(0.7071, 0.7071), vec2(0.0, 1.0), vec2(-0.7071, 0.7071),
                            vec2(-1.0, 0.0), vec2(-0.7071, -0.7071), vec2(0.0, -1.0), vec2(0.7071, -0.7071));
const vec3 LIGHT = vec3(-0.45, 0.62, 0.64);

float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

void main() {
    vec4 hand = texture(HandTex, texCoord);
    float coverage = hand.a * Multiplier.a;
    if (coverage <= 0.001) discard;
    vec2 texel = 1.0 / vec2(textureSize(HandTex, 0));

    // Liquid glass is a thick lens: a smooth height field from the silhouette (how far inside the hand a pixel is),
    // rising over a wide band from the edge. Its slope bends the background, strongest at the rim, flat inside.
    float band = 0.022; // ~2% of the screen height, resolution independent
    vec2 r1 = vec2(band * 0.45 * texel.x / texel.y, band * 0.45), r2 = r1 * 2.2;
    vec2 slope = vec2(0.0);
    float inside = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = texture(HandTex, texCoord + DIRS[i] * r1).a;
        float b = texture(HandTex, texCoord + DIRS[i] * r2).a;
        slope += DIRS[i] * (a * 0.6 + b * 0.4);
        inside += a + b;
    }
    inside /= 16.0;                            // 1 deep inside, ~0.5 at the silhouette
    float height = smoothstep(0.45, 1.0, inside);
    // The hand's own shading carries its 3D form (cube faces, item pixels): a little of it shapes the surface too.
    vec2 d = texel * 1.5;
    vec2 detail = vec2(luma(texture(HandTex, texCoord + vec2(d.x, 0)).rgb) - luma(texture(HandTex, texCoord - vec2(d.x, 0)).rgb),
                       luma(texture(HandTex, texCoord + vec2(0, d.y)).rgb) - luma(texture(HandTex, texCoord - vec2(0, d.y)).rgb));
    vec3 normal = normalize(vec3(-slope * 0.55 * (1.0 - height * 0.85) - detail * 0.25, 1.0));

    // Refraction: the background seen through the lens is pulled inwards at the edge and slightly magnified inside.
    float strength = GlassParams.z * 4.5;
    vec2 offset = -normal.xy * strength + (vec2(0.5) - texCoord) * 0.012 * height;
    // Dispersion: each channel bends by a different amount, visible as thin colour fringes at the rim only.
    float spread = 0.08 * (1.0 - height);
    vec3 scene;
    scene.r = texture(SceneTex, texCoord + offset * (1.0 + spread)).r;
    scene.g = texture(SceneTex, texCoord + offset).g;
    scene.b = texture(SceneTex, texCoord + offset * (1.0 - spread)).b;
    // Soft frost, stronger in the thick middle.
    vec2 b = texel * (1.5 + 2.5 * height);
    vec3 blur = (texture(SceneTex, texCoord + offset + vec2(b.x, b.y)).rgb + texture(SceneTex, texCoord + offset - vec2(b.x, b.y)).rgb
               + texture(SceneTex, texCoord + offset + vec2(b.x, -b.y)).rgb + texture(SceneTex, texCoord + offset - vec2(b.x, -b.y)).rgb) * 0.25;
    scene = mix(scene, blur, GlassParams.w * (0.4 + 0.6 * height));

    vec3 tint = mix(vec3(1.0), Multiplier.rgb, 0.3);
    vec3 body = scene * tint * mix(1.0, GlassParams.x, height);
    // A faint ghost of the hand's shading so its shape still reads through the glass.
    body *= mix(1.0, 0.75 + 0.5 * luma(hand.rgb), 0.35);

    // Fresnel: grazing edges reflect more, so the rim brightens; a specular glint where the surface faces the light.
    float facing = clamp(normal.z, 0.0, 1.0);
    float fresnel = pow(1.0 - facing, 3.0);
    vec3 reflected = reflect(-normalize(LIGHT), normal);
    float specular = pow(clamp(reflected.z, 0.0, 1.0), 48.0) * 0.9;
    // Thin bright line on the lit side of the rim, a dimmer one opposite (as on real glass edges).
    float rimLit = clamp(dot(normalize(slope + 1e-5), -LIGHT.xy), -1.0, 1.0);
    float rim = (1.0 - height) * smoothstep(0.5, 0.7, inside) * (0.55 + 0.45 * rimLit);
    vec3 glow = mix(vec3(1.0), Multiplier.rgb, 0.2);
    vec3 glass = body + glow * (fresnel * 0.35 + specular + rim * GlassParams.y * 1.6);

    vec3 original = texture(SceneTex, texCoord).rgb;
    fragColor = vec4(mix(original, glass, coverage), 1.0);
}
