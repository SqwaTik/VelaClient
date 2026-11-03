package dev.sqwat.vela.mixin;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Rules Mixin checks only when the game starts: broken ones crash the client before the title screen. */
class MixinRulesTest {
    private static final Path ROOT=Path.of("src/main/java/dev/sqwat/vela/mixin");
    private static final Pattern STATIC_METHOD=Pattern.compile("^\\s*(?:@\\S+\\s+)*(?:public\\s+|protected\\s+)?static\\s+(?!class|final\\s+class|record|interface|enum)[\\w<>\\[\\],.? ]+?\\s+[\\w$]+\\s*\\(");

    private static java.util.List<String> mixins()throws Exception{
        var json=JsonParser.parseString(Files.readString(Path.of("src/main/resources/velaclient.mixins.json"))).getAsJsonObject();
        var names=new ArrayList<String>();
        for(String key:new String[]{"mixins","client","server"})if(json.has(key))for(var e:json.getAsJsonArray(key))names.add(e.getAsString());
        return names;
    }
    @Test void mixinClassesHaveNoPublicStaticMethodsAndAreNotCalledDirectly()throws Exception{
        var problems=new ArrayList<String>();
        var names=mixins();
        for(String name:names){
            Path file=ROOT.resolve(name.replace('.','/')+".java");
            assertTrue(Files.exists(file),"missing "+file);
            String source=Files.readString(file);
            String simple=name.substring(name.lastIndexOf('.')+1);
            // Accessor and invoker interfaces may have static members and be called from anywhere.
            if(Pattern.compile("\\binterface\\s+"+simple+"\\b").matcher(source).find())continue;
            for(String line:source.split("\n"))
                if(STATIC_METHOD.matcher(line).find()&&!line.contains("private "))problems.add(simple+": non-private static method: "+line.strip());
            try(var files=Files.walk(Path.of("src/main/java"))){
                for(var other:files.filter(p->p.toString().endsWith(".java")&&!p.equals(file)).toList())
                    if(Pattern.compile("\\b"+simple+"\\s*\\.").matcher(Files.readString(other)).find())problems.add(simple+" is used directly from "+other.getFileName());
            }
        }
        assertTrue(problems.isEmpty(),String.join("\n",problems));
    }
}
