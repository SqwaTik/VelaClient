#version 330

uniform sampler2D Scene;
uniform sampler2D DepthSampler;

layout(std140) uniform ReflectParams {
    vec4 camPosTime;
    vec4 skyTintReflectivity;
    vec4 sunDirWetness;
    vec4 surface;
    vec4 trace;
    mat4 invViewProj;
    mat4 viewProj;
};

in vec2 texCoord;
out vec4 fragColor;

const int MAX_STEPS = 40;
const int REFINE_STEPS = 3;
const float STEP_GROWTH = 0.12;
const float SURFACE_BIAS = 0.035;
const float EDGE_FADE = 0.12;
const float SILHOUETTE_LIMIT = 0.55;
// A reflected object has to stand above the wet plane; anything closer to it is the floor seeing itself
// (depth precision at grazing angles), which drew bright stripes across distant ground.
const float PLANE_CLEARANCE = 0.12;
const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

float ndcZ(float depth) {
    return mix(depth * 2.0 - 1.0, depth, trace.w);
}

vec3 worldFromDepth(vec2 uv, float depth) {
    vec4 pos = invViewProj * vec4(uv * 2.0 - 1.0, ndcZ(depth), 1.0);
    return pos.xyz / pos.w;
}

// View depth (clip w) of the scene at uv: one dot product with the inverse's last row instead of a full
// unprojection and a length. Directly comparable with the clip w of a point on the ray.
float sceneW(vec2 uv, float depth) {
    vec4 row = vec4(invViewProj[0][3], invViewProj[1][3], invViewProj[2][3], invViewProj[3][3]);
    return 1.0 / dot(row, vec4(uv * 2.0 - 1.0, ndcZ(depth), 1.0));
}

vec3 faceNormal(vec2 uv, float depth, vec3 view, out float discontinuity) {
    vec2 texel = 1.0 / vec2(textureSize(DepthSampler, 0));

    vec2 uvL = uv - vec2(texel.x, 0.0);
    vec2 uvR = uv + vec2(texel.x, 0.0);
    vec2 uvD = uv - vec2(0.0, texel.y);
    vec2 uvU = uv + vec2(0.0, texel.y);

    float dL = texture(DepthSampler, uvL).r;
    float dR = texture(DepthSampler, uvR).r;
    float dD = texture(DepthSampler, uvD).r;
    float dU = texture(DepthSampler, uvU).r;

    bool leftCloser = abs(dL - depth) < abs(dR - depth);
    bool downCloser = abs(dD - depth) < abs(dU - depth);

    vec3 ddx = leftCloser ? view - worldFromDepth(uvL, dL) : worldFromDepth(uvR, dR) - view;
    vec3 ddy = downCloser ? view - worldFromDepth(uvD, dD) : worldFromDepth(uvU, dU) - view;

    discontinuity = max(length(ddx), length(ddy)) / max(length(view), 1.0);

    vec3 n = cross(ddx, ddy);
    float len = length(n);
    if (len < 1e-8) return normalize(-view);

    n /= len;
    n = dot(n, -view) < 0.0 ? -n : n;
    // Block faces are axis-aligned: snapping removes the striped normals that depth precision causes far away.
    vec3 a = abs(n);
    if (a.y >= a.x && a.y >= a.z) return vec3(0.0, sign(n.y), 0.0);
    return a.x >= a.z ? vec3(sign(n.x), 0.0, 0.0) : vec3(0.0, 0.0, sign(n.z));
}

// Per-pixel hash for the ray start. Interleaved gradient noise is built for temporal accumulation; this pass has
// none, so its diagonal structure stayed on screen as fine stripes in the reflections.
float startJitter(vec2 pixel) {
    vec3 p = fract(vec3(pixel.xyx) * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float edgeFade(vec2 uv) {
    vec2 fade = smoothstep(vec2(0.0), vec2(EDGE_FADE), uv)
              * smoothstep(vec2(0.0), vec2(EDGE_FADE), 1.0 - uv);
    return fade.x * fade.y;
}

// GGX distribution with the cheap Kelemen visibility term: a soft, energy-bounded highlight. The old Phong
// lobe (power 190) on perfectly flat snapped normals was a hard disc that crawled and blew out to white.
float glint(vec3 normal, vec3 toLight, vec3 toEye, float roughness) {
    vec3 h = normalize(toLight + toEye);
    float nh = max(dot(normal, h), 0.0);
    float vh = max(dot(toEye, h), 0.05);
    float nl = max(dot(normal, toLight), 0.0);
    float a2 = roughness * roughness * roughness * roughness;
    float d = nh * nh * (a2 - 1.0) + 1.0;
    float ggx = a2 / (3.14159265 * d * d);
    float fresnel = 0.02 + 0.98 * exp2(-9.28 * vh); // Schlick, exp2 form
    return min(ggx * fresnel * nl / (4.0 * vh * vh), 16.0);
}

// Wet ground is darker everywhere it is wet: side faces and block silhouettes too, otherwise the thin unsoaked
// edges of paths and slabs stood out as light stripes between the darker tops. Fades out with distance, so
// there is no visible line where the pass stops.
vec4 soakOnly(vec2 uv, float wetness, float mask, float viewDistance, float depth, float fade) {
    vec3 src = texture(Scene, uv).rgb;
    float soak = 0.32 * wetness * mix(0.7, 1.0, mask) * (1.0 - smoothstep(64.0, 96.0, viewDistance));
    return vec4(-src * soak * fade, depth);
}

void main() {
    // Neutral sky: wetness must not tint the ground (sunset or biome colours turned puddles red).
    vec3 SkyTint = vec3(dot(skyTintReflectivity.xyz, LUMA));
    float Reflectivity = skyTintReflectivity.w;
    vec3 SunDir = sunDirWetness.xyz;
    float Wetness = sunDirWetness.w;
    float Gloss = surface.y;
    float Steps = surface.z;
    float HitThickness = surface.w;
    float MaxDistance = trace.x;
    float UpOnly = trace.y;
    float Fade = trace.z;

    // This pass runs at a fraction of the screen, so its pixel centres land exactly on borders between depth
    // texels. Nearest sampling there picks one side or the other depending on rounding, which flipped every few
    // rows and gave those rows a wrong normal: even horizontal stripes across the floor, like a cracked screen.
    // Snapping to one depth texel centre makes the depth, its neighbours and the scene colour agree.
    vec2 depthSize = vec2(textureSize(DepthSampler, 0));
    vec2 uv0 = (floor(texCoord * depthSize) + 0.5) / depthSize;
    float depth = texelFetch(DepthSampler, ivec2(uv0 * depthSize), 0).r;
    if (depth <= 0.0) { fragColor = vec4(0.0); return; }

    vec3 view = worldFromDepth(uv0, depth);
    float viewDistance = length(view);
    // Past this nothing changes (both fades are zero): skip the normal and the trace entirely.
    if (viewDistance >= 96.0) { fragColor = vec4(0.0, 0.0, 0.0, depth); return; }
    // Night: the sky is dark, and so is what a puddle shows of it. A full-brightness sky tint (custom
    // Ambience sky colours are bright at any hour) turned the ground milky grey at grazing angles.
    SkyTint *= mix(0.12, 1.0, smoothstep(-0.1, 0.25, SunDir.y));

    float discontinuity;
    vec3 normal = faceNormal(uv0, depth, view, discontinuity);
    float mask = mix(1.0, smoothstep(0.5, 0.82, normal.y), UpOnly);
    if (discontinuity > SILHOUETTE_LIMIT || mask <= 0.002) {
        fragColor = soakOnly(uv0, Wetness, discontinuity > SILHOUETTE_LIMIT ? 0.0 : mask, viewDistance, depth, Fade);
        return;
    }

    vec4 src = texture(Scene, uv0);
    vec3 incident = view / viewDistance;
    vec3 toEye = -incident;
    float distanceFade = 1.0 - smoothstep(48.0, 96.0, viewDistance);

    // Wet material is only darker; no saturation boost, so it never shifts colour.
    vec3 soaked = src.rgb * (1.0 - 0.32 * Wetness * mix(0.7, 1.0, mask) * (1.0 - smoothstep(64.0, 96.0, viewDistance)));

    float fresnel = 0.02 + 0.98 * pow(1.0 - clamp(dot(normal, toEye), 0.0, 1.0), 5.0);
    // Capped below a mirror: at grazing angles fresnel goes to 1 and the whole floor became the sky colour.
    float amount = min(Reflectivity * (0.08 + 0.62 * fresnel), 0.7) * mask * distanceFade;

    vec3 reflected = reflect(incident, normal);
    vec3 origin = view + normal * SURFACE_BIAS;
    vec2 hitUv = vec2(0.0);
    float hit = 0.0;

    // Invisible reflections (no reflectivity, far away, faces looking straight at the camera with a low
    // setting) need no ray: this skips most of the screen's trace cost.
    if (amount > 0.01) {
        int steps = clamp(int(Steps), 8, MAX_STEPS);
        float stepLength = MaxDistance / float(steps);
        float travelled = stepLength * (0.35 + startJitter(gl_FragCoord.xy) * 0.65);
        float previous = travelled;

        for (int i = 0; i < MAX_STEPS; i++) {
            if (i >= steps || travelled >= MaxDistance) break;

            vec3 marchPos = origin + reflected * travelled;
            vec4 clip = viewProj * vec4(marchPos, 1.0);
            if (clip.w <= 0.001) break;

            vec2 uv = (clip.xy / clip.w) * 0.5 + 0.5;
            if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) break;

            float sceneDepth = texture(DepthSampler, uv).r;
            if (sceneDepth > 0.0) {
                float behind = clip.w - sceneW(uv, sceneDepth);
                if (behind > 0.0 && behind < HitThickness + travelled * 0.08
                        && dot(worldFromDepth(uv, sceneDepth) - view, normal) > PLANE_CLEARANCE) {
                    float lo = previous;
                    float hi = travelled;
                    for (int r = 0; r < REFINE_STEPS; r++) {
                        float mid = (lo + hi) * 0.5;
                        vec4 probeClip = viewProj * vec4(origin + reflected * mid, 1.0);
                        vec2 probeUv = clamp((probeClip.xy / max(probeClip.w, 1e-4)) * 0.5 + 0.5, 0.0, 1.0);
                        float probeDepth = texture(DepthSampler, probeUv).r;
                        if (probeDepth > 0.0 && probeClip.w > sceneW(probeUv, probeDepth)) { hi = mid; uv = probeUv; }
                        else { lo = mid; }
                    }
                    hitUv = uv;
                    hit = edgeFade(uv) * (1.0 - smoothstep(MaxDistance * 0.6, MaxDistance, hi));
                    break;
                }
            }

            previous = travelled;
            travelled += stepLength * (1.0 + float(i) * STEP_GROWTH);
        }
    }

    vec3 reflection = SkyTint;
    if (hit > 0.0) {
        vec3 hitColor = texture(Scene, hitUv).rgb;
        reflection = mix(SkyTint, mix(vec3(dot(hitColor, LUMA)), hitColor, 0.55), hit);
    }

    // Highlight from the sun by day and the (dimmer, cooler) moon by night, fading in above the horizon
    // instead of popping. Shaded and dark surfaces (caves, under roofs, night ground) barely glint: the scene's
    // own brightness stands in for the missing shadow test. Same distance fade as the reflection, so a flat
    // world no longer gets a bright smear stretching to the horizon.
    float day = step(0.0, SunDir.y);
    vec3 toLight = SunDir * (day * 2.0 - 1.0);
    float elevation = smoothstep(0.0, 0.15, toLight.y);
    vec3 lightColor = mix(vec3(0.55, 0.65, 0.85) * 0.35, vec3(1.0, 0.95, 0.86), day);
    float lit = smoothstep(0.03, 0.30, dot(src.rgb, LUMA));
    float roughness = mix(0.30, 0.14, Wetness);
    vec3 specular = lightColor * (glint(normal, toLight, toEye, roughness) * 0.07
        * Gloss * mask * elevation * lit * distanceFade);

    vec3 shaded = mix(soaked, reflection, amount) + specular;

    // Store only lighting change, preserving full-resolution scene detail during resolve.
    fragColor = vec4((clamp(shaded, 0.0, 1.0) - src.rgb) * Fade, depth);
}
