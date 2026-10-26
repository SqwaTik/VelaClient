#version 330



layout(std140) uniform SkyParams {
    mat4 invViewProj;
    vec4 misc;
    vec4 skyColor;
    vec4 skyColor2;
    vec4 taa;
    mat4 prevViewProj;
    vec4 skyExtra;
};

in vec2 texCoord;
out vec4 fragColor;

// One candidate meteor per angular sector; no per-pixel scan of all meteors.
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 farPoint = invViewProj * vec4(ndc, mix(-0.9996, 0.0002, skyExtra.y), 1.0);
    vec4 nearPoint = invViewProj * vec4(ndc, 1.0, 1.0);
    vec3 ray = normalize(farPoint.xyz / farPoint.w - nearPoint.xyz / nearPoint.w);
    float azimuth = atan(ray.z, ray.x) / 6.2831853 + 0.5;
    vec2 uv = vec2(azimuth, ray.y);
    vec3 color = vec3(0.008, 0.013, 0.032);
    // Stable directional stars, softened at the actual raster footprint.
    vec2 grid = uv * vec2(256.0, 128.0);
    vec2 cell = floor(grid);
    vec2 p = fract(grid) - 0.5;
    float random = hash(cell);
    float size = max(0.065, length(fwidth(grid)) * 0.35);
    float star = 1.0 - smoothstep(0.0, size, length(p));
    color += vec3(star * step(0.975, random) * (0.75 + 0.25 * sin(misc.x + random * 60.0)));
    float sector = floor(azimuth * 8.0);
    float seed = hash(vec2(sector, 7.0));
    float cycle = misc.x / (3.0 + seed * 3.0) + seed * 11.0;
    float phase = fract(cycle);
    float meteorWidth = max(0.003, length(fwidth(vec2(azimuth * 8.0, ray.y))) * 0.5);
    if (phase < 0.3 && ray.y > 0.03 && ray.y < 0.95) {
        float t = phase / 0.3;
        float height = 0.5 + 0.3 * hash(vec2(sector, floor(cycle)));
        vec2 local = vec2(fract(azimuth * 8.0), ray.y);
        vec2 head = vec2(0.2 + t * 0.5, height - t * 0.28);
        vec2 delta = local - head;
        vec2 direction = normalize(vec2(0.5, -0.28));
        float along = dot(delta, direction);
        float across = dot(delta, vec2(-direction.y, direction.x));
        float width = meteorWidth;
        float line = exp(-across * across / (width * width));
        float tail = clamp(1.0 + along / 0.22, 0.0, 1.0) * (1.0 - smoothstep(0.0, width, along));
        float core = exp(-dot(delta, delta) / (width * width * 4.0));
        vec3 tint = mix(skyColor.rgb, vec3(1.0), 0.55);
        color += tint * (line * tail * tail + core) * sin(t * 3.14159265);
    }
    fragColor = vec4(1.0 - exp(-color * misc.w), 1.0);
}
