package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.ZakoOptions;
import java.util.*;

/** Vela owns persistence; the vendored renderer reads a per-frame settings snapshot. */
public final class ZakoOptimization {
    public static final String TITLE="Оптимизация 3.0";
    private final List<Setting<?>> settings=new ArrayList<>();
    private java.util.function.BooleanSupplier availability=()->true;
    private <T extends Setting<?>> T setting(T value){settings.add(value);return value;}
    public List<Setting<?>> settings(){return Collections.unmodifiableList(settings);}
    public boolean available(){return availability.getAsBoolean();}
    public void require(java.util.function.BooleanSupplier condition){
        availability=Objects.requireNonNull(condition);
        if(!available())dev.sqwat.vela.zako.IntegrationRuntime.requestReset();
    }
    private final ChoiceSetting page=setting(new ChoiceSetting("page","Раздел","Группа настроек оптимизации","Рендер",List.of("Рендер","Сущности","Спавнеры","Блоки","Частицы","Интерфейс","Память")));
    private final ZakoOptConfig.Values values=new ZakoOptConfig.Values();
    private final List<Runnable> bindings=new ArrayList<>();
    // Features are shared; switching service instances must publish again.
    private static Object publishedOwner;
    private final Object owner=new Object();
    private Object[] publishedValues;
    private final Map<String,Setting<?>> options=new LinkedHashMap<>();
    public final BooleanSetting entityLod,playerLod,singleCopy,spawnerCull,particleLight,particlePhysics,blockCull,textureBuffer;
    public final NumberSetting entityDistance,spawnerDistance;
    private final BooleanSetting occludePlayers,occludeHanging,occludeHolograms,occludeMobs;
    private BooleanSetting occlusion(String id,String name,String description){
        var toggle=setting(new BooleanSetting(id,name,description,true));toggle.visibleWhen(()->page.value().equals("Сущности"));return toggle;
    }
    // Independent of the vendored renderer: occlusion is Vela's own pass and also works next to external ZakoOpt.
    public boolean occludePlayers(){return occludePlayers.value();}
    public boolean occludeHanging(){return occludeHanging.value();}
    public boolean occludeHolograms(){return occludeHolograms.value();}
    public boolean occludeMobs(){return occludeMobs.value();}
    public boolean anyOcclusion(){return occludePlayers.value()||occludeHanging.value()||occludeHolograms.value()||occludeMobs.value();}
    public ZakoOptimization(){
        for(var p:ZakoOptions.pages(values))for(var group:p.groups())for(var option:group.options()){
            String label=groupName(group.key());
            Setting<?> entry;
            if(option instanceof ZakoOptions.Toggle t){
                var toggle=setting(new BooleanSetting(t.key(),text(t.key(),false),text(t.key(),true),t.get().get()));
                bindings.add(()->t.set().accept(toggle.value()));entry=toggle;
            }else{
                var n=(ZakoOptions.Distance)option;
                var number=setting(new NumberSetting(n.key(),text(n.key(),false),text(n.key(),true),n.get().get(),n.min(),n.max(),1));
                bindings.add(()->n.set().accept(number.value().intValue()));entry=number;
            }
            entry.visibleWhen(()->page.value().equals(label)&&option.available()&&parentEnabled(option.key()));
            options.put(option.key(),entry);
        }
        textureBuffer=setting(new BooleanSetting("texture_buffer","Кэш буферов OpenGL","Пропускать повторное подключение того же буфера",true));
        textureBuffer.visibleWhen(()->page.value().equals("Рендер")&&!dev.sqwat.vela.zako.MixinPlugin.VULKAN);
        bindings.add(()->values.textureBuffer=textureBuffer.value());
        occludePlayers=occlusion("occlusion_players","Игроки за стенами","Не рисовать модели, броню и предметы игроков за блоками. Ники остаются");
        occludeHanging=occlusion("occlusion_hanging","Картины и рамки за стенами","Не рисовать картины, рамки и карты за блоками");
        occludeHolograms=occlusion("occlusion_holograms","Голограммы за стенами","Не рисовать текстовые дисплеи и стойки за блоками. Дисплеи «сквозь стены» остаются");
        occludeMobs=occlusion("occlusion_mobs","Мобы за стенами","Не рисовать мобов без имени за блоками");
        entityLod=flag("entity_lod");playerLod=flag("player_lod");singleCopy=flag("item_single_copy");
        spawnerCull=flag("spawner_cull");particleLight=flag("particle_light");particlePhysics=flag("particle_physics");blockCull=flag("block_entity_early_cull");
        entityDistance=(NumberSetting)options.get("entity_lod_distance");spawnerDistance=(NumberSetting)options.get("spawner_distance");
        bindFeatures();
    }
    private void bindFeatures(){
        var defaults=dev.sqwat.vela.zako.feature.Features.defaults();
        for(var feature:dev.sqwat.vela.zako.feature.Features.all()){
            String group=groupName(feature.data().group().key());
            BooleanSetting toggle=(BooleanSetting)options.get(feature.key());
            if(toggle==null){
                toggle=setting(new BooleanSetting(feature.key(),text(feature.key(),false),text(feature.key(),true),(Boolean)defaults.get(feature.key())));
                options.put(feature.key(),toggle);
            }
            final BooleanSetting enabled=toggle;
            enabled.visibleWhen(()->page.value().equals(group)&&feature.available()&&parentEnabled(feature.key()));
            bindings.add(()->feature.value=enabled.value());
            for(var field:feature.settings()){
                String key=field.getAnnotation(dev.sqwat.vela.zako.feature.Setting.class).key();
                Setting<?> entry=options.get(key);
                if(entry==null){
                    Object initial=defaults.get(key);
                    if(field.getType()==boolean.class){
                        entry=setting(new BooleanSetting(key,text(key,false),text(key,true),(Boolean)initial));
                    }else{
                        var range=field.getAnnotation(dev.sqwat.vela.zako.feature.Range.class);
                        if(range==null)throw new IllegalStateException("Missing optimization range: "+key);
                        entry=setting(new NumberSetting(key,text(key,false),text(key,true),((Number)initial).intValue(),range.min(),range.max(),1));
                    }
                    options.put(key,entry);
                }
                final Setting<?> control=entry;
                control.visibleWhen(()->page.value().equals(group)&&feature.available()&&enabled.value());
                bindings.add(()->feature.set(field,control instanceof NumberSetting number?number.value().intValue():control.value()));
            }
        }
    }
    private BooleanSetting flag(String id){return (BooleanSetting)options.get(id);}
    private boolean parentEnabled(String id){
        String parent=switch(id){case "item_lod_distance"->"item_lod";case "entity_lod_distance","player_lod"->"entity_lod";case "entity_anim_distance"->"entity_anim_freeze";case "spawner_distance"->"spawner_cull";default->null;};
        return parent==null||flag(parent).value();
    }
    private static String groupName(String group){return switch(group){case "items"->"Сущности";case "spawners"->"Спавнеры";case "blocks"->"Блоки";case "particles"->"Частицы";case "interface"->"Интерфейс";case "memory"->"Память";default->"Рендер";};}
    private static final Map<String,String> LABELS=loadLabels();
    private static Map<String,String> loadLabels(){
        try(var in=ZakoOptimization.class.getResourceAsStream("/assets/velaclient/zako-options.json")){
            if(in==null)throw new IllegalStateException("Missing optimization labels");
            try(var reader=new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)){
                var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                var labels=new HashMap<String,String>();json.entrySet().forEach(e->labels.put(e.getKey(),e.getValue().getAsString()));return Map.copyOf(labels);
            }
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot load optimization labels",e);}
    }
    private static String text(String key,boolean description){return LABELS.getOrDefault("zakoopt.option."+key+(description?".tooltip":""),key);}
    public ZakoOptConfig.Values values(){
        var settings=this.settings;
        boolean changed=publishedOwner!=owner||publishedValues==null;
        if(!changed)for(int i=0;i<settings.size();i++){
            if(!Objects.equals(publishedValues[i],settings.get(i).value())){changed=true;break;}
        }
        if(changed){
            bindings.forEach(Runnable::run);
            if(publishedValues==null)publishedValues=new Object[settings.size()];
            for(int i=0;i<settings.size();i++)publishedValues[i]=settings.get(i).value();
            publishedOwner=owner;
        }
        return values;
    }
    public boolean reduceLayers(boolean player,double squaredDistance){return available()&&entityLod.value()&&(!player||playerLod.value())&&beyond(squaredDistance,entityDistance.value());}
    public static boolean beyond(double squaredDistance,double distance){return squaredDistance>distance*distance;}
    public static ZakoOptimization active(){
        var services=dev.sqwat.vela.VelaClient.services();if(services==null)return null;
        var m=services.zakoOptimization();return m.available()?m:null;
    }
}
