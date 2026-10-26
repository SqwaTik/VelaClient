#version 330
uniform sampler2D Sky;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    fragColor = vec4(texture(Sky, texCoord).rgb, 1.0);
}
