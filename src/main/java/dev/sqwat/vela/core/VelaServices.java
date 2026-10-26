package dev.sqwat.vela.core;

import dev.sqwat.vela.account.AccountService;
import dev.sqwat.vela.config.ConfigService;
import dev.sqwat.vela.friend.FriendService;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.ui.HudLayoutService;

import java.nio.file.Path;
import java.util.List;

public final class VelaServices implements AutoCloseable {
    private final ModuleRegistry modules = new ModuleRegistry();
    private final dev.sqwat.vela.module.Far far = new dev.sqwat.vela.module.Far();
    private final dev.sqwat.vela.module.RenderTweaks renderTweaks = new dev.sqwat.vela.module.RenderTweaks();
    private final dev.sqwat.vela.module.ZakoOptimization zakoOptimization = new dev.sqwat.vela.module.ZakoOptimization();
    private final FriendService friends;
    private final AccountService accounts;
    private final ConfigService configs;
    private final HudLayoutService hudLayout;
    private final dev.sqwat.vela.marker.MarkerService markers;
    private final dev.sqwat.vela.module.CommandEntries commands;
    public dev.sqwat.vela.module.CommandEntries commands(){return commands;}
    public dev.sqwat.vela.marker.MarkerService markers(){return markers;}

    public VelaServices(Path configRoot) {
        registerFoundationModules();
        commands=new dev.sqwat.vela.module.CommandEntries(configRoot);
        friends = new FriendService(configRoot);
        accounts = new AccountService(configRoot);
        configs = new ConfigService(configRoot, modules,zakoOptimization);
        hudLayout = new HudLayoutService(configRoot, modules);
        markers=new dev.sqwat.vela.marker.MarkerService(configRoot);
    }

    private void registerFoundationModules() {
        modules.register(new dev.sqwat.vela.module.ShulkerPreview());
        modules.register(new dev.sqwat.vela.module.Crosshair());
        modules.register(new dev.sqwat.vela.module.HitColor());
        modules.register(new dev.sqwat.vela.module.HitBubbles());
        modules.register(new dev.sqwat.vela.module.Animations());
        modules.register(new dev.sqwat.vela.module.SwingAnimations());
        modules.register(new dev.sqwat.vela.module.Ambience());
        modules.register(new dev.sqwat.vela.module.Shaders());
        modules.register(new dev.sqwat.vela.module.AutoSprint());
        modules.register(new dev.sqwat.vela.module.ClickPearl());
        modules.register(new dev.sqwat.vela.module.ItemScroller());
        modules.register(new dev.sqwat.vela.module.Pinger());
        modules.register(new dev.sqwat.vela.module.Trajectories());
        modules.register(new dev.sqwat.vela.module.FakePlayer());
        modules.register(new dev.sqwat.vela.module.JumpCircle());
        modules.register(new dev.sqwat.vela.module.ChinaHat());
        modules.register(new dev.sqwat.vela.module.DashTrail());
        modules.register(new dev.sqwat.vela.module.TargetESP());
        modules.register(new dev.sqwat.vela.module.KillEffect());
        modules.register(new dev.sqwat.vela.module.WorldParticles());
        modules.register(new dev.sqwat.vela.module.ItemHighlight());
        modules.register(new dev.sqwat.vela.module.Hitboxes());
        modules.register(new dev.sqwat.vela.module.SelfTag());
        modules.register(new dev.sqwat.vela.module.HpCounter());
        modules.register(new dev.sqwat.vela.module.ExplosionWave());
        modules.register(new dev.sqwat.vela.module.StreamerMode());
        modules.register(new dev.sqwat.vela.module.Particles());
        modules.register(new dev.sqwat.vela.module.ViewModel());
        modules.register(new dev.sqwat.vela.module.MotionBlur());
        modules.register(new dev.sqwat.vela.module.CrystalOptimizer());
        modules.register(new dev.sqwat.vela.module.AspectRatio());
        modules.register(new dev.sqwat.vela.module.AutoTpAccept());
        modules.register(new dev.sqwat.vela.module.BlockOverlay());
        modules.register(new dev.sqwat.vela.module.Gamma());
        modules.register(new dev.sqwat.vela.module.CommandModule(false));
        modules.register(new dev.sqwat.vela.module.CommandModule(true));
        modules.register(renderTweaks);
        modules.register(far);
        modules.register(new dev.sqwat.vela.module.ItemPhysics());
        modules.register(new FoundationModule("coordinates", "Coordinates", "Ваше положение в мире", ModuleCategory.HUD)
            .add(new ChoiceSetting("format", "Формат", "Вид записи координат", "XYZ", List.of("XYZ", "Compact"))));
        modules.register(new FoundationModule("armor", "Armor HUD", "Отображение прочности брони", ModuleCategory.HUD));
        modules.register(new FoundationModule("watermark","Watermark","Логотип, FPS, пинг и тики сервера",ModuleCategory.HUD)
            .add(new BooleanSetting("fps","FPS","Частота кадров",true))
            .add(new BooleanSetting("tps","TPS","Оценка частоты тиков по сообщениям сервера",true))
            .add(new BooleanSetting("ping","Пинг","Задержка соединения",true)));
        var nearMode=new ChoiceSetting("mode","Режим","Список игроков или указатели вокруг прицела","HUD",List.of("HUD","Стрелки"));
        var nearNames=new BooleanSetting("names","Ник","Показывать имя у стрелки",false);
        var nearDistance=new BooleanSetting("distance","Расстояние","Расстояние до игрока в метрах",true);
        var nearTheme=new BooleanSetting("client_color","Цвет клиента","Цвет стрелок из темы",true);
        var nearColor=new dev.sqwat.vela.module.setting.ColorSetting("color","Цвет",0x8877FF);
        nearNames.visibleWhen(()->nearMode.value().equals("Стрелки"));
        nearDistance.visibleWhen(()->nearMode.value().equals("Стрелки"));
        nearTheme.visibleWhen(()->nearMode.value().equals("Стрелки"));
        nearColor.visibleWhen(()->nearMode.value().equals("Стрелки")&&!nearTheme.value());
        modules.register(new FoundationModule("near","Near","Ближайшие игроки",ModuleCategory.HUD)
            .add(nearMode).add(nearNames).add(nearDistance).add(nearTheme).add(nearColor));
        modules.register(new FoundationModule("hotkeys","Hotkeys","Отображение активных биндов",ModuleCategory.HUD));
        modules.register(new FoundationModule("inventory","Inventory HUD","Отображение предметов в инвентаре",ModuleCategory.HUD));
        var targetEquipment=new BooleanSetting("equipment","Показывать экипировку","Броня и предметы в руках цели",true);
        var targetPercent=new BooleanSetting("equipment_percent","Проценты прочности","Показывать проценты под предметами; полоски остаются видимыми",true);
        targetPercent.visibleWhen(targetEquipment::value);
        modules.register(new FoundationModule("target","Target HUD","Отображение цели",ModuleCategory.HUD).add(targetEquipment).add(targetPercent));
        modules.register(new FoundationModule("potions","Potions","Отображение активных эффектов",ModuleCategory.HUD));
        modules.register(new FoundationModule("speakers","Speakers","Отображение говорящих игроков",ModuleCategory.HUD));
        modules.register(new FoundationModule("cooldowns","Cooldowns","Отображение перезарядки предметов",ModuleCategory.HUD));
        for(ClientModule module:modules.all())if(module.category()==ModuleCategory.HUD)
            ((FoundationModule)module).add(new BooleanSetting("transparent","Прозрачный","Заменяет обычный фон прозрачным стеклом",false));
    }

    public ModuleRegistry modules() { return modules; }
    public dev.sqwat.vela.module.Far far() { return far; }
    public dev.sqwat.vela.module.RenderTweaks renderTweaks() { return renderTweaks; }
    public dev.sqwat.vela.module.ZakoOptimization zakoOptimization() { return zakoOptimization; }
    public FriendService friends() { return friends; }
    public AccountService accounts() { return accounts; }
    public ConfigService configs() { return configs; }
    public HudLayoutService hudLayout() { return hudLayout; }

    @Override public void close() { hudLayout.save(); configs.close(); }

    private static final class FoundationModule extends ClientModule {
        private FoundationModule(String id, String name, String description, ModuleCategory category) {
            super(id, name, description, category);
        }

        private FoundationModule add(dev.sqwat.vela.module.setting.Setting<?> setting) {
            setting(setting);
            return this;
        }
    }
}
