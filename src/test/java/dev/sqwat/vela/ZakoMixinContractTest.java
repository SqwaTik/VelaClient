package dev.sqwat.vela;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import java.io.InputStreamReader;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Resolves injection methods against the actual Minecraft dependency without booting a game. */
class ZakoMixinContractTest {
    private static ClassNode read(String name) throws Exception {
        try(var in=ZakoMixinContractTest.class.getClassLoader().getResourceAsStream(name+".class")){
            assertNotNull(in,"Missing class "+name);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;
        }
    }
    private static Object value(AnnotationNode a,String key){
        if(a.values!=null)for(int n=0;n<a.values.size();n+=2)if(key.equals(a.values.get(n)))return a.values.get(n+1);
        return null;
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> a,List<AnnotationNode> b){
        var out=new ArrayList<AnnotationNode>();if(a!=null)out.addAll(a);if(b!=null)out.addAll(b);return out;
    }
    private static List<?> list(Object v){return v instanceof List<?> l?l:v==null?List.of():List.of(v);}
    @Test void injectionMethodsAndInvokeTargetsExist() throws Exception {
        try(var in=getClass().getResourceAsStream("/vela-zako.mixins.json")){
            assertNotNull(in);
            var config=JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            int checked=0;
            var mixins=new LinkedHashSet<String>();
            for(var item:config.getAsJsonArray("client"))mixins.add(item.getAsString());
            for(String name:dev.sqwat.vela.zako.MixinPlugin.SODIUM_MIXINS)
                if(dev.sqwat.vela.zako.MixinPlugin.built(name)&&!name.contains("OldMixin"))mixins.add(name);
            for(String shortName:mixins){
                // These optional integrations have no target when their external mods are absent.
                if(shortName.contains("Lunar")||shortName.contains("ImmediatelyFast"))continue;
                var mixin=read("dev/sqwat/vela/zako/mixin/"+shortName.replace('.','/'));
                var marker=annotations(mixin.visibleAnnotations,mixin.invisibleAnnotations).stream().filter(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
                var targets=new ArrayList<String>();
                for(var v:list(value(marker,"value")))targets.add(((Type)v).getInternalName());
                for(var v:list(value(marker,"targets")))targets.add(v.toString().replace('.','/'));
                for(var targetName:targets){
                    var target=read(targetName);
                    for(var method:mixin.methods)for(var annotation:annotations(method.visibleAnnotations,method.invisibleAnnotations)){
                        Object selectors=value(annotation,"method");if(selectors==null)continue;
                        for(var selector:list(selectors)){
                            String name=selector.toString();int d=name.indexOf('(');
                            String base=d<0?name:name.substring(0,d);String desc=d<0?null:name.substring(d);
                            var matches=target.methods.stream().filter(m->(base.equals("*")||m.name.equals(base))&&(desc==null||m.desc.equals(desc))).toList();
                            assertFalse(matches.isEmpty(),shortName+" -> "+targetName+"."+name);
                            for(var atValue:list(value(annotation,"at"))){
                                if(!(atValue instanceof AnnotationNode at)||!"INVOKE".equals(value(at,"value")))continue;
                                String invoke=(String)value(at,"target");if(invoke==null||!invoke.startsWith("L"))continue;
                                int sep=invoke.indexOf(';'),args=invoke.indexOf('(',sep);if(args<0)continue;
                                String owner=invoke.substring(1,sep),called=invoke.substring(sep+1,args),signature=invoke.substring(args);
                                boolean found=matches.stream().flatMap(m->Arrays.stream(m.instructions.toArray())).anyMatch(i->i instanceof MethodInsnNode call&&call.owner.equals(owner)&&call.name.equals(called)&&call.desc.equals(signature));
                                assertTrue(found,shortName+"."+method.name+" -> "+name+" missing "+invoke);
                            }
                            checked++;
                        }
                    }
                }
            }
            assertTrue(checked>50,"Expected complete native mixin coverage");
        }
    }
}
