#version 330

uniform sampler2D DepthSampler;

layout(std140) uniform FogParams {
    vec4 header;
    vec4 header2;
    vec4 camPos;
    vec4 fogColor;
    mat4 invViewProj;
};

in vec2 texCoord;
out vec4 fragColor;

vec3 worldFromDepth(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, mix(depth * 2.0 - 1.0, depth, header2.w), 1.0);
    vec4 world = invViewProj * clip;
    return world.xyz / world.w;
}

float hash12(vec2 p) {
    vec3 a = fract(p.xyx * vec3(123.34, 234.34, 345.65));
    a += dot(a, a + 34.45);
    return fract(a.x * a.y);
}

float vnoise(vec2 p) {
    vec2 id = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash12(id);
    float b = hash12(id + vec2(1.0, 0.0));
    float c = hash12(id + vec2(0.0, 1.0));
    float d = hash12(id + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

void main() {
    vec2 uv = texCoord;
    float density = header.x;
    float time = header.y;
    float falloff = max(header.z, 0.0006);
    float baseY = header.w;
    float layerStrength = header2.x;
    float skyHaze = header2.y;
    float farDist = max(header2.z, 64.0);

    // Blended as SRC_ALPHA/ONE_MINUS_SRC_ALPHA: alpha is the fog amount, untouched pixels are discarded.
    if (density <= 0.00001) discard;

    float dScene = texture(DepthSampler, uv).r;
    bool sky = dScene <= 0.0;

    vec3 rel = worldFromDepth(uv, sky ? 0.0002 : dScene);
    float relLen = length(rel);
    vec3 rayDir = rel / max(relLen, 1e-4);
    float dist = sky ? farDist * mix(0.5, 2.5, skyHaze) : relLen;

    float camY = camPos.y;
    float deltaY = rayDir.y * dist;
    float h0 = clamp((camY - baseY) * falloff, -6.0, 12.0);
    float f0 = exp(-h0);
    float integ;
    if (abs(deltaY) > 0.01) {
        float he = clamp(deltaY * falloff, -12.0, 12.0);
        integ = f0 * (1.0 - exp(-he)) / he;
    } else {
        integ = f0;
    }
    integ = min(integ, 6.0);

    float layerMod = 1.0;
    float layerTaper = 1.0 - smoothstep(90.0, 220.0, dist);
    if (layerStrength > 0.001 && layerTaper > 0.001) {
        vec2 np = (camPos.xz + rayDir.xz * min(dist, 140.0) * 0.6) * 0.014
                + vec2(time * 0.026, time * 0.017);
        float n = vnoise(np) * 0.65 + vnoise(np * 2.7 + vec2(13.7, 7.1)) * 0.35;
        layerMod = mix(1.0, 0.45 + 1.2 * n, layerStrength * layerTaper);
    }

    float fogAmount = clamp(1.0 - exp(-density * integ * layerMod * dist), 0.0, 1.0);
    if (fogAmount <= 0.002) discard;

    fragColor = vec4(fogColor.rgb, fogAmount);
}
