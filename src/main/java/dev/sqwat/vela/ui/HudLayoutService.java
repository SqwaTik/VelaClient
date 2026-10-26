package dev.sqwat.vela.ui;

import com.google.gson.reflect.TypeToken;
import dev.sqwat.vela.config.JsonFileStore;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudLayoutService {
    private final dev.sqwat.vela.ui.animation.HudMotion hudMotion=new dev.sqwat.vela.ui.animation.HudMotion();
    /** Per-panel animation state: one lookup per frame, no boxed Float/Long churn. */
    private static final class PanelState{float height=Float.NaN,alpha=Float.NaN;long time;}
    private final Map<String,PanelState> panels=new java.util.HashMap<>();
    private PanelState panel(String id){var state=panels.get(id);if(state==null){state=new PanelState();panels.put(id,state);}return state;}
    private Object motionLevel;
    private boolean contentVisible(Minecraft c,String id){
        if(c.player==null)return false;
        if(!id.equals("hotkeys")&&c.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen)return true;
        return switch(id){
            case "inventory"->InventoryHudContent.hasItems(slot->!c.player.getInventory().getItem(slot).isEmpty());
            case "near"->!nearby(c).isEmpty();case "potions"->!c.player.getActiveEffects().isEmpty();
            case "speakers"->!HudTelemetry.speakers().isEmpty();case "cooldowns"->!CooldownTracker.snapshot().isEmpty();
            case "armor"->java.util.Arrays.stream(ARMOR_SLOTS).anyMatch(slot->!c.player.getItemBySlot(slot).isEmpty());
            case "hotkeys"->hotkeyCount()>0;
            case "target"->target!=null&&target.level()==c.level&&target.isAlive()&&System.nanoTime()<=targetUntil;
            default->true;
        };
    }
    private java.util.List<ClientModule> shownHotkeys=java.util.List.of();
    private final KeyStrokesHud keyStrokes=new KeyStrokesHud();
    private boolean keyFlag(String setting){return flag("keystrokes",setting);}
    private int hotkeyCount(){int count=0;for(var m:modules.all())if(m.enabled()&&m.boundKey()>=0&&m.available())count++;return count;}
    private net.minecraft.world.entity.LivingEntity target;
    private long targetUntil,targetFrame,targetHitUntil;
    private float targetHealth;
    private net.minecraft.world.entity.LivingEntity displayedTarget;
    private net.minecraft.world.entity.LivingEntity equipmentEntity;
    private int equipmentTick=Integer.MIN_VALUE;
    private List<net.minecraft.world.item.ItemStack> equipmentCache=List.of();
    private Object nearbyLevel;
    private final NearPresence<net.minecraft.world.entity.player.Player> nearPresence=new NearPresence<>();
    private int nearbyTick=Integer.MIN_VALUE;
    private float nearWidth=112;
    private int nearWidthGoal=112;
    private long nearWidthTime;
    private int nearPanelWidth(Minecraft client){
        nearby(client);
        long now=System.nanoTime();
        var services=dev.sqwat.vela.VelaClient.services();
        boolean immediate=services!=null&&(!services.configs().current().animations||services.configs().current().reducedMotion);
        nearWidth=immediate?nearWidthGoal:dev.sqwat.vela.ui.animation.Motion.approach(nearWidth,nearWidthGoal,12,nearWidthTime==0?0:(now-nearWidthTime)/1e9f);
        nearWidthTime=now;return Math.round(nearWidth);
    }
    private java.util.List<net.minecraft.world.entity.player.Player> nearbyCache=java.util.List.of();
    private final java.util.Map<java.util.UUID,String> nearbyLabels=new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID,String> nearbyDistances=new java.util.HashMap<>();
    public void attacked(net.minecraft.world.entity.LivingEntity entity){
        target=entity;targetUntil=System.nanoTime()+3_000_000_000L;targetHitUntil=targetUntil;
    }
    public net.minecraft.world.entity.LivingEntity visualTarget(Minecraft client){
        return target!=null&&target.level()==client.level&&target.isAlive()&&!target.isRemoved()
            &&!target.isInvisible()&&System.nanoTime()<=targetUntil?target:null;
    }
    public void tickTarget(Minecraft client){
        if(client.player==null||client.level==null){target=null;targetUntil=targetHitUntil=0;arrowMotion.clear();arrowWorld=null;arrowTime=0;return;}
        long now=System.nanoTime();
        if(target!=null&&(target.level()!=client.level||!target.isAlive())){target=null;targetHitUntil=0;}
        if(now<targetHitUntil)return;
        net.minecraft.world.entity.player.Player nearest=null;
        double nearestDistance=9;
        for(var player:client.level.players()){
            if(player==client.player||!player.isAlive())continue;
            double distance=client.player.distanceToSqr(player);
            if(distance<=nearestDistance){nearestDistance=distance;nearest=player;}
        }
        if(nearest!=null){
            target=nearest;targetUntil=now+3_000_000_000L;
        }
    }
    private static final List<String> VANILLA_IDS = List.of("bossbar", "scoreboard", "tablist","chat","hotbar");
    private final JsonFileStore<dev.sqwat.vela.config.HudConfig> store;
    private final dev.sqwat.vela.config.HudConfig hudConfig;
    private final ModuleRegistry modules;
    private final Map<String, HudPosition> positions;
    private final Map<String,HudPosition> defaultPositions;
    private final Map<String,Float> defaultScales;
    private boolean editing;
    private final Map<String,dev.sqwat.vela.ui.render.Rect> nativeBounds=new LinkedHashMap<>();
    private final Map<String,dev.sqwat.vela.ui.render.Rect> nativePreviews=new LinkedHashMap<>();
    public synchronized void previewNative(String id,dev.sqwat.vela.ui.render.Rect rect){nativePreviews.put(id,rect);}
    private final java.util.Set<String> nativeVisible=new java.util.HashSet<>();
    private final Map<String,Float> scales;
    public synchronized void recordNative(String id,dev.sqwat.vela.ui.render.Rect rect){if(rect==null){nativeVisible.remove(id);if(!editing)nativeBounds.remove(id);}else {nativeBounds.put(id,rect);nativeVisible.add(id);}}
    public synchronized dev.sqwat.vela.ui.render.Rect nativeOrigin(String id){return nativeBounds.containsKey(id)?nativeBounds.get(id):editing?nativePreviews.get(id):null;}
    public synchronized boolean nativeVisible(String id){return nativeVisible.contains(id);}
    public synchronized void seedNative(String id,dev.sqwat.vela.ui.render.Rect rect){nativeBounds.putIfAbsent(id,rect);}
    public static boolean isNative(String id){return VANILLA_IDS.contains(id);}
    public synchronized float scale(String id){Float s=scales.get(id);return s==null||!Float.isFinite(s)?(isNative(id)?1f:.75f):Math.max(.5f,Math.min(1.25f,s));}
    public synchronized void setScale(String id,float value){scales.put(id,Math.max(.5f,Math.min(1.25f,value)));save();}


    public HudLayoutService(Path root, ModuleRegistry modules) {
        this.modules = modules;
        store=new JsonFileStore<>(root.resolve("hud.vela"),dev.sqwat.vela.config.HudConfig.class,dev.sqwat.vela.config.HudConfig::new);
        boolean migrate=!java.nio.file.Files.exists(root.resolve("hud.vela"));
        hudConfig=store.load();
        if(migrate){
            hudConfig.scales=new JsonFileStore<Map<String,Float>>(root.resolve("hud-scale.json"),new TypeToken<Map<String,Float>>(){}.getType(),LinkedHashMap::new).load();
            hudConfig.positions=new JsonFileStore<Map<String,HudPosition>>(root.resolve("hud-layout.json"),new TypeToken<Map<String,HudPosition>>(){}.getType(),LinkedHashMap::new).load();
        }
        scales=hudConfig.scales==null?new LinkedHashMap<>():hudConfig.scales;
        positions=hudConfig.positions==null?new LinkedHashMap<>():hudConfig.positions;
        positions.putIfAbsent("coordinates", new HudPosition(10, 10));
        positions.putIfAbsent("armor", new HudPosition(10, 28));
        positions.putIfAbsent("watermark",new HudPosition(10,48));
        positions.putIfAbsent("near",new HudPosition(10,75));
        positions.putIfAbsent("inventory",new HudPosition(10,155));
        positions.putIfAbsent("target",new HudPosition(210,155));
        positions.putIfAbsent("potions",new HudPosition(160,75));
        positions.putIfAbsent("speakers",new HudPosition(320,75));
        positions.putIfAbsent("cooldowns",new HudPosition(320,155));
        positions.putIfAbsent("keystrokes",new HudPosition(10,240));
        var defaults=new JsonFileStore<Map<String,HudPosition>>(root.resolve("hud-default-layout.json"),new TypeToken<Map<String,HudPosition>>(){}.getType(),()->new LinkedHashMap<>(positions));
        var sizeDefaults=new JsonFileStore<Map<String,Float>>(root.resolve("hud-default-scale.json"),new TypeToken<Map<String,Float>>(){}.getType(),()->new LinkedHashMap<>(scales));
        defaultPositions=migrate?new LinkedHashMap<>(defaults.load()):hudConfig.defaultPositions==null?new LinkedHashMap<>(positions):hudConfig.defaultPositions;
        defaultScales=migrate?new LinkedHashMap<>(sizeDefaults.load()):hudConfig.defaultScales==null?new LinkedHashMap<>(scales):hudConfig.defaultScales;
        hudConfig.positions=positions;hudConfig.scales=scales;hudConfig.defaultPositions=defaultPositions;hudConfig.defaultScales=defaultScales;
        try{
            store.save(hudConfig);
            if(migrate){
                Path backup=root.resolve("legacy-hud-backup");
                for(String file:List.of("hud-layout.json","hud-scale.json","hud-default-layout.json","hud-default-scale.json")){
                    Path source=root.resolve(file),destination=backup.resolve(file);
                    if(java.nio.file.Files.exists(source)&&!java.nio.file.Files.exists(destination)){
                        java.nio.file.Files.createDirectories(backup);java.nio.file.Files.move(source,destination);
                    }
                }
            }
        }catch(IOException error){org.slf4j.LoggerFactory.getLogger("Vela").warn("HUD settings could not be saved or archived",error);Notifications.show("HUD не сохранён","Проверьте доступ к папке клиента","save");}
    }

    public synchronized List<String> enabledIds() {
        return modules.all().stream().filter(module->module.category()==dev.sqwat.vela.module.ModuleCategory.HUD&&module.enabled()&&module.available()).map(ClientModule::id).toList();
    }

    public synchronized List<String> editableIds() {
        var result = new java.util.ArrayList<>(enabledIds());
        if(editing&&!result.contains("notifications"))result.add("notifications");
        result.addAll(nativeBounds.keySet());
        if(editing)for(String id:nativePreviews.keySet())if(!result.contains(id))result.add(id);
        return List.copyOf(result);
    }

    public synchronized HudPosition position(String id) { return positions.getOrDefault(id, new HudPosition(10, 10)); }
    private float viewportFactor=1;
    public synchronized void viewport(int gameScale){float factor=2f/Math.max(1,gameScale);if(viewportFactor!=factor){nativeBounds.clear();nativeVisible.clear();nativePreviews.clear();}viewportFactor=factor;}
    private float renderScale(String id){return scale(id)*(isNative(id)?1:viewportFactor);}

    public synchronized HudPosition position(String id, int screenWidth, int screenHeight) {
        var stored=positions.get(id);if(stored==null)return defaultPosition(id,screenWidth,screenHeight);
        if(id.equals("notifications")&&!editing)return new HudPosition(
            Math.round(stored.x()*viewportFactor+HudGeometry.notificationOffset(notificationWidth())*renderScale(id)),
            Math.round(stored.y()*viewportFactor+(24-notificationHeight())*renderScale(id)/2));
        return new HudPosition(Math.round(stored.x()*viewportFactor),Math.round(stored.y()*viewportFactor));
    }

    public synchronized HudPosition translation(String id, int screenWidth, int screenHeight) {
        HudPosition actual = position(id, screenWidth, screenHeight);
        HudPosition origin = defaultPosition(id, screenWidth, screenHeight);
        return new HudPosition(actual.x - origin.x, actual.y - origin.y);
    }

    public synchronized void move(String id, int x, int y, int screenWidth, int screenHeight) {
        int elementWidth = id.equals("bossbar") ? 190 : id.equals("scoreboard") ? 132 : 72;
        int elementHeight = id.equals("scoreboard") ? 72 : id.equals("bossbar") ? 26 : 22;
        move(id, x, y, screenWidth, screenHeight, new RectSize(elementWidth, elementHeight));
    }

    public synchronized void move(String id, int x, int y, int screenWidth, int screenHeight, RectSize size) {
        if(isNative(id)){var origin=defaultPosition(id,screenWidth,screenHeight);if(Math.abs(x-origin.x())<=12&&Math.abs(y-origin.y())<=12){positions.remove(id);return;}}
        int elementWidth = size.width();
        int elementHeight = size.height();
        int edge = VANILLA_IDS.contains(id) ? 2 : 0;
        positions.put(id, new HudPosition(Math.round(Math.max(0, Math.min(screenWidth - elementWidth - edge, x))/viewportFactor), Math.round(Math.max(0, Math.min(screenHeight - elementHeight - edge, y))/viewportFactor)));
    }

    public synchronized void reset() {
        positions.clear();positions.putAll(defaultPositions);scales.clear();scales.putAll(defaultScales);
        VANILLA_IDS.forEach(positions::remove);VANILLA_IDS.forEach(scales::remove);
        save();
    }

    public synchronized void save() {
        try { store.save(hudConfig); } catch (IOException error) { dev.sqwat.vela.ui.Notifications.show("HUD не сохранён","Проверьте доступ к папке клиента","save"); }
    }

    public synchronized void setEditing(boolean editing) { this.editing = editing; }

    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        if (editing) return;
        Minecraft client = Minecraft.getInstance();
        if(!HudVisibility.allowed(client)){hudMotion.clear();return;}
        viewport(client.getWindow().getGuiScale());
        if(modules.byId("jump_circle") instanceof dev.sqwat.vela.module.JumpCircle jump&&jump.enabled())jump.draw(g);
        if(modules.byId("hit_bubbles") instanceof dev.sqwat.vela.module.HitBubbles bubbles&&bubbles.enabled())bubbles.draw(g);
        if(modules.byId("hp_counter") instanceof dev.sqwat.vela.module.HpCounter counter&&counter.enabled())counter.draw(g);
        if(modules.byId("explosion_wave") instanceof dev.sqwat.vela.module.ExplosionWave wave&&wave.enabled())wave.draw(g);
        if(modules.byId("target_esp") instanceof dev.sqwat.vela.module.TargetESP target&&target.enabled())target.draw(g);
        if(modules.byId("dash_trail") instanceof dev.sqwat.vela.module.DashTrail trail&&trail.enabled())trail.draw(g);
        if(modules.byId("kill_effect") instanceof dev.sqwat.vela.module.KillEffect kill&&kill.enabled())kill.draw(g);
        if(modules.byId("particles") instanceof dev.sqwat.vela.module.Particles particles&&particles.enabled())particles.draw(g);
        if(modules.byId("world_particles") instanceof dev.sqwat.vela.module.WorldParticles ambient&&ambient.enabled())ambient.draw(g);
        if(modules.byId("trajectories") instanceof dev.sqwat.vela.module.Trajectories trajectories&&trajectories.enabled())trajectories.draw(g);
        if(modules.byId("pinger") instanceof dev.sqwat.vela.module.Pinger pinger&&pinger.screenWarningActive()){
            var config=dev.sqwat.vela.VelaClient.services().configs().current();
            int edge=Math.max(16,g.guiHeight()/6),alpha=Math.round(255*pinger.warningOpacity(System.nanoTime(),config.reducedMotion||!config.animations)),color=(alpha<<24)|0xC51028;
            g.fillGradient(0,0,g.guiWidth(),edge,color,0x00FF2448);
            g.fillGradient(0,g.guiHeight()-edge,g.guiWidth(),g.guiHeight(),0x00FF2448,color);
            var canvas=new dev.sqwat.vela.ui.render.GameUiCanvas(g);
            int side=Math.max(16,g.guiWidth()/9);
            for(int i=0;i<24;i++){
                float fade=1-i/24f;int tint=(Math.round(alpha*fade*fade)<<24)|0xC51028;
                float band=side/24f;
                canvas.fill(i*band,0,band,g.guiHeight(),tint);canvas.fill(g.guiWidth()-(i+1)*band,0,band,g.guiHeight(),tint);
            }
        }
        if(Notifications.hudVisible()&&client.gui.overlay()==null){
            HudPosition pos=position("notifications",g.guiWidth(),g.guiHeight());
            drawChip(g,client,"notifications",pos.x(),pos.y());
        }
        if(modules.byId("keystrokes") instanceof ClientModule strokes&&strokes.enabled())keyStrokes.sample(client);
        if(motionLevel!=client.level){hudMotion.clear();for(var state:panels.values())state.height=Float.NaN;motionLevel=client.level;}
        // Rendering runs much more often than ticks: avoid constructing a stream/list every frame.
        for (ClientModule module : modules.all()) {
            String id=module.id();
            if(module.category()!=dev.sqwat.vela.module.ModuleCategory.HUD||VANILLA_IDS.contains(id))continue;
            if(id.equals("near")&&nearArrows()){
                if(module.enabled()&&module.available())drawNearArrows(g,client,deltaTracker.getGameTimeDeltaPartialTick(false));
                continue;
            }
            boolean visible=module.enabled()&&module.available()&&contentVisible(client,id);
            if(hudMotion.skipHidden(id,visible))continue;
            var size=bounds(client,id);
            var service=dev.sqwat.vela.VelaClient.services();boolean immediate=service!=null&&(!service.configs().current().animations||service.configs().current().reducedMotion);
            var frame=hudMotion.update(id,visible,size.height()/renderScale(id),System.nanoTime(),immediate);
            float reveal=frame.reveal();panel(id).height=frame.height();
            if(reveal<.005f)continue;
            HudPosition position = position(id,g.guiWidth(),g.guiHeight());
            dev.sqwat.vela.ui.render.UiClip.enable(g,position.x()-1,position.y()-1,position.x()+size.width()+2,position.y()+(int)Math.ceil(frame.height()*renderScale(id)*reveal)+2);
            try{drawChip(g,client,id,position.x(),position.y());}finally{g.disableScissor();}
        }
    }

    public void drawChip(GuiGraphicsExtractor g,Minecraft client,String id,int x,int y){
            float scale=renderScale(id);
            g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale);
            try {
            int panel=panelColor(id);
            if(id.equals("notifications")){if(editing){drawPanel(g,id,128,24,panel);dev.sqwat.vela.ui.render.ReloadNotice.icon(g,7,6,VelaTheme.TEXT_SECONDARY);VelaDraw.text(g,"Notifications",23,7,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);}else if(client.gui.overlay()==null&&Notifications.hudVisible())Notifications.drawHud(g);else dev.sqwat.vela.ui.render.ReloadNotice.draw(g);return;}
            if(id.equals("hotkeys")){
                var active=modules.all().stream().filter(m->m.enabled()&&m.boundKey()>=0&&m.available()).toList();
                shownHotkeys=active;
                int visibleHeight=24+shownHotkeys.size()*21;
                drawPanel(g,id,160,visibleHeight,panel);hudHeading(g,"key","Hotkeys");
                for(int i=0;i<shownHotkeys.size();i++){var m=shownHotkeys.get(i);VelaDraw.text(g,VelaDraw.ellipsize(m.name(),100,VelaDraw.Weight.REGULAR,8),8,29+i*21,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);String key=KeyLabels.of(m.boundKey(),m.keybind());VelaDraw.roundedRect(g,116,25+i*21,36,17,4,VelaTheme.SURFACE_DEEP);VelaDraw.centeredText(g,VelaDraw.ellipsize(key,32,VelaDraw.Weight.REGULAR,8),134,29+i*21,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);}return;
            }
            boolean preview=editing||client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen;
            if(id.equals("speakers")){
                var voices=HudTelemetry.speakers();
                drawPanel(g,id,152,24+voices.size()*25,panel);
                hudHeading(g,"speakers","Speakers");
                for(int i=0;i<voices.size();i++){
                    var voice=voices.get(i);var info=client.getConnection()==null?null:client.getConnection().getPlayerInfo(voice.id());
                    String name=info==null?"Игрок":info.getProfile().name();int sy=26+i*25;
                    VelaDraw.text(g,VelaDraw.ellipsize(name,134,VelaDraw.Weight.REGULAR,8),8,sy,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
                    VelaDraw.roundedRect(g,8,sy+13,136,3,1,VelaTheme.BORDER);
                    VelaDraw.roundedRect(g,8,sy+13,Math.round(136*voice.level()),3,1,VelaTheme.PRIMARY);
                }return;
            }
            if(id.equals("cooldowns")){
                var entries=CooldownTracker.snapshot().entrySet().stream().toList();
                drawPanel(g,id,160,24+entries.size()*21,panel);
                hudHeading(g,"cooldowns","Cooldowns");
                for(int i=0;i<entries.size();i++){
                    var entry=entries.get(i);int sy=25+i*21;net.minecraft.world.item.ItemStack stack=net.minecraft.world.item.ItemStack.EMPTY;
                    if(client.player!=null)for(int slot=0;slot<client.player.getInventory().getContainerSize();slot++){
                        var candidate=client.player.getInventory().getItem(slot);
                        if(!candidate.isEmpty()&&client.player.getCooldowns().getCooldownGroup(candidate).equals(entry.getKey())){stack=candidate;break;}
                    }
                    if(!stack.isEmpty())g.item(stack,5,sy);
                    String name=stack.isEmpty()?entry.getKey().getPath():stack.getHoverName().getString();
                    VelaDraw.text(g,VelaDraw.ellipsize(name,94,VelaDraw.Weight.REGULAR,8),26,sy+4,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
                    VelaDraw.text(g,String.format(java.util.Locale.ROOT,"%.1fs",entry.getValue()/20f),128,sy+4,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7);
                }return;
            }
            if(id.equals("target")){
                var entity=preview?client.player:target;
                long now=System.nanoTime();float dt=targetFrame==0?0:Math.min(.1f,(now-targetFrame)/1e9f);targetFrame=now;
                boolean visible=entity!=null&&(preview||(now<=targetUntil&&entity.level()==client.level&&entity.isAlive()));
                if(visible&&displayedTarget!=entity){displayedTarget=entity;targetHealth=entity.getHealth();}
                if(displayedTarget==null)return;
                entity=displayedTarget;
                if(entity.level()!=client.level)return;
                // Reveal by clipping; item models keep a stable pixel size.
                targetHealth=dev.sqwat.vela.ui.animation.Motion.approach(targetHealth,entity.getHealth(),9,dt);
                var equipment=targetEquipment(entity);
                int targetHeight=equipment.isEmpty()?46:78;
                drawPanel(g,id,142,targetHeight,panel);
                if(entity instanceof net.minecraft.client.player.AbstractClientPlayer player)
                    net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(g,player.getSkin(),8,8,28);
                else dev.sqwat.vela.ui.render.MobPortrait.draw(g,entity,8,8,28);
                if(entity.hurtTime>0)VelaDraw.roundedRect(g,8,8,28,28,2,((int)(85*entity.hurtTime/10f)<<24)|0xFF3030);
                VelaDraw.text(g,VelaDraw.ellipsize(entity.getName().getString(),90,VelaDraw.Weight.MEDIUM,9),44,9,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);
                VelaDraw.text(g,Math.round(targetHealth)+" HP",44,24,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
                VelaDraw.roundedRect(g,44,36,90,3,1,VelaTheme.BORDER);
                VelaDraw.roundedRect(g,44,36,Math.round(90*Math.max(0,Math.min(1,targetHealth/Math.max(1,entity.getMaxHealth())))),3,1,VelaTheme.PRIMARY);
                int equipmentX=(142-equipment.size()*20+4)/2;
                for(int i=0;i<equipment.size();i++){
                    VelaDraw.roundedRect(g,equipmentX+i*20-1,47,18,18,3,0xB00B0F15);
                    g.item(equipment.get(i),equipmentX+i*20,48);
                    g.itemDecorations(client.font,equipment.get(i),equipmentX+i*20,48);
                    var stack=equipment.get(i);
                    if(stack.isDamageableItem()&&flag("target","equipment_percent")){
                        int percent=Math.max(0,Math.round(100f*(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage()));
                        VelaDraw.centeredText(g,percent+"%",equipmentX+i*20+8,68,percent<=20?0xFFFF596D:VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,6);
                    }
                }
                return;
            }
            if(id.equals("potions")){
                if(client.player==null)return;
                var effects=client.player.getActiveEffects().stream().toList();
                drawPanel(g,id,160,24+effects.size()*21,panel);
                hudHeading(g,"potions","Potions");
                for(int i=0;i<effects.size();i++){var effect=effects.get(i);int sy=25+i*21;
                    g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,net.minecraft.client.gui.Hud.getMobEffectSprite(effect.getEffect()),5,sy,18,18);
                    String level=EffectLevels.suffix(effect.getAmplifier());
                    VelaDraw.text(g,VelaDraw.ellipsize(effect.getEffect().value().getDisplayName().getString(),90-VelaDraw.textWidth(level,VelaDraw.Weight.REGULAR,8),VelaDraw.Weight.REGULAR,8)+level,26,sy+4,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
                    int seconds=(int)Math.ceil(effect.getDuration()/20.0);
                    VelaDraw.text(g,effect.isInfiniteDuration()?"∞":String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60),128,sy+4,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7);
                }return;
            }
            if(id.equals("inventory")){
                drawPanel(g,id,184,84,panel);hudHeading(g,"inventory","Inventory");g.pose().translate(0,20);
                for(int row=0;row<3;row++)for(int col=0;col<9;col++){
                    int sx=4+col*20,sy=4+row*19;
                    VelaDraw.roundedRect(g,sx,sy,17,17,3,(panel&0xFF000000)|0x1C222C);
                    if(client.player!=null){var stack=client.player.getInventory().getItem(9+row*9+col);g.item(stack,sx,sy);g.itemDecorations(client.font,stack,sx,sy);}
                }
                return;
            }
            if(id.equals("near")){
                var players=nearby(client);
                int width=nearPanelWidth(client);
                drawPanel(g,id,width,24+players.size()*21,panel);
                dev.sqwat.vela.ui.render.VelaIcons.draw(g,"friends",7,6,10,VelaTheme.TEXT_SECONDARY);
                VelaDraw.text(g,"Near",23,7,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
                for(int i=0;i<players.size();i++){var player=players.get(i);int sy=25+i*21;
                    PlayerAvatars.draw(g,player.getName().getString(),6,sy,16);
                    String label=nearbyLabels.getOrDefault(player.getUUID(),player.getGameProfile().name());
                    boolean friend=dev.sqwat.vela.VelaClient.services().friends().contains(player.getGameProfile().name());
                    VelaDraw.text(g,VelaDraw.ellipsize(label,width-76,VelaDraw.Weight.REGULAR,8),27,sy+4,friend?VelaTheme.POSITIVE:VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
                    VelaDraw.text(g,Math.round(client.player.distanceTo(player))+"m",width-46,sy+4,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,7);
                    g.pose().pushMatrix();g.pose().translate(width-11,sy+8);
                    g.pose().rotate(NearDirection.radians(player.getX()-client.player.getX(),player.getZ()-client.player.getZ(),client.player.getYRot()));
                    dev.sqwat.vela.ui.render.VelaIcons.draw(g,"near_arrow",-5,-5,10,VelaTheme.PRIMARY);
                    g.pose().popMatrix();
                }return;
            }
            if(id.equals("watermark")){
                watermark(client);String[] parts=watermarkParts;int w=watermarkWidth;
                drawPanel(g,id,w,22,panel);VelaDraw.logo(g,5,4,14);
                dev.sqwat.vela.ui.render.GameUiCanvas canvas=parts.length>1?new dev.sqwat.vela.ui.render.GameUiCanvas(g):null;
                for(int i=0;i<parts.length;i++){
                    int tx=watermarkTextX[i];
                    if(i>0)dev.sqwat.vela.ui.render.MetricIcon.draw(canvas,parts[i],tx-13,6,VelaTheme.TEXT_SECONDARY);
                    VelaDraw.text(g,client.font,watermarkLabels[i],tx,7,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);
                }return;
            }
            if(id.equals("keystrokes")){
                boolean mouse=keyFlag("mouse"),space=keyFlag("space");
                drawPanel(g,id,KeyStrokesHud.WIDTH,KeyStrokesHud.height(mouse,space),panel);
                var config=dev.sqwat.vela.VelaClient.services().configs().current();
                keyStrokes.draw(g,client,mouse,space,keyFlag("cps"),!config.animations||config.reducedMotion);
                return;
            }
            if(id.equals("armor")){
                if(client.player==null)return;
                ClientModule armor=modules.byId(id);
                boolean bg=armor.settings().stream().filter(s->s.id().equals("background")).noneMatch(s->s instanceof dev.sqwat.vela.module.setting.BooleanSetting b&&!b.value());
                drawPanel(g,id,80,24,panel);
                var slots=List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET);
                for(int i=0;i<slots.size();i++){
                    var stack=client.player==null?net.minecraft.world.item.ItemStack.EMPTY:client.player.getItemBySlot(slots.get(i));
                    if(!stack.isEmpty()){g.item(stack,3+i*19,3);g.itemDecorations(client.font,stack,3+i*19,3);}
                }
                return;
            }

            String text = label(client, id);
            int width = labelWidth(text) + 16;
            drawPanel(g,id,width,22,panel);
            VelaDraw.text(g, client.font, text,8,7,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);
            } finally {g.pose().popMatrix();}
    }

    public String label(Minecraft client, String id) {
        return switch (id) {
            case "coordinates" -> client.player == null ? "XYZ — — —" : coordinateLabel(client);
            case "armor" -> armorLabel(client);
            case "watermark" -> watermark(client);
            case "near" -> "Near";
            case "inventory" -> "Inventory";
            case "bossbar" -> "Boss Bar";
            case "scoreboard" -> "Scoreboard";
            case "tablist" -> "Player List";
            default -> id;
        };
    }

    public String functionName(String id) {
        var module=modules.byId(id);
        if(module!=null)return module.name();
        return switch(id){case "notifications"->"Notifications";case "hotbar"->"Hotbar";case "bossbar"->"Boss Bar";case "scoreboard"->"Scoreboard";case "tablist"->"Player List";case "chat"->"Chat";default->id;};
    }

    public RectSize bounds(Minecraft client, String id) {
        RectSize raw=unscaledBounds(client,id);return isNative(id)?raw:new RectSize(Math.round(raw.width()*viewportFactor),Math.round(raw.height()*viewportFactor));
    }
    private RectSize unscaledBounds(Minecraft client,String id){
        var actual=nativeBounds.get(id);
        if(actual!=null)return new RectSize(Math.round(actual.width()*scale(id)),Math.round(actual.height()*scale(id)));
        return switch (id) {
            case "notifications" -> new RectSize(Math.round(notificationWidth()*scale(id)),Math.round(notificationHeight()*scale(id)));
            case "watermark" -> {watermark(client);yield new RectSize(Math.round(watermarkWidth*scale(id)),Math.round(22*scale(id)));}
            case "bossbar" -> new RectSize(190, 26);
            case "armor" -> new RectSize(Math.round(80*scale(id)),Math.round(24*scale(id)));
            case "inventory" -> new RectSize(Math.round(184*scale(id)),Math.round(84*scale(id)));
            case "near" -> new RectSize(Math.round(nearPanelWidth(client)*scale(id)),Math.round((24+nearby(client).size()*21)*scale(id)));
            case "target" -> new RectSize(Math.round(142*scale(id)),Math.round((targetEquipment(editing||client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen?client.player:target).isEmpty()?46:78)*scale(id)));
            case "potions" -> new RectSize(Math.round(160*scale(id)),Math.round((24+(client.player==null?0:client.player.getActiveEffects().size())*21)*scale(id)));
            case "speakers" -> new RectSize(Math.round(152*scale(id)),Math.round((24+HudTelemetry.speakers().size()*25)*scale(id)));
            case "cooldowns" -> new RectSize(Math.round(160*scale(id)),Math.round((24+CooldownTracker.snapshot().size()*21)*scale(id)));
            case "hotkeys" -> new RectSize(Math.round(160*scale(id)),Math.round((24+hotkeyCount()*21)*scale(id)));
            case "scoreboard" -> new RectSize(132, 72);
            case "keystrokes" -> new RectSize(Math.round(KeyStrokesHud.WIDTH*scale(id)),Math.round(KeyStrokesHud.height(keyFlag("mouse"),keyFlag("space"))*scale(id)));
            default -> new RectSize(Math.round((labelWidth(label(client, id)) + 16)*scale(id)), Math.round(22*scale(id)));
        };
    }

    private HudPosition defaultPosition(String id, int screenWidth, int screenHeight) {
        var actual=nativeBounds.get(id);if(actual!=null)return new HudPosition(actual.x(),actual.y());
        return switch (id) {
            case "notifications" -> new HudPosition(Math.round((screenWidth-notificationWidth()*renderScale(id))/2),Math.round((screenHeight-notificationHeight()*renderScale(id))/2));
            case "bossbar" -> new HudPosition(screenWidth / 2 - 91, 12);
            case "scoreboard" -> new HudPosition(screenWidth - 132, screenHeight / 2 - 36);
            default -> new HudPosition(10, 10);
        };
    }

    private int notificationWidth(){return editing?128:Notifications.hudWidth();}
    private int notificationHeight(){return !editing&&Notifications.hudVisible()?32:24;}

    private String coordinateText="";
    private int coordinateX,coordinateY,coordinateZ;
    private boolean coordinateCompact;
    private String coordinateLabel(Minecraft client) {
        var module = modules.byId("coordinates");
        boolean compact=false;
        if(module!=null)for(var s:module.settings())if(s.id().equals("format")&&"Compact".equals(s.value())){compact=true;break;}
        int x=client.player.getBlockX(),y=client.player.getBlockY(),z=client.player.getBlockZ();
        if(coordinateText.isEmpty()||x!=coordinateX||y!=coordinateY||z!=coordinateZ||compact!=coordinateCompact){
            coordinateX=x;coordinateY=y;coordinateZ=z;coordinateCompact=compact;
            coordinateText=(compact ? "" : "XYZ ") + x + " " + y + " " + z;
        }
        return coordinateText;
    }
    // Measured twice per frame (bounds + draw) for text chips; the label rarely changes between frames.
    private final Map<String,Integer> labelWidths=new java.util.HashMap<>();
    private Object labelMask;
    private int labelWidth(String text){
        Object mask=dev.sqwat.vela.module.StreamerMode.mask();
        if(mask!=labelMask||labelWidths.size()>64){labelWidths.clear();labelMask=mask;}
        Integer cached=labelWidths.get(text);
        if(cached!=null)return cached;
        int width=VelaDraw.textWidth(text,VelaDraw.Weight.MEDIUM);labelWidths.put(text,width);return width;
    }

    private String armorLabel(Minecraft client) {
        if (client.player == null) return "Armor —";
        int remaining = 0, maximum = 0;
        for (var slot : java.util.List.of(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET)) {
            var stack = client.player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) { maximum += stack.getMaxDamage(); remaining += stack.getMaxDamage() - stack.getDamageValue(); }
        }
        return maximum == 0 ? "Armor —" : "Armor " + Math.round(remaining * 100f / maximum) + "%";
    }
    private boolean flag(String id,String setting){var module=modules.byId(id);if(module==null)return false;for(var option:module.settings())if(option.id().equals(setting))return Boolean.TRUE.equals(option.value());return false;}
    private boolean nearArrows(){
        var module=modules.byId("near");
        return module!=null&&module.settings().stream().anyMatch(s->s.id().equals("mode")&&s.value().equals("Стрелки"));
    }
    private final Map<java.util.UUID,float[]> arrowMotion=new java.util.HashMap<>();
    private final float[] arrowAngles=new float[24];
    private final ArrowLayout arrowLayout=new ArrowLayout(24);
    private float arrowRadius=64;
    private long arrowTime;
    private Object arrowWorld;
    private int arrowCacheTick=Integer.MIN_VALUE;
    private final Map<java.util.UUID,String> arrowLabels=new java.util.HashMap<>();
    private final Map<java.util.UUID,String> arrowCaptions=new java.util.HashMap<>();
    private int arrowCaptionFlags=-1;
    private static final net.minecraft.resources.Identifier NEAR_ARROW=net.minecraft.resources.Identifier.fromNamespaceAndPath("velaclient","textures/effects/near_arrow.png");
    private void drawNearArrows(GuiGraphicsExtractor g,Minecraft client,float partial){
        if(client.player==null)return;
        int rgb=VelaTheme.PRIMARY&0xFFFFFF;
        if(!flag("near","client_color"))for(var option:modules.byId("near").settings())
            if(option.id().equals("color"))rgb=(Integer)option.value()&0xFFFFFF;
        float density=renderScale("near");
        boolean names=flag("near","names"),distance=flag("near","distance");
        var camera=client.gameRenderer.mainCamera();var origin=camera.position();
        var players=nearby(client);
        if(arrowWorld!=client.level){arrowMotion.clear();arrowLabels.clear();arrowCaptions.clear();arrowCacheTick=Integer.MIN_VALUE;arrowWorld=client.level;arrowTime=0;}
        long now=System.nanoTime();float dt=arrowTime==0?1/60f:Math.min(.1f,(now-arrowTime)/1e9f);arrowTime=now;
        boolean reduced=dev.sqwat.vela.VelaClient.services().configs().current().reducedMotion||!dev.sqwat.vela.VelaClient.services().configs().current().animations;
        int captionFlags=(names?1:0)|(distance?2:0);
        if(arrowCacheTick!=nearbyTick||arrowCaptionFlags!=captionFlags){
            arrowCaptionFlags=captionFlags;
            arrowCacheTick=nearbyTick;var ids=new java.util.HashSet<java.util.UUID>();
            for(var p:players){ids.add(p.getUUID());String name=nearbyLabels.getOrDefault(p.getUUID(),p.getGameProfile().name());arrowLabels.put(p.getUUID(),VelaDraw.ellipsize(name,64,VelaDraw.Weight.MEDIUM,8));}
            arrowMotion.keySet().retainAll(ids);arrowLabels.keySet().retainAll(ids);
            arrowCaptions.clear();
            for(var p:players)arrowCaptions.put(p.getUUID(),(names?arrowLabels.get(p.getUUID()):"")+(names&&distance?"\n":"")+(distance?nearbyDistances.getOrDefault(p.getUUID(),""):""));
        }
        float maxRadius=Math.max(40,Math.min(g.guiWidth(),g.guiHeight())*.5f-48*density);
        float spacing=(names?64:distance?40:32)*density;
        float goal=Math.min(maxRadius,Math.max(96*density,players.size()*spacing/(2*(float)Math.PI)));
        arrowRadius=reduced?goal:dev.sqwat.vela.ui.animation.Motion.approach(arrowRadius,goal,9,dt);
        for(int i=0;i<players.size();i++){
            var point=players.get(i).getPosition(partial);
            arrowAngles[i]=NearDirection.radians(point.x-origin.x,point.z-origin.z,camera.yRot());
        }
        float iconSize=Math.min(28*density,players.size()<2?28*density:2*arrowRadius*(float)Math.sin(Math.PI/players.size())-3*density);
        iconSize=Math.max(4,iconSize);
        float[] placed=arrowLayout.place(arrowAngles,players.size(),arrowRadius,iconSize+2*density);
        for(int i=0;i<players.size();i++){
            var player=players.get(i);
            int playerRgb=dev.sqwat.vela.VelaClient.services().friends().contains(player.getGameProfile().name())?VelaTheme.POSITIVE&0xFFFFFF:rgb;
            float angle=placed[i];
            final float destination=angle;
            float[] state=arrowMotion.computeIfAbsent(player.getUUID(),id->new float[]{0,destination});
            state[0]=reduced?1:dev.sqwat.vela.ui.animation.Motion.approach(state[0],1,10,dt);
            float r=arrowRadius;
            float x=g.guiWidth()/2f+(float)Math.cos(angle)*r,y=g.guiHeight()/2f+(float)Math.sin(angle)*r;
            int alpha=Math.round(255*state[0]);
            g.pose().pushMatrix();
            try{
                g.pose().translate(x,y);g.pose().scale(density);g.pose().rotate(angle+(float)Math.PI/2);
                float size=iconSize/density;
                VelaDraw.blit(g,NEAR_ARROW,-size/2,-size/2,size,size,0,0,48,48,48,48,(alpha<<24)|playerRgb);
            }finally{g.pose().popMatrix();}
            if(!names&&!distance)continue;
            g.pose().pushMatrix();
            try{
                g.pose().translate(x,y+14*density);g.pose().scale(density);
                String caption=arrowCaptions.getOrDefault(player.getUUID(),"");
                VelaDraw.floatingText(g,caption,1,3,alpha<<24,8);
                VelaDraw.floatingText(g,caption,0,2,(alpha<<24)|0xF4F6FF,8);
            }finally{g.pose().popMatrix();}
        }
    }
    private static void nearArrowText(GuiGraphicsExtractor g,String text,int y,int alpha,int rgb){
        VelaDraw.centeredText(g,text,1,y+1,(alpha<<24),VelaDraw.Weight.MEDIUM,8);
        VelaDraw.centeredText(g,text,0,y,(alpha<<24)|rgb,VelaDraw.Weight.MEDIUM,8);
    }
    public boolean supportsTransparency(String id){var module=modules.byId(id);return module!=null&&module.settings().stream().anyMatch(s->s.id().equals("transparent"));}
    public boolean transparent(String id){return flag(id,"transparent");}
    public java.util.List<dev.sqwat.vela.module.setting.BooleanSetting> contextSettings(String id){
        var module=modules.byId(id);if(module==null)return List.of();
        return module.settings().stream().filter(s->s instanceof dev.sqwat.vela.module.setting.BooleanSetting&&s.visible()&&(id.equals("target")||s.id().equals("transparent"))).map(s->(dev.sqwat.vela.module.setting.BooleanSetting)s).toList();
    }
    public void toggleContextSetting(String id,String setting){
        for(var option:contextSettings(id))if(option.id().equals(setting)){option.toggle();var services=dev.sqwat.vela.VelaClient.services();if(services!=null)services.configs().requestSave();return;}
    }
    public void toggleTransparency(String id){var module=modules.byId(id);if(module==null)return;module.settings().stream().filter(s->s.id().equals("transparent")&&s instanceof dev.sqwat.vela.module.setting.BooleanSetting).forEach(s->((dev.sqwat.vela.module.setting.BooleanSetting)s).toggle());var services=dev.sqwat.vela.VelaClient.services();if(services!=null)services.configs().requestSave();}
    private List<net.minecraft.world.item.ItemStack> targetEquipment(net.minecraft.world.entity.LivingEntity entity){
        if(entity==null||!flag("target","equipment"))return List.of();
        if(equipmentEntity==entity&&equipmentTick==entity.tickCount)return equipmentCache;
        equipmentEntity=entity;equipmentTick=entity.tickCount;
        var items=new java.util.ArrayList<net.minecraft.world.item.ItemStack>(6);
        if(!entity.getMainHandItem().isEmpty())items.add(entity.getMainHandItem());
        if(!entity.getOffhandItem().isEmpty())items.add(entity.getOffhandItem());
        for(var slot:ARMOR_SLOTS){var stack=entity.getItemBySlot(slot);if(!stack.isEmpty())items.add(stack);}
        equipmentCache=items;
        return equipmentCache;
    }
    private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR_SLOTS={net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};

    private static void hudHeading(GuiGraphicsExtractor g,String icon,String title){
        dev.sqwat.vela.ui.render.VelaIcons.draw(g,icon,7,6,10,VelaTheme.PRIMARY);
        VelaDraw.text(g,title,23,7,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
    }
    private void drawPanel(GuiGraphicsExtractor g,String id,int width,int height,int panel){
        var state=panels.get(id);
        if(!editing&&Minecraft.getInstance().gui.screen()==null&&state!=null&&!Float.isNaN(state.height))height=Math.round(state.height);
        VelaDraw.roundedRect(g,0,0,width,height,6,panel);
        float opaque=state==null||Float.isNaN(state.alpha)?1f:state.alpha;
        dev.sqwat.vela.ui.render.GlassSurface.draw(g,0,0,width,height,1-opaque);
        int borderAlpha=Math.round(112*opaque);
        if(borderAlpha>0)VelaDraw.roundedOutline(g,0,0,width,height,6,(borderAlpha<<24)|0x576578,0x00000000);
    }
    private int panelColor(String id){
        long now=System.nanoTime();var state=panel(id);boolean first=Float.isNaN(state.alpha);
        float target=flag(id,"transparent")?0:1;
        float alpha=dev.sqwat.vela.ui.animation.Motion.approach(first?target:state.alpha,target,12,first?0:Math.min(.1f,(now-state.time)/1e9f));
        state.time=now;state.alpha=alpha;return (Math.round(184.5f*alpha)<<24)|0x10141C;
    }
    private static int watermarkWidth(String[] parts){int width=30+Math.max(0,parts.length-1)*21;for(int i=0;i<parts.length;i++)width+=VelaDraw.textWidth(i==0?parts[i]:dev.sqwat.vela.ui.render.MetricIcon.value(parts[i]),VelaDraw.Weight.MEDIUM);return width;}
    private String watermarkText="Vela";
    private String[] watermarkParts={"Vela"},watermarkLabels={"Vela"};
    private int[] watermarkTextX={23};
    private int watermarkWidth=-1,watermarkFps,watermarkPing;
    private long watermarkTps;
    private Object watermarkMask;
    private String watermark(Minecraft client){
        // Inputs are compared every frame; the string, split and text measurement only rebuild when one changes.
        int fps=flag("watermark","fps")?client.getFps():-1;
        long tps=Double.doubleToRawLongBits(Double.NaN);
        if(flag("watermark","tps"))tps=Double.doubleToRawLongBits(HudTelemetry.tps());
        int ping=-1;
        if(flag("watermark","ping")&&client.player!=null&&client.getConnection()!=null){var info=client.getConnection().getPlayerInfo(client.player.getUUID());if(info!=null)ping=Math.max(-1,HudTelemetry.ping(info.getLatency()));}
        Object mask=dev.sqwat.vela.module.StreamerMode.mask();
        if(watermarkWidth>=0&&fps==watermarkFps&&tps==watermarkTps&&ping==watermarkPing&&mask==watermarkMask)return watermarkText;
        watermarkFps=fps;watermarkTps=tps;watermarkPing=ping;watermarkMask=mask;
        String text="Vela";
        if(fps>=0)text+=" · "+fps+" FPS";
        double ticks=Double.longBitsToDouble(tps);if(!Double.isNaN(ticks))text+=" · "+String.format(java.util.Locale.ROOT,"%.1f",ticks)+" TPS";
        if(ping>=0)text+=" · "+ping+" ms";
        watermarkText=text;watermarkParts=text.split(" · ");watermarkWidth=watermarkWidth(watermarkParts);
        watermarkLabels=new String[watermarkParts.length];watermarkTextX=new int[watermarkParts.length];
        int tx=23;
        for(int i=0;i<watermarkParts.length;i++){
            if(i>0)tx+=21;
            watermarkLabels[i]=i==0?watermarkParts[i]:dev.sqwat.vela.ui.render.MetricIcon.value(watermarkParts[i]);
            watermarkTextX[i]=tx;tx+=VelaDraw.textWidth(watermarkLabels[i],VelaDraw.Weight.MEDIUM);
        }
        return text;
    }
    private java.util.List<net.minecraft.world.entity.player.Player> nearby(Minecraft client){
        if(client.player==null||client.level==null){nearPresence.clear();nearbyLabels.clear();nearbyDistances.clear();nearbyLevel=null;nearbyTick=Integer.MIN_VALUE;return nearbyCache=java.util.List.of();}
        if(nearbyLevel==client.level&&nearbyTick==client.player.tickCount)return nearbyCache;
        if(nearbyLevel!=client.level)nearPresence.clear();
        nearbyLevel=client.level;nearbyTick=client.player.tickCount;
        var connection=client.getConnection();if(connection==null)return nearbyCache=java.util.List.of();
        var listed=new java.util.HashSet<>(connection.getListedOnlinePlayers());
        nearbyCache=NearPlayers.<net.minecraft.world.entity.player.Player>closest(client.level.players(),nearArrows()?24:NearDirection.LIMIT,p->{
            if(p==client.player||!p.isAlive()||p.isSpectator())return false;
            var info=connection.getPlayerInfo(p.getUUID());
            return info!=null&&NearPlayers.eligible(listed.contains(info),p.getUUID(),p.getGameProfile().name(),info.getProfile().id(),info.getProfile().name())&&nearPresence.observe(p,nearbyTick);
        },p->client.player.distanceToSqr(p),p->dev.sqwat.vela.VelaClient.services().friends().contains(p.getGameProfile().name()));
        nearPresence.finish(nearbyTick);
        nearbyLabels.clear();nearbyDistances.clear();
        nearWidthGoal=112;
        for(var player:nearbyCache){
            String name=player.getGameProfile().name();
            nearbyLabels.put(player.getUUID(),name);
            nearbyDistances.put(player.getUUID(),Math.round(client.player.distanceTo(player))+" м");
            nearWidthGoal=Math.max(nearWidthGoal,HudGeometry.nearWidth(VelaDraw.textWidth(name,VelaDraw.Weight.REGULAR,8)));
        }
        return nearbyCache;
    }

    public record HudPosition(int x, int y) {}
    public record RectSize(int width, int height) {}
}
