package dev.sqwat.vela;

import dev.sqwat.vela.zako.GuardWeaver;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/** JVM-verifies the actual generated wrapper, including wide arguments and fallback returns. */
public class ZakoGuardWeaverTest {
    @Test void mixinEntryPointWrapsOnlyOnceAndLeavesOperationHandlersAlone(){
        var target=new ClassNode();target.name="vela/test/GuardSelection";target.superName="java/lang/Object";
        var safe=new MethodNode(ACC_STATIC,"zakoopt$safe","()V",null,null);safe.instructions.add(new InsnNode(RETURN));
        var operation=new MethodNode(ACC_STATIC,"zakoopt$operation","(Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)V",null,null);operation.instructions.add(new InsnNode(RETURN));
        target.methods.add(safe);target.methods.add(operation);
        var source=new ClassNode();source.methods.add(safe);source.methods.add(operation);
        var info=(org.spongepowered.asm.mixin.extensibility.IMixinInfo)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{org.spongepowered.asm.mixin.extensibility.IMixinInfo.class},(p,m,a)->m.getName().equals("getClassNode")?source:null);
        GuardWeaver.weave(target,"dev.sqwat.vela.zako.mixin.test.Selection",info);
        assertEquals(4,target.methods.size());assertTrue(operation.tryCatchBlocks.isEmpty());
        GuardWeaver.weave(target,"dev.sqwat.vela.zako.mixin.test.Selection",info);assertEquals(4,target.methods.size());
    }
    public static void failure(Throwable error,String mixin,String handler){}
    private Class<?> wrapped(String desc,boolean throwing) throws Exception {
        var target=new ClassNode();target.version=V17;target.access=ACC_PUBLIC;target.name="vela/test/GeneratedGuard";target.superName="java/lang/Object";
        var method=new MethodNode(ACC_PUBLIC|ACC_STATIC,"zakoopt$test",desc,null,null);
        if(throwing){
            method.instructions.add(new TypeInsnNode(NEW,"java/lang/IllegalStateException"));method.instructions.add(new InsnNode(DUP));
            method.instructions.add(new MethodInsnNode(INVOKESPECIAL,"java/lang/IllegalStateException","<init>","()V",false));method.instructions.add(new InsnNode(ATHROW));
        }else{
            method.instructions.add(new VarInsnNode(LLOAD,0));method.instructions.add(new InsnNode(LCONST_1));method.instructions.add(new InsnNode(LADD));method.instructions.add(new InsnNode(LRETURN));
        }
        target.methods.add(method);
        var wrap=GuardWeaver.class.getDeclaredMethod("wrap",ClassNode.class,MethodNode.class,String.class,String.class,boolean.class);wrap.setAccessible(true);
        wrap.invoke(null,target,method,"dev.sqwat.vela.zako.mixin.test.Fixture","zakoopt$test",false);
        for(var m:target.methods)for(var instruction:m.instructions)
            if(instruction instanceof MethodInsnNode call&&call.owner.equals("dev/sqwat/vela/zako/Guard"))call.owner=Type.getInternalName(ZakoGuardWeaverTest.class);
        // The production failure handler is replaced only to isolate bytecode from game/log state.
        for(var m:target.methods)for(var instruction:m.instructions)
            if(instruction instanceof MethodInsnNode call&&call.owner.equals(Type.getInternalName(ZakoGuardWeaverTest.class)))call.name="failure";
        var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);target.accept(writer);
        byte[] bytes=writer.toByteArray();
        return new ClassLoader(getClass().getClassLoader()){Class<?> define(){return defineClass(null,bytes,0,bytes.length);}}.define();
    }
    @Test void normalBodyRetainsItsReturn()throws Exception{assertEquals(8L,wrapped("(J)J",false).getMethod("zakoopt$test",long.class).invoke(null,7L));}
    @Test void failedModifierReturnsOriginalWideValue()throws Exception{assertEquals(7L,wrapped("(J)J",true).getMethod("zakoopt$test",long.class).invoke(null,7L));}
    @Test void voidHandlerCanReturnSafely()throws Exception{assertNull(wrapped("()V",true).getMethod("zakoopt$test").invoke(null));}
    @Test void handlerWithoutValidFallbackRethrows()throws Exception{
        var method=wrapped("()I",true).getMethod("zakoopt$test");
        var error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->method.invoke(null));assertInstanceOf(IllegalStateException.class,error.getCause());
    }
}
