#version 330
uniform sampler2D Scene;
layout(std140) uniform BubbleParams {
    vec4 config;
    vec4 appearance;
    vec4 colorA;
    vec4 colorB;
    vec4 bounds;
    vec4 waves[16];
};
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 offset=vec2(0);
    float influence=0.0,crest=0.0;
    float width=clamp(appearance.w*0.4,0.06,0.22);
    for(int i=0;i<16;i++){
        if(i>=int(config.x))break;
        vec2 delta=(texCoord-waves[i].xy)*vec2(config.y,1);
        float distance=length(delta),radius=waves[i].z;
        if(distance>radius+width*2.0)continue;
        float d=(distance-radius)/width;
        float ring=exp(-d*d*8.0)*waves[i].w;
        vec2 direction=delta/max(distance,0.0001);
        offset+=direction*ring*cos(d*7.0)*0.018*appearance.z/vec2(config.y,1);
        float inside=(1.0-smoothstep(radius,radius+width,distance))*waves[i].w;
        if(config.w>0.0)offset+=sin(delta.yx*45.0+config.z*3.0)*config.w*inside/vec2(config.y,1);
        influence=max(influence,inside);crest=max(crest,ring);
    }
    if(influence<0.001&&crest<0.001)discard;
    vec2 pixel=1.0/vec2(textureSize(Scene,0));
    vec3 color=texture(Scene,clamp(texCoord+clamp(offset,vec2(-0.08),vec2(0.08)),pixel,1.0-pixel)).rgb;
    float luminance=dot(color,vec3(0.2126,0.7152,0.0722));
    color=mix(color,mix(vec3(luminance),color,appearance.x),influence);
    vec3 tint=mix(colorA.rgb,colorB.rgb,0.5+0.5*sin(texCoord.y*9.0+config.z));
    color=mix(color,tint,clamp(crest*appearance.y*0.55,0.0,0.75));
    color+=tint*crest*appearance.y*0.12;
    fragColor=vec4(clamp(color,0.0,1.0),1);
}
