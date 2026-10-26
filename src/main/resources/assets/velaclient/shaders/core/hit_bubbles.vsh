#version 330
layout(std140) uniform BubbleParams {
    vec4 config;
    vec4 appearance;
    vec4 colorA;
    vec4 colorB;
    vec4 bounds;
    vec4 waves[16];
};
out vec2 texCoord;
void main() {
    vec2 corners[6]=vec2[](vec2(0,0),vec2(1,0),vec2(1,1),vec2(0,0),vec2(1,1),vec2(0,1));
    texCoord=mix(bounds.xy,bounds.zw,corners[gl_VertexID]);
    gl_Position=vec4(texCoord*2.0-1.0,0,1);
}
