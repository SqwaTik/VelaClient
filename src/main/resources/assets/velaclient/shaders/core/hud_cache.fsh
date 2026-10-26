#version 330
uniform sampler2D Sampler0;
in vec2 sceneUv;
out vec4 fragColor;
void main() {
    fragColor = texture(Sampler0, sceneUv);
}
