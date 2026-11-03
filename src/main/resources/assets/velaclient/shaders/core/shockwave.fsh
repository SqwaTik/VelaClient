#version 330
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
uniform sampler2D Sampler0;
in vec3 waveUv;
in vec2 sceneUv;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec2 p=(waveUv.xy/max(waveUv.z,.000001))*2.0-1.0;
    float radius=length(p);
    float band=smoothstep(.25,.58,radius)*(1.0-smoothstep(.78,1.0,radius));
    // Screen derivatives follow the projected ring even at oblique camera angles.
    vec2 direction=normalize(vec2(dFdx(radius),dFdy(radius))+vec2(.000001));
    if(band<.005)discard;
    vec2 texel=1.0/vec2(textureSize(Sampler0,0));
    float slope=(radius-.68)/.32;
    float distortion=-sin(clamp(slope,-1.0,1.0)*3.14159265)*band;
    // Broad compression / rarefaction front. Fade displacement with energy as well
    // as opacity; no opaque ring or duplicate white outline.
    vec2 offset=direction*texel*distortion*38.0*vertexColor.a;
    vec2 uv=clamp(sceneUv+offset,texel,1.0-texel);
    vec3 refracted=vec3(texture(Sampler0,clamp(uv+offset*.08,texel,1.0-texel)).r,
        texture(Sampler0,uv).g,texture(Sampler0,clamp(uv-offset*.08,texel,1.0-texel)).b);
    float crestX=(radius-.76)*15.3846154;
    float crest=exp(-crestX*crestX);
    vec3 glass=mix(refracted,vertexColor.rgb,.035*band);
    glass+=mix(vertexColor.rgb,vec3(1.0),.5)*crest*.10;
    fragColor=vec4(glass,band*vertexColor.a)*ColorModulator;
}
