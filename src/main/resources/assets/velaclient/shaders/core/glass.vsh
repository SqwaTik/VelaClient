#version 330
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection { mat4 ProjMat; };
in vec3 Position;
in vec2 UV0;
in vec4 Color;
out vec2 localUv;
out vec2 sceneUv;
out vec4 vertexColor;
void main() {
    vec4 clip=ProjMat*ModelViewMat*vec4(Position,1.0);
    gl_Position=clip;
    sceneUv=clip.xy/clip.w*.5+.5;
    localUv=UV0;
    vertexColor=Color;
}
