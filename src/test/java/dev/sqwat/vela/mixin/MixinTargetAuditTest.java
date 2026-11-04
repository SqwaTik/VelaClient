package dev.sqwat.vela.mixin;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Finds mixins whose targets are gone without starting the game: every injector's target method, its {@code @At}
 * call, field or constructor inside that method, {@code @Shadow} members, accessors and invokers, and the argument
 * list of {@code @Inject} handlers are checked against the game classes on the test classpath. Mixin reports these
 * only when the target class loads, one crash at a time; a port to another game version finds them all here.
 * Injectors with {@code require = 0} may miss.
 */
class MixinTargetAuditTest {
    private static final Set<String> INJECTORS = Set.of(
        "Lorg/spongepowered/asm/mixin/injection/Inject;",
        "Lorg/spongepowered/asm/mixin/injection/Redirect;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",
        "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;",
        "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;",
        "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;",
        "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",
        "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
        "Lcom/llamalad7/mixinextras/injector/ModifyReceiver;",
        "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;");
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String WRAP_OPERATION = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";
    private static final Set<String> MODIFY_VALUE = Set.of(
        "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",
        "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;");

    private final Map<String, Optional<ClassNode>> classes = new HashMap<>();

    @Test void everyMixinTargetExists() throws IOException {
        var problems = new ArrayList<String>();
        int mixins = 0;
        try (var configs = Files.list(Path.of("src/main/resources"))) {
            for (var config : configs.filter(p -> p.getFileName().toString().matches(".*mixins.*\\.json")).sorted().toList()) {
                var json = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
                String pkg = json.get("package").getAsString().replace('.', '/');
                for (String side : List.of("mixins", "client", "server")) {
                    if (!json.has(side)) continue;
                    for (var entry : json.getAsJsonArray(side)) {
                        String name = pkg + "/" + entry.getAsString().replace('.', '/');
                        var mixin = node(name);
                        if (mixin.isEmpty()) { problems.add(config.getFileName() + ": " + name + " is not compiled"); continue; }
                        mixins++;
                        audit(mixin.get(), problems);
                    }
                }
            }
        }
        assertTrue(mixins > 0);
        assertTrue(problems.isEmpty(), problems.size() + " broken mixin targets:\n" + String.join("\n", problems));
    }

    private void audit(ClassNode mixin, List<String> problems) {
        var annotation = find(mixin.invisibleAnnotations, MIXIN);
        if (annotation == null) return;
        var targets = new ArrayList<ClassNode>();
        var names = new ArrayList<String>();
        for (Object t : list(value(annotation, "value"))) names.add(((Type) t).getInternalName());
        for (Object t : list(value(annotation, "targets"))) names.add(((String) t).replace('.', '/'));
        for (String name : names) node(name).ifPresentOrElse(targets::add, () -> {
            // Other mods' classes are only there when that mod is installed; the config plugin skips those mixins.
            if (name.startsWith("net/minecraft/") || name.startsWith("com/mojang/")) problems.add(simple(mixin) + ": target class " + name + " is gone");
        });
        if (targets.isEmpty()) return;
        for (var field : mixin.fields) if (find(field.visibleAnnotations, SHADOW) != null || find(field.invisibleAnnotations, SHADOW) != null)
            for (var target : targets) if (field(target, shadowName(field.name, field.visibleAnnotations, field.invisibleAnnotations), null) == null)
                problems.add(simple(mixin) + ": @Shadow field " + field.name + " is gone from " + target.name);
        for (var method : mixin.methods) {
            var annotations = new ArrayList<AnnotationNode>();
            if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
            if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
            for (var a : annotations) {
                String where = simple(mixin) + "." + method.name;
                if (a.desc.equals(SHADOW)) {
                    String name = shadowName(method.name, method.visibleAnnotations, method.invisibleAnnotations);
                    for (var target : targets) if (method(target, name, method.desc) == null)
                        problems.add(where + ": @Shadow method " + name + method.desc + " is gone from " + target.name);
                } else if (a.desc.equals(ACCESSOR) || a.desc.equals(INVOKER)) {
                    accessor(where, a, method, targets, problems);
                } else if (INJECTORS.contains(a.desc)) {
                    Object require = value(a, "require");
                    if (require instanceof Integer r && r == 0) continue;
                    injector(where, a, method, targets, problems);
                }
            }
        }
    }

    private void accessor(String where, AnnotationNode a, MethodNode method, List<ClassNode> targets, List<String> problems) {
        String name = (String) value(a, "value");
        boolean invoker = a.desc.equals(INVOKER);
        if (name == null || name.isEmpty()) name = method.name.replaceFirst(invoker ? "^(call|invoke|new|create)" : "^(get|set|is)", "");
        if (name.isEmpty()) return;
        String given = name;
        for (var target : targets) {
            boolean found;
            if (invoker) found = name.equals("<init>") || target.methods.stream().anyMatch(m -> m.name.equals(given) || m.name.equalsIgnoreCase(given));
            else found = field(target, name, null) != null || field(target, Character.toLowerCase(name.charAt(0)) + name.substring(1), null) != null
                || target.fields.stream().anyMatch(f -> f.name.equalsIgnoreCase(given));
            if (!found) problems.add(where + ": " + (invoker ? "@Invoker " : "@Accessor ") + name + " is gone from " + target.name);
        }
    }

    private void injector(String where, AnnotationNode a, MethodNode handler, List<ClassNode> targets, List<String> problems) {
        var selectors = list(value(a, "method"));
        var ats = new ArrayList<AnnotationNode>();
        Object at = value(a, "at");
        if (at instanceof AnnotationNode one) ats.add(one);
        else for (Object o : list(at)) ats.add((AnnotationNode) o);
        for (var target : targets) {
            // several selectors (often one name per game version) only need one match between them
            var missing = new ArrayList<String>();
            boolean matched = false;
            for (Object s : selectors) {
                String selector = (String) s;
                if (selector.contains("*") || selector.startsWith("/")) { matched = true; continue; }
                var candidates = select(target, selector);
                if (candidates.isEmpty()) { missing.add(selector); continue; }
                matched = true;
                for (var ref : ats) {
                    String kind = (String) value(ref, "value"), wanted = (String) value(ref, "target");
                    if (wanted == null || wanted.isEmpty()) continue;
                    if (candidates.stream().noneMatch(m -> contains(m, kind, wanted)))
                        problems.add(where + ": " + kind + " " + wanted + " is no longer in " + target.name + "." + selector);
                }
                if (a.desc.equals(INJECT) && find(handler.visibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/Coerce;") == null)
                    for (var m : candidates) if (!injectArgsFit(m, handler))
                        problems.add(where + ": @Inject handler " + handler.desc + " does not fit " + target.name + "." + m.name + m.desc);
                if (a.desc.equals(WRAP_OPERATION))
                    for (var m : candidates) if (!wrapArgsFit(m, handler))
                        problems.add(where + ": @WrapOperation handler " + handler.desc + " captures arguments " + target.name + "." + m.name + m.desc + " does not have");
                if (MODIFY_VALUE.contains(a.desc))
                    for (var m : candidates) if (!capturedArgsFit(m, handler, 1))
                        problems.add(where + ": value modifier " + handler.desc + " captures arguments " + target.name + "." + m.name + m.desc + " does not have");
            }
            if (!matched) problems.add(where + ": method " + String.join(" / ", missing) + " is gone from " + target.name);
        }
    }

    /** Handler arguments are the target's arguments (all or none), a CallbackInfo, then optional captured locals. */
    private static boolean injectArgsFit(MethodNode target, MethodNode handler) {
        var targetArgs = Type.getArgumentTypes(target.desc);
        var handlerArgs = Type.getArgumentTypes(handler.desc);
        int ci = -1;
        for (int i = 0; i < handlerArgs.length; i++) if (handlerArgs[i].getInternalName().startsWith("org/spongepowered/asm/mixin/injection/callback/CallbackInfo")) { ci = i; break; }
        if (ci < 0) return false;
        if (ci == 0) return true;
        if (ci != targetArgs.length) return false;
        for (int i = 0; i < ci; i++) if (!handlerArgs[i].equals(targetArgs[i])) return false;
        return true;
    }

    /** After the Operation a wrapper may capture the target's first arguments. */
    private static boolean wrapArgsFit(MethodNode target, MethodNode handler) {
        var handlerArgs = Type.getArgumentTypes(handler.desc);
        int op = -1;
        for (int i = 0; i < handlerArgs.length; i++) if (handlerArgs[i].getInternalName().equals("com/llamalad7/mixinextras/injector/wrapoperation/Operation")) { op = i; break; }
        return op < 0 || capturedArgsFit(target, handler, op + 1);
    }

    /** From handler argument {@code from} on, the handler may capture the target's first arguments; @Local/@Share ones are skipped. */
    private static boolean capturedArgsFit(MethodNode target, MethodNode handler, int from) {
        var targetArgs = Type.getArgumentTypes(target.desc);
        var handlerArgs = Type.getArgumentTypes(handler.desc);
        int captured = 0;
        for (int i = from; i < handlerArgs.length; i++) {
            if (annotated(handler.visibleParameterAnnotations, i) || annotated(handler.invisibleParameterAnnotations, i)) continue;
            if (captured >= targetArgs.length || !handlerArgs[i].equals(targetArgs[captured])) return false;
            captured++;
        }
        return true;
    }

    private static boolean annotated(List<AnnotationNode>[] annotations, int index) {
        return annotations != null && index < annotations.length && annotations[index] != null && !annotations[index].isEmpty();
    }

    private List<MethodNode> select(ClassNode target, String selector) {
        String s = selector;
        if (s.startsWith("L") && s.contains(";")) s = s.substring(s.indexOf(';') + 1);
        int paren = s.indexOf('(');
        String name = paren < 0 ? s : s.substring(0, paren), desc = paren < 0 ? null : s.substring(paren);
        var result = new ArrayList<MethodNode>();
        for (var m : target.methods) if (m.name.equals(name) && (desc == null || m.desc.equals(desc))) result.add(m);
        // a bare name hits every real overload of that name; bridge methods are skipped
        if (desc == null) result.removeIf(m -> (m.access & org.objectweb.asm.Opcodes.ACC_BRIDGE) != 0);
        return result;
    }

    private boolean contains(MethodNode method, String kind, String wanted) {
        if (method.instructions == null) return false;
        switch (kind) {
            case "INVOKE", "INVOKE_ASSIGN", "INVOKE_STRING" -> {
                var ref = member(wanted);
                if (ref == null) return true;
                for (var insn : method.instructions) {
                    if (insn instanceof MethodInsnNode m && (ref[0] == null || m.owner.equals(ref[0])) && m.name.equals(ref[1]) && (ref[2] == null || m.desc.equals(ref[2]))) return true;
                    if (insn instanceof InvokeDynamicInsnNode d) for (Object arg : d.bsmArgs)
                        if (arg instanceof org.objectweb.asm.Handle h && h.getName().equals(ref[1]) && (ref[0] == null || h.getOwner().equals(ref[0]))) return true;
                }
                return false;
            }
            case "FIELD" -> {
                int colon = wanted.indexOf(':');
                var ref = member(colon < 0 ? wanted : wanted.substring(0, colon) + "()V");
                if (ref == null) return true;
                String desc = colon < 0 ? null : wanted.substring(colon + 1);
                for (var insn : method.instructions)
                    if (insn instanceof FieldInsnNode f && (ref[0] == null || f.owner.equals(ref[0])) && f.name.equals(ref[1]) && (desc == null || f.desc.equals(desc))) return true;
                return false;
            }
            case "NEW" -> {
                String owner = wanted.startsWith("(") ? Type.getReturnType(wanted).getInternalName()
                    : wanted.startsWith("L") ? wanted.substring(1, wanted.length() - 1) : wanted.replace('.', '/');
                String desc = wanted.startsWith("(") ? wanted.substring(0, wanted.indexOf(')') + 1) + "V" : null;
                boolean made = false;
                for (var insn : method.instructions) {
                    if (insn instanceof TypeInsnNode t && t.getOpcode() == org.objectweb.asm.Opcodes.NEW && t.desc.equals(owner)) made = true;
                    if (made && insn instanceof MethodInsnNode m && m.name.equals("<init>") && m.owner.equals(owner) && (desc == null || m.desc.equals(desc))) return true;
                }
                return made && desc == null;
            }
            default -> { return true; }
        }
    }

    /** "Lowner;name(desc)" to {owner, name, desc}; null when the target is not in that form. */
    private static String[] member(String target) {
        String owner = null, rest = target;
        if (target.startsWith("L") && target.contains(";")) { owner = target.substring(1, target.indexOf(';')); rest = target.substring(target.indexOf(';') + 1); }
        int paren = rest.indexOf('(');
        if (paren <= 0) return null;
        return new String[]{owner, rest.substring(0, paren), rest.substring(paren)};
    }

    private MethodNode method(ClassNode type, String name, String desc) {
        for (var c = Optional.of(type); c.isPresent(); c = c.get().superName == null ? Optional.empty() : node(c.get().superName)) {
            for (var m : c.get().methods) if (m.name.equals(name) && m.desc.equals(desc)) return m;
            for (String itf : c.get().interfaces) { var found = node(itf).map(i -> method(i, name, desc)).orElse(null); if (found != null) return found; }
        }
        return null;
    }

    private FieldNode field(ClassNode type, String name, String desc) {
        for (var c = Optional.of(type); c.isPresent(); c = c.get().superName == null ? Optional.empty() : node(c.get().superName))
            for (var f : c.get().fields) if (f.name.equals(name) && (desc == null || f.desc.equals(desc))) return f;
        return null;
    }

    private static String shadowName(String name, List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        var shadow = find(visible, SHADOW);
        if (shadow == null) shadow = find(invisible, SHADOW);
        String prefix = shadow == null ? null : (String) value(shadow, "prefix");
        if (prefix == null) prefix = "shadow$";
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }

    private Optional<ClassNode> node(String name) {
        return classes.computeIfAbsent(name, n -> {
            try (var in = MixinTargetAuditTest.class.getResourceAsStream("/" + n + ".class")) {
                if (in == null) return Optional.empty();
                var node = new ClassNode();
                new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                return Optional.of(node);
            } catch (IOException e) {
                return Optional.empty();
            }
        });
    }

    private static AnnotationNode find(List<AnnotationNode> annotations, String desc) {
        if (annotations != null) for (var a : annotations) if (a.desc.equals(desc)) return a;
        return null;
    }

    private static Object value(AnnotationNode a, String key) {
        if (a.values != null) for (int i = 0; i < a.values.size(); i += 2) if (a.values.get(i).equals(key)) return a.values.get(i + 1);
        return null;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> l ? l : value == null ? List.of() : List.of(value);
    }

    private static String simple(ClassNode node) {
        return node.name.substring(node.name.lastIndexOf('/') + 1);
    }
}
