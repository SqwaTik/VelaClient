#version 330
uniform sampler2D NoiseTex;
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

// Analytic sky illustration: fixed work per pixel, no volumetric ray marching.
void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 farPoint = invViewProj * vec4(ndc, mix(-0.9996, 0.0002, skyExtra.y), 1.0);
    vec4 nearPoint = invViewProj * vec4(ndc, 1.0, 1.0);
    vec3 ray = normalize(farPoint.xyz / farPoint.w - nearPoint.xyz / nearPoint.w);
    vec3 axis = normalize(vec3(0.34, 0.62, 0.71));
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), axis));
    vec3 up = cross(axis, right);
    float facing = dot(ray, axis);
    vec2 p = vec2(dot(ray, right), dot(ray, up)) / max(facing, 0.001);
    float radius = length(p);
    vec3 color = vec3(0.004, 0.006, 0.014);
    if (facing > 0.0 && radius < 1.4) {
        float angle = atan(p.y, p.x);
        float grain = texture(NoiseTex, vec2(angle / 6.2831853 + misc.x * 0.015, radius * 3.0)).r;
        float diskRadius = length(vec2(p.x, p.y * 4.2));
        float disk = exp(-abs(diskRadius - 0.29) * 18.0);
        float horizon = smoothstep(0.112, 0.128, radius);
        float ring = exp(-abs(radius - 0.132) * 160.0);
        float halo = exp(-abs(radius - 0.15) * 23.0) * 0.28;
        float flow = 0.7 + 0.3 * sin(diskRadius * 110.0 + angle * 3.0 - misc.x * 1.2);
        vec3 tint = mix(vec3(1.0, 0.58, 0.23), max(skyColor.rgb, vec3(0.08)), 0.45);
        color += tint * (disk * flow * (0.65 + grain * 0.7) + halo) * horizon;
        color += mix(tint, vec3(1.0), 0.65) * ring;
        color *= horizon;
    }
    fragColor = vec4(1.0 - exp(-color * misc.w), 1.0);
}
