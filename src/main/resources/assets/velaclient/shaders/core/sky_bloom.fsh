#version 330
uniform sampler2D Source;
layout(std140) uniform BloomParams { vec4 params; };
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 d = params.xy;
    // Four bilinear taps cover the downsample footprint.
    vec3 color = texture(Source, texCoord + d).rgb;
    color += texture(Source, texCoord - d).rgb;
    color += texture(Source, texCoord + vec2(d.x, -d.y)).rgb;
    color += texture(Source, texCoord + vec2(-d.x, d.y)).rgb;
    fragColor = vec4(max(color * 0.25 - params.z, vec3(0.0)), 1.0);
}
