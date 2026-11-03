#version 330
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
uniform sampler2D Sampler0;
in vec2 localUv;
in vec2 sceneUv;
in vec4 vertexColor;
out vec4 fragColor;
void main(){
    vec2 motion=(vertexColor.rg*(255.0/254.0)-0.5)*0.16;
    // Under half a pixel of motion the 9 taps would all land on the same texel: one is enough.
    vec2 pixels=motion*vec2(textureSize(Sampler0,0));
    if(dot(pixels,pixels)<0.25){
        fragColor=vec4(texture(Sampler0,sceneUv).rgb,1.0)*ColorModulator;
        return;
    }
    vec3 sum=vec3(0.0);
    float total=0.0;
    for(int i=0;i<9;i++){
        float t=float(i)/8.0-0.5;
        float weight=1.0-abs(t);
        sum+=texture(Sampler0,clamp(sceneUv+motion*t,vec2(0.0),vec2(1.0))).rgb*weight;
        total+=weight;
    }
    fragColor=vec4(sum/total,1.0)*ColorModulator;
}
