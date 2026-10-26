#version 330
uniform sampler2D Sampler0;
in vec3 skinUv;
in vec4 parameters;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
float noise(vec2 p) {
    vec2 cell=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(hash(cell),hash(cell+vec2(1,0)),f.x),mix(hash(cell+vec2(0,1)),hash(cell+vec2(1,1)),f.x),f.y);
}
void main() {
    vec2 uv=skinUv.xy/skinUv.z;
    vec4 skin=texture(Sampler0,uv);
    if(skin.a<0.1)discard;
    float mask=noise(uv*90.0)*0.75+noise(uv*230.0)*0.25;
    float threshold=mix(-0.08,1.08,parameters.a);
    if(mask<threshold)discard;
    float edge=1.0-smoothstep(0.0,0.11,mask-threshold);
    vec3 glow=vec3(1.0,0.05,0.12)+vec3(0.6,0.12,0.08)*edge;
    fragColor=vec4(mix(skin.rgb*parameters.rgb,glow,edge),skin.a);
}
