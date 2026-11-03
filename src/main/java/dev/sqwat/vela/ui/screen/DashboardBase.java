package dev.sqwat.vela.ui.screen;
import dev.sqwat.vela.ui.render.ControlChrome;
import dev.sqwat.vela.ui.render.ModuleSettingsGeometry;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.friend.FriendEntry;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.BooleanGroup;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.module.setting.Setting;
import dev.sqwat.vela.ui.animation.FrameClock;
import dev.sqwat.vela.ui.animation.Motion;
import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dashboard state, layout and shared helpers. Drawing lives in the subclasses (DashboardModules, DashboardPanels). */
abstract class DashboardBase extends VelaScreen {
    protected dev.sqwat.vela.module.setting.TextSetting editingText;
    protected final dev.sqwat.vela.ui.input.TextEdit settingTextEdit=new dev.sqwat.vela.ui.input.TextEdit();
    protected final java.util.Map<dev.sqwat.vela.module.setting.TextSetting,Rect> textSettingHits=new java.util.HashMap<>();
    protected void finishTextSetting(){
        if(editingText==null)return;
        editingText.set(settingTextEdit.text());editingText=null;
        dev.sqwat.vela.module.utility.StreamerMode.refresh();services.configs().requestSave();
    }
    protected final VelaServices services;
    protected dev.sqwat.vela.module.setting.KeySetting capturingSetting;
    protected final Screen parent;
    protected final MarkerPanel markerPanel;
    protected final dev.sqwat.vela.ui.render.UiControls controls=new dev.sqwat.vela.ui.render.UiControls();
    protected final FrameClock clock = new FrameClock();
    protected final Map<String, Float> hover = new HashMap<>();
    protected final Map<String,float[]> friendPositions=new HashMap<>();
    protected final Map<String,float[]> modulePositions=new HashMap<>();
    protected long moduleReorderUntil;
    protected Rect animatedFriendCard(String key,Rect destination){
        float[] p=friendPositions.computeIfAbsent(key,k->new float[]{destination.x(),destination.y()+scroll});
        p[0]=destination.x();
        p[1]=Motion.approach(p[1],destination.y()+scroll,reducedMotion()?60:13,frameDt);
        return new Rect(Math.round(p[0]),Math.round(p[1]-scroll),destination.width(),destination.height());
    }
    protected final Map<VelaSection, Rect> navHits = new EnumMap<>(VelaSection.class);
    protected final List<CardHit> cardHits = new ArrayList<>();
    protected final List<SettingHit> settingHits = new ArrayList<>();
    protected final List<ProfileHit> profileHits = new ArrayList<>();
    protected VelaSection section = VelaSection.UTILITIES;
    protected final java.util.Set<String> expandedModules=new java.util.LinkedHashSet<>();
    public boolean isModuleSettingsOpen(String id){return expandedModules.contains(id)&&cardHits.stream().anyMatch(hit->hit.module().id().equals(id));}
    protected String search = "";
    protected boolean searchFocused;
    protected final dev.sqwat.vela.ui.input.TextEdit searchEdit=new dev.sqwat.vela.ui.input.TextEdit(),keyEdit=new dev.sqwat.vela.ui.input.TextEdit();
    protected float scroll;
    protected float targetScroll;
    protected float scrollLimit;
    protected float entrance;
    protected float frameDt;
    protected boolean configPanelOpen;
    protected float sideReveal;
    protected float sideScroll,sideScrollTarget;
    protected Rect sideHit=new Rect(0,0,0,0),sideCloseHit=sideHit,configOptionsHit=sideHit,configFolderHit=sideHit;
    protected int pointerX, pointerY;
    protected Rect searchHit = new Rect(0, 0, 0, 0);
    protected Rect actionHit = new Rect(0, 0, 0, 0);
    protected Rect gearHit = actionHit, contentHit = actionHit, popupRect = actionHit, moduleContentHit=actionHit;
    protected NumberSetting dragging;
    protected Rect dragTrack;
    protected ChoiceSetting choice;
    protected final RgbPicker rgbPicker=new RgbPicker();
    protected float choiceScroll,choiceScrollTarget;
    /** How each module's settings are grouped for display (ModuleSettingGroups). */
    protected final ModuleSettingGroups groups;
    protected List<Setting<?>> visibleSettings(ClientModule module){return groups.visible(module);}
    protected ClientModule binding, contextModule;
    protected boolean capturing;
    protected int draftKey;
    protected String draftLabel;
    protected ClientModule.BindMode draftMode;
    protected final List<PopupHit> popupHits = new ArrayList<>();
    protected final List<FriendHit> friendHits = new ArrayList<>();

    protected static VelaSection rememberedSection(VelaServices services){try{return VelaSection.valueOf(services.configs().current().lastSection);}catch(Exception e){return VelaSection.UTILITIES;}}
    protected final Map<Rect,String> favoriteHits=new java.util.LinkedHashMap<>();
    /** Search results show their tab; clicking it opens the tab, scrolls to the card and flashes it. */
    protected final Map<Rect,ClientModule> locationHits=new java.util.LinkedHashMap<>();
    protected String revealModule,flashModule;
    protected long flashUntil;
    protected static final long FLASH_NANOS=1_200_000_000L;
    protected static final int LOCATION_PAD=6,LOCATION_GAP=6;
    protected static VelaSection sectionOf(ClientModule module){for(var s:VelaSection.values())if(s.category()==module.category())return s;return VelaSection.UTILITIES;}
    protected void jumpTo(ClientModule module){
        section=sectionOf(module);sideReveal=sideScroll=sideScrollTarget=0;friendFocused=configKeyFocused=configNameFocused=false;
        search="";searchEdit.set("");searchFocused=false;targetScroll=scroll=0;
        revealModule=flashModule=module.id();flashUntil=System.nanoTime()+FLASH_NANOS;sectionSwitchAt=System.nanoTime();
        dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.TAB);persistView();
    }
    protected float flash(ClientModule module){
        if(!module.id().equals(flashModule))return 0;
        float left=(flashUntil-System.nanoTime())/(float)FLASH_NANOS;
        if(left<=0){flashModule=null;return 0;}
        // Eases in while the tab slides in, holds, then fades out.
        float rise=Math.clamp((1-left)*FLASH_NANOS/1.8e8f,0,1);
        return rise*rise*(3-2*rise)*Math.min(1,left*2.5f);
    }
    /** Scroll position of each tab for this game session: reopening the dashboard or a tab continues where it was left. */
    private static final java.util.EnumMap<VelaSection,Float> SCROLLS=new java.util.EnumMap<>(VelaSection.class);
    protected void rememberScroll(){SCROLLS.put(section,targetScroll);}
    protected static float rememberedScroll(VelaSection section){return SCROLLS.getOrDefault(section,0f);}
    protected void persistView(){rememberScroll();services.configs().current().lastSection=section.name();services.configs().current().expandedModules=new java.util.LinkedHashSet<>(expandedModules);services.configs().current().friendOptionsOpen=friendSettings;services.configs().requestSave();}
    protected void saveBind(){if(binding!=null){binding.bind(draftKey,draftLabel,draftMode);services.configs().requestSave();}}

    protected Rect dragModeHit=new Rect(0,0,0,0),versionHit=new Rect(0,0,0,0);
    protected long sectionSwitchAt;

    protected String friendInput="";
    protected final Map<Rect,String> suggestionHits=new java.util.LinkedHashMap<>();
    protected java.util.List<String> suggestions=java.util.List.of();
    protected String suggestionQuery="";
    protected long suggestionRefresh;
    protected int suggestionOffset,suggestionSelection=-1;
    protected Rect suggestionPanel=new Rect(0,0,0,0);
    protected final Map<String,String> suggestionLabels=new HashMap<>();
    protected float suggestionVisualOffset;
    protected final dev.sqwat.vela.ui.input.TextEdit friendEdit=new dev.sqwat.vela.ui.input.TextEdit();
    protected boolean friendFocused,friendSettings;
    protected final BooleanGroup friendEffects=new BooleanGroup("friend-effects","Визуалы",List.of(
        new BooleanSetting("china_hat","ChinaHat","",true),new BooleanSetting("dash_trail","DashTrail","",true),new BooleanSetting("jump_circle","JumpCircle","",true)),false);
    protected Rect friendEffectsHit=new Rect(0,0,0,0);
    protected Rect friendInputHit=new Rect(0,0,0,0),friendOptionsHit=friendInputHit,friendVisualHit=friendInputHit;
    protected void addFriend(){if(services.friends().add(friendInput)){friendInput="";showToast("Друг добавлен");}else showToast("Введите новый корректный ник");}
    
    protected String configKey="",configName="",renameSource;
    protected boolean configNameFocused;
    protected final dev.sqwat.vela.ui.input.TextEdit nameEdit=new dev.sqwat.vela.ui.input.TextEdit();
    protected Rect configNameHit=new Rect(0,0,0,0);
    protected boolean configKeyFocused;
    protected Rect configKeyHit=new Rect(0,0,0,0),importHit=configKeyHit;

    protected final Map<String,Long> savedTimes=new HashMap<>();
    protected long savedTimesRefresh;
    protected String savedLabel(String name){
        long now=System.currentTimeMillis();
        if(now>savedTimesRefresh){savedTimes.clear();for(String profile:services.configs().profiles())try{savedTimes.put(profile,java.nio.file.Files.getLastModifiedTime(services.configs().profilePath(profile)).toMillis());}catch(java.io.IOException ignored){}savedTimesRefresh=now+1000;}
        Long time=savedTimes.get(name);if(time==null)return "Время сохранения неизвестно";
        long minutes=Math.max(0,(now-time)/60000);return minutes==0?"Сохранено только что":minutes<60?dev.sqwat.vela.ui.Lang.tr("Сохранено ")+minutes+dev.sqwat.vela.ui.Lang.tr(" мин. назад"):minutes<1440?dev.sqwat.vela.ui.Lang.tr("Сохранено ")+minutes/60+dev.sqwat.vela.ui.Lang.tr(" ч. назад"):dev.sqwat.vela.ui.Lang.tr("Сохранено ")+minutes/1440+dev.sqwat.vela.ui.Lang.tr(" д. назад");
    }
    protected void drawerButton(GuiGraphicsExtractor g,Rect rect,String icon,boolean open){controls.iconButton(g,"drawer:"+section+":"+icon,rect,icon,open,rect.contains(pointerX,pointerY));}
    protected boolean sideOpen(){return section==VelaSection.MARKERS?markerPanel.settingsOpen():section==VelaSection.FRIENDS?friendSettings:section==VelaSection.CONFIGS&&configPanelOpen;}
    protected int sidePanelHeight(){return section==VelaSection.MARKERS?markerPanel.settingsHeight():section==VelaSection.CONFIGS?244:166;}
    protected void closeSide(){markerPanel.closeSettings();friendSettings=false;configPanelOpen=false;renameSource=null;configNameFocused=configKeyFocused=false;persistView();}

    protected void drawToast(GuiGraphicsExtractor g, Layout l) {
    }

    public boolean hasModal(){return rgbPicker.open()||binding!=null||contextModule!=null||choice!=null;}
    @Override public void onClose() { finishTextSetting();persistView(); dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.CLOSE); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    protected Layout layout() {
        var l=dev.sqwat.vela.ui.render.DashboardChrome.layout(width,height);
        return new Layout(l.x(),l.y(),l.width(),l.height(),l.sidebar());
    }
    @Override protected float interfaceScale(){return services.configs().current().uiScale;}

    protected boolean reducedMotion() { return services.configs().current().reducedMotion || !services.configs().current().animations; }
    protected float animate(String key, boolean active, float dt) {
        float current = hover.getOrDefault(key, 0f);
        float next = Motion.approach(current, active ? 1 : 0, reducedMotion() ? 60 : 10.8f, dt);
        hover.put(key, next);
        return next;
    }
    protected void createConfig(){
        String name=configName.strip();
        if(!name.matches("[A-Za-z0-9_-]{1,32}")){showToast("Имя: латиница, цифры, _ и -");return;}
        if(renameSource!=null){try{services.configs().renameProfile(renameSource,name);renameSource=null;configName="";nameEdit.set("");savedTimesRefresh=0;showToast(dev.sqwat.vela.ui.Lang.tr("Конфиг переименован · ")+name);}catch(java.io.IOException error){showToast(error.getMessage());}return;}
        if(services.configs().profiles().contains(name.toLowerCase(java.util.Locale.ROOT))){showToast("Такой конфиг уже есть");return;}
        services.configs().createProfile(name);configName="";nameEdit.set("");showToast(dev.sqwat.vela.ui.Lang.tr("Конфиг создан · ")+name);
    }
    protected void showToast(String message) {String[] lines=message.split(" · ",2);dev.sqwat.vela.ui.Notifications.show(lines[0],lines.length>1?lines[1]:"",section==VelaSection.CONFIGS?"configs":section==VelaSection.FRIENDS?"friends":"settings");}
    protected Rect primaryButton(GuiGraphicsExtractor g, Rect rect, String label) {
        float progress=actionProgress(rect,label);
        primaryButton(g,rect,label,progress);
        return dev.sqwat.vela.ui.render.ActionGeometry.hit(rect,progress);
    }
    protected float actionProgress(Rect rect,String label) {
        String key="action-"+label+"-"+rect.x()+"-"+rect.y();
        Rect visible=dev.sqwat.vela.ui.render.ActionGeometry.hit(rect,hover.getOrDefault(key,0f));
        return animate(key,visible.contains(pointerX,pointerY),frameDt);
    }
    protected void primaryButton(GuiGraphicsExtractor g,Rect rect,String label,float h){dev.sqwat.vela.ui.render.UiControls.drawAction(g,rect,label,h);}
    protected void showHint(GuiGraphicsExtractor g,String text,int x,int y){int w=Math.round(VelaDraw.textWidth(text,VelaDraw.Weight.REGULAR,7))+12;x=Math.min(x,contentHit.x()+contentHit.width()-w);VelaDraw.roundedRect(g,x,y,w,18,4,VelaTheme.SURFACE_DEEP);VelaDraw.text(g,text,x+6,y+5,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7);}
    protected void drawEmpty(GuiGraphicsExtractor g, Rect area, String label) {
        dev.sqwat.vela.ui.render.DashboardChrome.empty(new dev.sqwat.vela.ui.render.GameUiCanvas(g),area,
            label.contains("модулей")?"Пока нет модулей":label,
            label.contains("модулей")?"Добавленные функции появятся в этом разделе":"Попробуйте изменить поисковый запрос");
    }
    protected static void drawSearchIcon(GuiGraphicsExtractor g, int x, int y, int color) {
        g.outline(x, y, 7, 7, color);
        g.fill(x + 6, y + 6, x + 10, y + 7, color);
    }
    protected static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
    protected static String ellipsis(String value, int max) { return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…"; }
    protected static String titleCase(String value) { return value.isBlank() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1); }
    protected static String format(double value) { return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.2f", value); }
    protected static boolean rowIntersects(Rect rect, int top, int bottom) { return rect.y() + rect.height() >= top && rect.y() <= bottom; }
    protected static void cycleSetting(Setting<?> setting) {
        if (setting instanceof BooleanSetting value) value.toggle();
        else if (setting instanceof NumberSetting value) {
            double next = value.value() + value.step();
            value.set(next > value.max() ? value.min() : next);
        } else if (setting instanceof ChoiceSetting value) {
            int current = value.options().indexOf(value.value());
            value.set(value.options().get((current + 1) % value.options().size()));
        }
    }
    protected void clippedText(GuiGraphicsExtractor g,String text,int x,int y,int max,int color,VelaDraw.Weight weight) {
        dev.sqwat.vela.ui.render.UiClip.enable(g,x,y-1,x+Math.max(1,max),y+14);VelaDraw.text(g,font,text,x,y,color,weight);g.disableScissor();
    }
    /** Plain holder (fields and accessors) so the dashboard subclasses can read it. */
    protected static final class PopupHit {
        final Rect rect;
        final Runnable action;
        PopupHit(Rect rect,Runnable action) {this.rect=rect;this.action=action;}
        Rect rect() {return rect;}
        Runnable action() {return action;}
    }
    /** Plain holder (fields and accessors) so the dashboard subclasses can read it. */
    protected static final class FriendHit {
        final java.util.UUID id;
        final Rect rect;
        final boolean favorite;
        FriendHit(java.util.UUID id,Rect rect,boolean favorite) {this.id=id;this.rect=rect;this.favorite=favorite;}
        java.util.UUID id() {return id;}
        Rect rect() {return rect;}
        boolean favorite() {return favorite;}
    }

    /** Plain holder (fields and accessors) so the dashboard subclasses can read it. */
    protected static final class Layout {
        final int x;
        final int y;
        final int w;
        final int h;
        final int sidebar;
        Layout(int x, int y, int w, int h, int sidebar) {this.x=x;this.y=y;this.w=w;this.h=h;this.sidebar=sidebar;}
        int x() {return x;}
        int y() {return y;}
        int w() {return w;}
        int h() {return h;}
        int sidebar() {return sidebar;}
    }
    /** Plain holder (fields and accessors) so the dashboard subclasses can read it. */
    protected static final class CardHit {
        final ClientModule module;
        final Rect card;
        final Rect toggle;
        CardHit(ClientModule module, Rect card, Rect toggle) {this.module=module;this.card=card;this.toggle=toggle;}
        ClientModule module() {return module;}
        Rect card() {return card;}
        Rect toggle() {return toggle;}
    }
    /** Plain holder (fields and accessors) so the dashboard subclasses can read it. */
    protected static final class SettingHit {
        final Setting<?> setting;
        final Rect rect;
        final Rect visible;
        SettingHit(Setting<?> setting, Rect rect,Rect visible) {this.setting=setting;this.rect=rect;this.visible=visible;}
        Setting<?> setting() {return setting;}
        Rect rect() {return rect;}
        Rect visible() {return visible;}
        SettingHit(Setting<?> setting,Rect rect){this(setting,rect,rect);
    }
    }
    /** Plain holder (fields and accessors) so the dashboard subclasses can read it. */
    protected static final class ProfileHit {
        final String name;
        final Rect rect;
        final int action;
        ProfileHit(String name, Rect rect,int action) {this.name=name;this.rect=rect;this.action=action;}
        String name() {return name;}
        Rect rect() {return rect;}
        int action() {return action;}
    }
    protected DashboardBase(Screen parent, VelaServices services, VelaSection initialSection) {
        super(Component.literal("Vela Client"));
        
        this.parent = parent;
        this.services = services;
        controls.animate("scrollbar-reveal",0);
        this.markerPanel=new MarkerPanel(services);
        this.groups=new ModuleSettingGroups(services);
        this.section = initialSection;
        this.scroll=this.targetScroll=rememberedScroll(initialSection);
        this.expandedModules.addAll(services.configs().current().expandedModules);this.friendSettings=services.configs().current().friendOptionsOpen;
    
    }

}
