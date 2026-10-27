package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.visual.DeathRecap.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import java.nio.file.*;
import java.util.*;

/**
 * Saved Death Recap replays in config/velaclient/replays, one gzip'd NBT file each. Entity state is stored per actor in
 * columns (one array per field) and equipment as indices into a small item palette, so a 10 s replay is tens of KB.
 * A replay plays back in the level the player is in, so it is offered only in the same world and dimension.
 */
public final class ReplayFiles {
    public static final int LIMIT=50;
    private static final int VERSION=1;
    /** Choices made in the save window; {@code secondsBefore} 0 keeps the whole recording. */
    public record Options(String title,DeathReplay.View view,boolean hud,float speed,int secondsBefore){}
    public record Saved(Path file,String title,long recordedAt,Options options,String world,String dimension){}
    private ReplayFiles(){}

    /** Visible folder next to saves and screenshots; replays saved by earlier builds in the config folder move here once. */
    public static Path dir(){
        var game=net.fabricmc.loader.api.FabricLoader.getInstance();
        Path dir=game.getGameDir().resolve("vela-replays"),old=game.getConfigDir().resolve("velaclient/replays");
        if(Files.isDirectory(old))try(var files=Files.list(old)){
            Files.createDirectories(dir);
            for(var f:(Iterable<Path>)files::iterator)Files.move(f,dir.resolve(f.getFileName()),StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(old);
        }catch(java.io.IOException ignored){}
        return dir;
    }

    public static Path save(Recording r,Options options)throws java.io.IOException{
        var mc=Minecraft.getInstance();
        if(options.secondsBefore()>0){
            int from=r.deathTick()-options.secondsBefore()*20;
            var kept=r.frames().stream().filter(f->f.tick()>=from).toList();
            if(kept.size()>=2)r=new Recording(kept,r.actors(),r.events().stream().filter(e->e.tick()>=from).toList(),r.self(),r.killer(),r.killerName(),r.message(),r.deathTick(),r.recordedAt(),r.world(),r.dimension());
        }
        if(mc.level==null)throw new java.io.IOException("No level");
        var ops=mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var root=new CompoundTag();
        root.putInt("version",VERSION);
        root.putString("world",r.world());root.putString("dimension",r.dimension());
        root.putLong("recordedAt",r.recordedAt());root.putInt("deathTick",r.deathTick());
        root.putString("view",options.view().name());root.putBoolean("hud",options.hud());root.putFloat("speed",options.speed());
        root.putString("title",options.title()==null?"":options.title());
        root.putString("message",r.message()==null?"":r.message().getString());
        if(r.killer()!=null)root.putString("killer",r.killer().toString());
        if(r.killerName()!=null)root.putString("killerName",r.killerName());
        root.putString("self",r.self()==null?"":r.self().toString());
        var frames=r.frames();
        int[] ticks=new int[frames.size()];float[] health=new float[ticks.length],absorption=new float[ticks.length],max=new float[ticks.length];int[] armor=new int[ticks.length],ping=new int[ticks.length];
        for(int i=0;i<ticks.length;i++){var f=frames.get(i);ticks[i]=f.tick();health[i]=f.health();absorption[i]=f.absorption();max[i]=f.maxHealth();armor[i]=f.armor();ping[i]=f.ping();}
        root.putIntArray("ticks",ticks);root.put("health",floats(health));root.put("absorption",floats(absorption));root.put("maxHealth",floats(max));root.putIntArray("armor",armor);root.putIntArray("ping",ping);
        // Item palette shared by all actors.
        var palette=new ArrayList<ItemStack>();var paletteTags=new ListTag();
        var actors=new ListTag();
        for(var actor:r.actors().values()){
            var a=new CompoundTag();
            a.putString("uuid",actor.uuid().toString());a.putString("type",BuiltInRegistries.ENTITY_TYPE.getKey(actor.type()).toString());
            a.putString("name",actor.name());a.putBoolean("self",actor.self());a.putBoolean("player",actor.player());a.putFloat("eye",actor.eyeHeight());
            var present=new ArrayList<Integer>();
            for(int i=0;i<frames.size();i++)if(frames.get(i).snaps().containsKey(actor.uuid()))present.add(i);
            int n=present.size();
            double[] x=new double[n],y=new double[n],z=new double[n];float[] yRot=new float[n],xRot=new float[n],head=new float[n],body=new float[n],walkPos=new float[n],walkSpeed=new float[n],attack=new float[n];
            int[] index=new int[n],pose=new int[n],flags=new int[n],swing=new int[n],hurt=new int[n],death=new int[n],gear=new int[n*6];
            for(int k=0;k<n;k++){
                var s=frames.get(present.get(k)).snaps().get(actor.uuid());
                index[k]=present.get(k);x[k]=s.x();y[k]=s.y();z[k]=s.z();yRot[k]=s.yRot();xRot[k]=s.xRot();head[k]=s.headYaw();body[k]=s.bodyYaw();
                walkPos[k]=s.walkPosition();walkSpeed[k]=s.walkSpeed();attack[k]=s.attackAnim();pose[k]=s.pose().ordinal();
                flags[k]=(s.swinging()?1:0)|(s.arm()==InteractionHand.OFF_HAND?2:0);swing[k]=s.swingTime();hurt[k]=s.hurtTime();death[k]=s.deathTime();
                for(int g=0;g<6;g++)gear[k*6+g]=paletteIndex(palette,paletteTags,s.gear()[g],ops);
            }
            a.putIntArray("frames",index);a.put("x",doubles(x));a.put("y",doubles(y));a.put("z",doubles(z));
            a.put("yRot",floats(yRot));a.put("xRot",floats(xRot));a.put("head",floats(head));a.put("body",floats(body));
            a.put("walkPos",floats(walkPos));a.put("walkSpeed",floats(walkSpeed));a.put("attack",floats(attack));
            a.putIntArray("pose",pose);a.putIntArray("flags",flags);a.putIntArray("swing",swing);a.putIntArray("hurt",hurt);a.putIntArray("death",death);a.putIntArray("gear",gear);
            actors.add(a);
        }
        root.put("actors",actors);root.put("items",paletteTags);
        var events=new ListTag();
        for(var e:r.events()){var t=new CompoundTag();t.putInt("tick",e.tick());t.putString("kind",e.kind().name());t.putString("title",e.title());t.putString("detail",e.detail());t.putFloat("amount",e.amount());if(e.source()!=null)t.putString("source",e.source().toString());events.add(t);}
        root.put("events",events);
        Files.createDirectories(dir());
        Path file=dir().resolve(fileName(options.title(),r.recordedAt())+".vrec");
        NbtIo.writeCompressed(root,file);
        prune();
        return file;
    }
    /** "2026-10-27 18-04 Killer name": readable in the folder, safe on every file system. */
    static String fileName(String title,long recordedAt){
        String time=java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss").format(java.time.Instant.ofEpochMilli(recordedAt).atZone(java.time.ZoneId.systemDefault()));
        String clean=title==null?"":title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","").strip();
        if(clean.length()>40)clean=clean.substring(0,40).strip();
        return clean.isEmpty()?time:time+" "+clean;
    }
    private static int paletteIndex(List<ItemStack> palette,ListTag tags,ItemStack stack,com.mojang.serialization.DynamicOps<Tag> ops){
        for(int i=0;i<palette.size();i++)if(palette.get(i)==stack||ItemStack.matches(palette.get(i),stack))return i;
        palette.add(stack);
        tags.add(ItemStack.OPTIONAL_CODEC.encodeStart(ops,stack).result().orElse(new CompoundTag()));
        return palette.size()-1;
    }
    private static ListTag floats(float[] v){var l=new ListTag();for(float f:v)l.add(FloatTag.valueOf(f));return l;}
    private static ListTag doubles(double[] v){var l=new ListTag();for(double d:v)l.add(DoubleTag.valueOf(d));return l;}
    private static void prune(){
        var all=list(null,null);
        for(int i=LIMIT;i<all.size();i++)try{Files.deleteIfExists(all.get(i).file());}catch(java.io.IOException ignored){}
    }

    /** Saved replays, newest first; {@code world}/{@code dimension} null for all. Reads only the small header fields. */
    public static List<Saved> list(String world,String dimension){
        var out=new ArrayList<Saved>();
        if(!Files.isDirectory(dir()))return out;
        try(var files=Files.list(dir())){
            for(var file:(Iterable<Path>)files.filter(f->f.toString().endsWith(".vrec"))::iterator){
                try{
                    var root=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());
                    String w=root.getStringOr("world",""),d=root.getStringOr("dimension","");
                    if(world!=null&&(!w.equals(world)||!d.equals(dimension)))continue;
                    String killer=root.getStringOr("killerName",""),message=root.getStringOr("message","");
                    DeathReplay.View view;try{view=DeathReplay.View.valueOf(root.getStringOr("view","ORBIT"));}catch(IllegalArgumentException bad){view=DeathReplay.View.ORBIT;}
                    String title=root.getStringOr("title","");if(title.isBlank())title=!message.isEmpty()?message:killer;
                    var options=new Options(title,view,root.getBooleanOr("hud",false),root.getFloatOr("speed",.5f),0);
                    out.add(new Saved(file,title,root.getLongOr("recordedAt",0),options,w,d));
                }catch(Exception unreadable){/* skip damaged files */}
            }
        }catch(java.io.IOException ignored){}
        out.sort(Comparator.comparingLong(Saved::recordedAt).reversed());
        return out;
    }
    public static void delete(Saved saved){try{Files.deleteIfExists(saved.file());}catch(java.io.IOException ignored){}}

    public static Recording load(Saved saved)throws java.io.IOException{
        var mc=Minecraft.getInstance();
        if(mc.level==null)throw new java.io.IOException("No level");
        var ops=mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var root=NbtIo.readCompressed(saved.file(),NbtAccounter.unlimitedHeap());
        int[] ticks=root.getIntArray("ticks").orElse(new int[0]);
        float[] health=floats(root,"health"),absorption=floats(root,"absorption"),max=floats(root,"maxHealth");
        int[] armor=root.getIntArray("armor").orElse(new int[ticks.length]),ping=root.getIntArray("ping").orElse(new int[ticks.length]);
        var items=new ArrayList<ItemStack>();
        for(var t:root.getListOrEmpty("items"))items.add(ItemStack.OPTIONAL_CODEC.parse(ops,t).result().orElse(ItemStack.EMPTY));
        List<Map<UUID,Snap>> snaps=new ArrayList<>();for(int i=0;i<ticks.length;i++)snaps.add(new HashMap<>());
        var actors=new HashMap<UUID,Actor>();var poses=Pose.values();
        for(var tag:root.getListOrEmpty("actors")){
            if(!(tag instanceof CompoundTag a))continue;
            var uuid=UUID.fromString(a.getStringOr("uuid",""));
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(a.getStringOr("type","minecraft:pig")));
            actors.put(uuid,new Actor(uuid,type,a.getStringOr("name",""),a.getBooleanOr("self",false),a.getBooleanOr("player",false),null,a.getFloatOr("eye",1.62f)));
            int[] index=a.getIntArray("frames").orElse(new int[0]);
            double[] x=doubles(a,"x"),y=doubles(a,"y"),z=doubles(a,"z");
            float[] yRot=floats(a,"yRot"),xRot=floats(a,"xRot"),head=floats(a,"head"),body=floats(a,"body"),walkPos=floats(a,"walkPos"),walkSpeed=floats(a,"walkSpeed"),attack=floats(a,"attack");
            int[] pose=a.getIntArray("pose").orElse(new int[index.length]),flags=a.getIntArray("flags").orElse(new int[index.length]),swing=a.getIntArray("swing").orElse(new int[index.length]),
                hurt=a.getIntArray("hurt").orElse(new int[index.length]),death=a.getIntArray("death").orElse(new int[index.length]),gear=a.getIntArray("gear").orElse(new int[index.length*6]);
            ItemStack[] last=null;
            for(int k=0;k<index.length;k++){
                var g=new ItemStack[6];boolean same=last!=null;
                for(int s=0;s<6;s++){int i=gear[k*6+s];g[s]=i>=0&&i<items.size()?items.get(i):ItemStack.EMPTY;same&=last!=null&&last[s]==g[s];}
                if(same)g=last;last=g;
                snaps.get(index[k]).put(uuid,new Snap(uuid,x[k],y[k],z[k],yRot[k],xRot[k],head[k],body[k],poses[Math.clamp(pose[k],0,poses.length-1)],
                    walkPos[k],walkSpeed[k],attack[k],(flags[k]&1)!=0,(flags[k]&2)!=0?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,swing[k],hurt[k],death[k],g));
            }
        }
        var frames=new ArrayList<Frame>(ticks.length);
        for(int i=0;i<ticks.length;i++)frames.add(new Frame(ticks[i],health[i],absorption[i],max[i],armor[i],ping[i],Collections.unmodifiableMap(snaps.get(i))));
        var events=new ArrayList<Event>();
        for(var tag:root.getListOrEmpty("events")){
            if(!(tag instanceof CompoundTag t))continue;
            String source=t.getStringOr("source","");
            Kind kind;try{kind=Kind.valueOf(t.getStringOr("kind","HIT"));}catch(IllegalArgumentException bad){continue;}
            events.add(new Event(t.getIntOr("tick",0),kind,t.getStringOr("title",""),t.getStringOr("detail",""),t.getFloatOr("amount",0),source.isEmpty()?null:UUID.fromString(source)));
        }
        String self=root.getStringOr("self",""),killer=root.getStringOr("killer",""),message=root.getStringOr("message","");
        return new Recording(List.copyOf(frames),Map.copyOf(actors),List.copyOf(events),self.isEmpty()?null:UUID.fromString(self),killer.isEmpty()?null:UUID.fromString(killer),
            root.getStringOr("killerName",null),message.isEmpty()?null:Component.literal(message),root.getIntOr("deathTick",0),root.getLongOr("recordedAt",0),saved.world(),saved.dimension());
    }
    private static float[] floats(CompoundTag t,String key){var l=t.getListOrEmpty(key);var out=new float[l.size()];for(int i=0;i<out.length;i++)out[i]=l.getFloatOr(i,0);return out;}
    private static double[] doubles(CompoundTag t,String key){var l=t.getListOrEmpty(key);var out=new double[l.size()];for(int i=0;i<out.length;i++)out[i]=l.getDoubleOr(i,0);return out;}
}
