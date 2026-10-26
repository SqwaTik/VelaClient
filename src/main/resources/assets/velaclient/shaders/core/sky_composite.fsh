#version 330
uniform sampler2D Sky;
uniform sampler2D Bloom0;
uniform sampler2D Bloom1;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 sky = texture(Sky, texCoord).rgb;
    vec3 bloom = texture(Bloom0, texCoord).rgb
               + texture(Bloom1, texCoord).rgb * 1.2;
    fragColor = vec4(sky + bloom * 0.34, 1.0);
}
