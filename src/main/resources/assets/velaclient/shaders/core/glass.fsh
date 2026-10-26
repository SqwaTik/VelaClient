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
void main() {
    vec2 extent=1.0/max(fwidth(localUv),vec2(.00001));
    vec2 p=(localUv-.5)*extent;
    float radius=min(9.0,min(extent.x,extent.y)*.25);
    vec2 q=abs(p)-(extent*.5-radius);
    float sdf=length(max(q,0.0))+min(max(q.x,q.y),0.0)-radius;
    float coverage=1.0-smoothstep(-.7,.7,sdf);
    if(coverage<=0.0)discard;
    vec2 texel=1.0/vec2(textureSize(Sampler0,0));
    // Edge Fresnel and separated colour refraction, following the Kimiko glass
    // surface model without its palette arrays or repeated full-screen passes.
    float fresnel=pow(1.0-clamp(-sdf/max(1.0,min(extent.x,extent.y)*.5),0.0,1.0),3.0);
    vec2 direction=p/max(length(p),1.0);
    vec2 offset=-direction*texel*fresnel*8.0;
    vec2 uv=clamp(sceneUv+offset,texel,1.0-texel);
    vec3 refracted=vec3(texture(Sampler0,clamp(uv+offset*.12,texel,1.0-texel)).r,
        texture(Sampler0,uv).g,texture(Sampler0,clamp(uv-offset*.12,texel,1.0-texel)).b);
    vec3 glass=mix(refracted*.78,vertexColor.rgb,.055+fresnel*.06);
    float edge=1.0-smoothstep(.3,1.8,abs(sdf));
    glass+=mix(vertexColor.rgb,vec3(1.0),.7)*edge*(.12+.14*(1.0-localUv.y));
    fragColor=vec4(glass,coverage*mix(.88,.98,fresnel)*vertexColor.a)*ColorModulator;
}
