package dev.sqwat.vela.ui.render;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Line tokenizer for the GLSL editor's colours, and the line numbers named in driver compile errors. */
public final class GlslSyntax {
    private GlslSyntax(){}

    public enum Kind{PLAIN,KEYWORD,TYPE,BUILTIN,EDITOR,NUMBER,COMMENT,PREPROCESSOR,PUNCTUATION}
    /** {@code [start,end)} of the line. */
    public record Span(int start,int end,Kind kind){}
    /** Spans of one line, and whether a block comment is still open at its end. */
    public record Line(List<Span> spans,boolean inComment){}

    private static final Set<String> KEYWORDS=Set.of("if","else","for","while","do","return","break","continue","discard",
        "switch","case","default","in","out","inout","const","uniform","layout","struct","true","false","precision",
        "highp","mediump","lowp","flat","smooth");
    private static final Set<String> TYPES=Set.of("void","bool","int","uint","float","double","vec2","vec3","vec4",
        "ivec2","ivec3","ivec4","uvec2","uvec3","uvec4","bvec2","bvec3","bvec4","mat2","mat3","mat4","mat2x2","mat3x3",
        "mat4x4","sampler2D");
    private static final Set<String> BUILTINS=Set.of("abs","acos","asin","atan","ceil","clamp","cos","cross","degrees",
        "distance","dot","exp","exp2","faceforward","floor","fract","inversesqrt","length","log","log2","max","min","mix",
        "mod","normalize","pow","radians","reflect","refract","round","sign","sin","smoothstep","sqrt","step","tan","tanh",
        "sinh","cosh","texture","transpose","inverse","determinant","fwidth","dFdx","dFdy","trunc","isnan","isinf",
        "floatBitsToUint","uintBitsToFloat","floatBitsToInt","intBitsToFloat","lessThan","greaterThan","equal","any","all",
        "not","gl_FragCoord");
    /** What the editor's wrapper provides to mainImage. */
    public static final List<String> EDITOR_NAMES=List.of("mainImage","iTime","iResolution","skyDirection","skyPanoramaUv","clientColor");
    private static final Set<String> EDITOR=Set.copyOf(EDITOR_NAMES);

    public static Line line(String line,boolean inComment){
        var spans=new ArrayList<Span>();
        int n=line.length(),i=0;
        if(inComment){
            int close=line.indexOf("*/");
            if(close<0){spans.add(new Span(0,n,Kind.COMMENT));return new Line(spans,true);}
            spans.add(new Span(0,close+2,Kind.COMMENT));i=close+2;
        }
        int first=0;while(first<n&&line.charAt(first)==' ')first++;
        if(i==0&&first<n&&line.charAt(first)=='#'){
            int comment=line.indexOf("//",first);
            spans.add(new Span(first,comment<0?n:comment,Kind.PREPROCESSOR));
            if(comment>=0)spans.add(new Span(comment,n,Kind.COMMENT));
            return new Line(spans,false);
        }
        while(i<n){
            char c=line.charAt(i);
            if(c==' '){i++;continue;}
            if(c=='/'&&i+1<n&&line.charAt(i+1)=='/'){spans.add(new Span(i,n,Kind.COMMENT));return new Line(spans,false);}
            if(c=='/'&&i+1<n&&line.charAt(i+1)=='*'){
                int close=line.indexOf("*/",i+2);
                if(close<0){spans.add(new Span(i,n,Kind.COMMENT));return new Line(spans,true);}
                spans.add(new Span(i,close+2,Kind.COMMENT));i=close+2;continue;
            }
            if(Character.isDigit(c)||(c=='.'&&i+1<n&&Character.isDigit(line.charAt(i+1)))){
                int j=i+1;
                while(j<n){
                    char d=line.charAt(j);
                    if(Character.isLetterOrDigit(d)||d=='.')j++;
                    else if((d=='-'||d=='+')&&(line.charAt(j-1)=='e'||line.charAt(j-1)=='E'))j++;
                    else break;
                }
                spans.add(new Span(i,j,Kind.NUMBER));i=j;continue;
            }
            if(Character.isLetter(c)||c=='_'){
                int j=i+1;while(j<n&&(Character.isLetterOrDigit(line.charAt(j))||line.charAt(j)=='_'))j++;
                String word=line.substring(i,j);
                Kind kind=KEYWORDS.contains(word)?Kind.KEYWORD:TYPES.contains(word)?Kind.TYPE:EDITOR.contains(word)?Kind.EDITOR
                    :BUILTINS.contains(word)?Kind.BUILTIN:Kind.PLAIN;
                spans.add(new Span(i,j,kind));i=j;continue;
            }
            int j=i+1;
            while(j<n&&"{}()[];,.+-*/%=<>!&|^?:~".indexOf(line.charAt(j))>=0&&"{}()[];,.+-*/%=<>!&|^?:~".indexOf(c)>=0
                    &&!(line.charAt(j)=='/'&&j+1<n&&(line.charAt(j+1)=='/'||line.charAt(j+1)=='*')))j++;
            spans.add(new Span(i,j,Kind.PUNCTUATION));i=j;
        }
        return new Line(spans,false);
    }

    // NVIDIA "0(12) : error", Mesa/AMD/Intel "0:12(5): error" and "ERROR: 0:12:". The wrapper puts "#line 1" before
    // the user's code, so these are the editor's own line numbers.
    private static final Pattern ERROR_LINE=Pattern.compile("(?<![\\d.])0[:(](\\d{1,6})");

    /** Zero-based editor lines named in a compile log. */
    public static BitSet errorLines(String log,int lineCount){
        var lines=new BitSet();
        if(log==null)return lines;
        Matcher m=ERROR_LINE.matcher(log);
        while(m.find()){
            try{int line=Integer.parseInt(m.group(1))-1;if(line>=0&&line<lineCount)lines.set(line);}catch(NumberFormatException ignored){}
        }
        return lines;
    }
}
