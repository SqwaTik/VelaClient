package dev.sqwat.vela.core;

import dev.sqwat.vela.account.AccountService;
import dev.sqwat.vela.core.ConfigService;
import dev.sqwat.vela.friend.FriendService;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.ui.hud.HudLayoutService;

import java.nio.file.Path;
import java.util.List;

public final class VelaServices implements AutoCloseable {
    private final ModuleRegistry modules = new ModuleRegistry();
    private final dev.sqwat.vela.module.utility.Far far = new dev.sqwat.vela.module.utility.Far();
    private final dev.sqwat.vela.module.utility.RenderTweaks renderTweaks = new dev.sqwat.vela.module.utility.RenderTweaks();
    private final dev.sqwat.vela.module.ZakoOptimization zakoOptimization = new dev.sqwat.vela.module.ZakoOptimization();
    private final FriendService friends;
    private final AccountService accounts;
    private final ConfigService configs;
    private final HudLayoutService hudLayout;
    private final dev.sqwat.vela.marker.MarkerService markers;
    private final dev.sqwat.vela.module.utility.CommandEntries commands;
    public dev.sqwat.vela.module.utility.CommandEntries commands(){return commands;}
    public dev.sqwat.vela.marker.MarkerService markers(){return markers;}

    public VelaServices(Path configRoot) {
        registerFoundationModules();
        commands=new dev.sqwat.vela.module.utility.CommandEntries(configRoot);
        friends = new FriendService(configRoot);
        accounts = new AccountService(configRoot);
        configs = new ConfigService(configRoot, modules,zakoOptimization);
        hudLayout = new HudLayoutService(configRoot, modules);
        markers=new dev.sqwat.vela.marker.MarkerService(configRoot);
    }

    private void registerFoundationModules() {
        modules.register(new dev.sqwat.vela.module.utility.ShulkerPreview());
        modules.register(new dev.sqwat.vela.module.visual.Crosshair());
        modules.register(new dev.sqwat.vela.module.visual.HitColor());
        modules.register(new dev.sqwat.vela.module.visual.HitBubbles());
        modules.register(new dev.sqwat.vela.module.visual.Animations());
        modules.register(new dev.sqwat.vela.module.visual.SwingAnimations());
        modules.register(new dev.sqwat.vela.module.visual.Ambience());
        modules.register(new dev.sqwat.vela.module.visual.Shaders());
        modules.register(new dev.sqwat.vela.module.utility.AutoSprint());
        modules.register(new dev.sqwat.vela.module.utility.ClickPearl());
        modules.register(new dev.sqwat.vela.module.utility.Pinger());
        modules.register(new dev.sqwat.vela.module.utility.Trajectories());
        modules.register(new dev.sqwat.vela.module.utility.FakePlayer());
        modules.register(new dev.sqwat.vela.module.visual.JumpCircle());
        modules.register(new dev.sqwat.vela.module.visual.ChinaHat());
        modules.register(new dev.sqwat.vela.module.visual.DashTrail());
        modules.register(new dev.sqwat.vela.module.visual.TargetESP());
        modules.register(new dev.sqwat.vela.module.visual.KillEffect());
        modules.register(new dev.sqwat.vela.module.visual.WorldParticles());
        modules.register(new dev.sqwat.vela.module.visual.ItemHighlight());
        modules.register(new dev.sqwat.vela.module.visual.Hitboxes());
        modules.register(new dev.sqwat.vela.module.visual.SelfTag());
        modules.register(new dev.sqwat.vela.module.visual.HpCounter());
        modules.register(new dev.sqwat.vela.module.visual.ExplosionWave());
        modules.register(new dev.sqwat.vela.module.utility.StreamerMode());
        modules.register(new dev.sqwat.vela.module.visual.Particles());
        modules.register(new dev.sqwat.vela.module.visual.ViewModel());
        modules.register(new dev.sqwat.vela.module.visual.MotionBlur());
        modules.register(new dev.sqwat.vela.module.utility.CrystalOptimizer());
        modules.register(new dev.sqwat.vela.module.utility.AspectRatio());
        modules.register(new dev.sqwat.vela.module.utility.Freelook());
        modules.register(new dev.sqwat.vela.module.visual.Wasted());
        modules.register(new dev.sqwat.vela.module.visual.CustomHotbar());
        modules.register(new dev.sqwat.vela.module.utility.HandSwap());
        modules.register(new dev.sqwat.vela.module.utility.TalTracker());
        modules.register(new dev.sqwat.vela.module.visual.BrewViewer());
        modules.register(new dev.sqwat.vela.module.utility.AutoReconnect());
        modules.register(new dev.sqwat.vela.module.utility.ChatTweaks());
        modules.register(new dev.sqwat.vela.module.utility.ContainerTools());
        modules.register(new dev.sqwat.vela.module.utility.Death());
        modules.register(new dev.sqwat.vela.module.utility.FoodInfo());
        modules.register(new dev.sqwat.vela.module.utility.StorageIndex());
        modules.register(new dev.sqwat.vela.module.visual.TabPing());
        modules.register(new dev.sqwat.vela.module.visual.ItemPickups());
        modules.register(new dev.sqwat.vela.module.utility.HitSound());
        modules.register(new dev.sqwat.vela.module.utility.AutoTpAccept());
        modules.register(new dev.sqwat.vela.module.utility.BlockOverlay());
        modules.register(new dev.sqwat.vela.module.utility.Gamma());
        modules.register(new dev.sqwat.vela.module.utility.CommandModule(false));
        modules.register(new dev.sqwat.vela.module.utility.CommandModule(true));
        modules.register(renderTweaks);
        modules.register(far);
        modules.register(new dev.sqwat.vela.module.visual.ItemPhysics());
        modules.register(new FoundationModule("coordinates", "Coordinates", "Ваше положение в мире", ModuleCategory.HUD)
            .add(new BooleanSetting("other_dimension","Другое измерение","Координаты портала в Незере или обычном мире",false))
            .add(new ChoiceSetting("format", "Формат", "Вид записи координат", "XYZ", List.of("XYZ", "Compact"))));
        modules.register(new FoundationModule("armor", "Armor HUD", "Отображение прочности брони", ModuleCategory.HUD));
        modules.register(new FoundationModule("watermark","Watermark","Логотип, FPS, скорость, пинг, тики, время и онлайн",ModuleCategory.HUD)
            .add(new BooleanSetting("fps","FPS","Частота кадров",true))
            .add(new BooleanSetting("bps","BPS","Горизонтальная скорость, блоков в секунду",true))
            .add(new BooleanSetting("tps","TPS","Оценка частоты тиков по сообщениям сервера",true))
            .add(new BooleanSetting("ping","Пинг","Задержка соединения",true))
            .add(new BooleanSetting("time","Время","Текущее время на компьютере",false))
            .add(new BooleanSetting("online","Онлайн","Игроков на сервере",false)));
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
        var strokesCps=new BooleanSetting("cps","CPS","Клики в секунду под кнопками мыши",true);
        var strokesMouse=new BooleanSetting("mouse","Кнопки мыши","Левая и правая кнопки",true);
        strokesCps.visibleWhen(strokesMouse::value);
        modules.register(new FoundationModule("module_list","Array List","Список включённых функций",ModuleCategory.HUD));
        modules.register(new FoundationModule("keystrokes","Key Strokes","Нажатия клавиш движения и кликов",ModuleCategory.HUD)
            .add(strokesMouse).add(strokesCps).add(new BooleanSetting("space","Пробел","Клавиша прыжка",true)));
        for(ClientModule module:modules.all())if(module.category()==ModuleCategory.HUD&&module instanceof FoundationModule foundation)
            foundation.add(new BooleanSetting("transparent","Прозрачный","Заменяет обычный фон прозрачным стеклом",false));
    }

    public ModuleRegistry modules() { return modules; }
    public dev.sqwat.vela.module.utility.Far far() { return far; }
    public dev.sqwat.vela.module.utility.RenderTweaks renderTweaks() { return renderTweaks; }
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
