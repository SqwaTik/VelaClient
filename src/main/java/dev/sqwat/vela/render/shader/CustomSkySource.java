package dev.sqwat.vela.render.shader;

/** Local mainImage contract; supplied code is compiled only after explicit Apply. */
public final class CustomSkySource {
    public static final int LIMIT=65536;
    public static final String DEFAULT="""
        void mainImage(out vec4 color, in vec2 pixel) {
            vec3 ray = skyDirection;
            float horizon = exp(-abs(ray.y) * 5.0);
            float wave = 0.5 + 0.5 * sin(ray.x * 6.0 + iTime);
            color = vec4(clientColor * (0.12 + horizon * 0.65 + wave * 0.12), 1.0);
        }
        """;
    private CustomSkySource(){}
    public static String validate(String source){
        if(source==null||source.isBlank())throw new IllegalArgumentException("Код шейдера пуст");
        if(source.length()>LIMIT)throw new IllegalArgumentException("Максимум 65536 символов");
        if(source.indexOf('\0')>=0)throw new IllegalArgumentException("Недопустимый нулевой символ");
        return source;
    }
    public static String fragment(String source){return """
        #version 330
        layout(std140) uniform SkyParams {
            mat4 invViewProj; vec4 misc; vec4 skyColor; vec4 skyColor2;
            vec4 taa; mat4 prevViewProj; vec4 skyExtra;
        };
        in vec2 texCoord;
        out vec4 fragColor;
        #define iTime misc.x
        #define iResolution vec3(misc.yz, 1.0)
        #define clientColor skyColor.rgb
        vec3 skyDirection;
        vec2 skyPanoramaUv;
        #line 1
        """+validate(source)+"""

        void main() {
            vec2 ndc = texCoord * 2.0 - 1.0;
            vec4 farP = invViewProj * vec4(ndc, mix(-0.9996, 0.0002, skyExtra.y), 1.0);
            vec4 nearP = invViewProj * vec4(ndc, 1.0, 1.0);
            skyDirection = normalize(farP.xyz/farP.w - nearP.xyz/nearP.w);
            skyPanoramaUv = vec2(atan(skyDirection.z,skyDirection.x)/6.2831853+0.5, asin(clamp(skyDirection.y,-1.0,1.0))/3.14159265+0.5);
            vec4 color = vec4(0.0,0.0,0.0,1.0);
            mainImage(color,texCoord*misc.yz);
            fragColor = vec4(max(color.rgb,vec3(0.0))*misc.w,1.0);
        }
        """;}
}
