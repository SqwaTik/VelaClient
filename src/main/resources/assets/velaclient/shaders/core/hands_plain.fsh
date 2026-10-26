#version 330
uniform sampler2D SceneSampler;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 hand = texture(SceneSampler, texCoord);
    fragColor = hand;
}

