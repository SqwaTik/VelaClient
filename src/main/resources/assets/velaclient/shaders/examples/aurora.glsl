void mainImage(out vec4 color, in vec2 pixel) {
    vec3 ray = normalize(skyDirection);
    float angle = atan(ray.z, ray.x);
    float wave = sin(angle * 5.0 + iTime * 0.35) * 0.08;
    wave += sin(angle * 11.0 - iTime * 0.2) * 0.025;
    float ribbon = exp(-abs(ray.y - 0.25 - wave) * 22.0);
    float curtain = 0.6 + 0.4 * sin(angle * 80.0 + iTime);
    vec3 tint = mix(clientColor, vec3(0.15, 0.85, 0.8), 0.25);
    vec3 sky = vec3(0.008, 0.014, 0.035);
    sky += tint * ribbon * curtain * 1.6;
    color = vec4(sky, 1.0);
}
