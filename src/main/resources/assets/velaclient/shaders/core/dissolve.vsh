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
out vec3 skinUv;
out vec4 parameters;
void main() {
    gl_Position=ProjMat*ModelViewMat*vec4(Position.xy,0.0,1.0);
    skinUv=vec3(UV0*Position.z,Position.z);
    parameters=Color;
}
