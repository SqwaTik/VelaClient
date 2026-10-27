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

public final class VelaDashboardScreen extends VelaScreen {
    private dev.sqwat.vela.module.setting.TextSetting editingText;
    private final dev.sqwat.vela.ui.input.TextEdit settingTextEdit=new dev.sqwat.vela.ui.input.TextEdit();
    private final java.util.Map<dev.sqwat.vela.module.setting.TextSetting,Rect> textSettingHits=new java.util.HashMap<>();
    private void finishTextSetting(){
        if(editingText==null)return;
        editingText.set(settingTextEdit.text());editingText=null;
        dev.sqwat.vela.module.utility.StreamerMode.refresh();services.configs().requestSave();
    }
    private final VelaServices services;
    private dev.sqwat.vela.module.setting.KeySetting capturingSetting;
    private final Screen parent;
    private final MarkerPanel markerPanel;
    private final dev.sqwat.vela.ui.render.UiControls controls=new dev.sqwat.vela.ui.render.UiControls();
    private final FrameClock clock = new FrameClock();
    private final Map<String, Float> hover = new HashMap<>();
    private final Map<String,float[]> friendPositions=new HashMap<>();
    private final Map<String,float[]> modulePositions=new HashMap<>();
    private long moduleReorderUntil;
    private Rect animatedFriendCard(String key,Rect destination){
        float[] p=friendPositions.computeIfAbsent(key,k->new float[]{destination.x(),destination.y()+scroll});
        p[0]=destination.x();
        p[1]=Motion.approach(p[1],destination.y()+scroll,reducedMotion()?60:13,frameDt);
        return new Rect(Math.round(p[0]),Math.round(p[1]-scroll),destination.width(),destination.height());
    }
    private final Map<VelaSection, Rect> navHits = new EnumMap<>(VelaSection.class);
    private final List<CardHit> cardHits = new ArrayList<>();
    private final List<SettingHit> settingHits = new ArrayList<>();
    private final List<ProfileHit> profileHits = new ArrayList<>();
    private VelaSection section = VelaSection.UTILITIES;
    private final java.util.Set<String> expandedModules=new java.util.LinkedHashSet<>();
    public boolean isModuleSettingsOpen(String id){return expandedModules.contains(id)&&cardHits.stream().anyMatch(hit->hit.module().id().equals(id));}
    private String search = "";
    private boolean searchFocused;
    private final dev.sqwat.vela.ui.input.TextEdit searchEdit=new dev.sqwat.vela.ui.input.TextEdit(),keyEdit=new dev.sqwat.vela.ui.input.TextEdit();
    private float scroll;
    private float targetScroll;
    private float scrollLimit;
    private float entrance;
    private float frameDt;
    private boolean configPanelOpen;
    private float sideReveal;
    private float sideScroll,sideScrollTarget;
    private Rect sideHit=new Rect(0,0,0,0),sideCloseHit=sideHit,configOptionsHit=sideHit,configFolderHit=sideHit;
    private int pointerX, pointerY;
    private Rect searchHit = new Rect(0, 0, 0, 0);
    private Rect actionHit = new Rect(0, 0, 0, 0);
    private Rect gearHit = actionHit, contentHit = actionHit, popupRect = actionHit, moduleContentHit=actionHit;
    private NumberSetting dragging;
    private Rect dragTrack;
    private ChoiceSetting choice;
    private final RgbPicker rgbPicker=new RgbPicker();
    private float choiceScroll,choiceScrollTarget;
    /** How each module's settings are grouped for display (ModuleSettingGroups). */
    private final ModuleSettingGroups groups;
    private List<Setting<?>> visibleSettings(ClientModule module){return groups.visible(module);}
    private ClientModule binding, contextModule;
    private boolean capturing;
    private int draftKey;
    private String draftLabel;
    private ClientModule.BindMode draftMode;
    private final List<PopupHit> popupHits = new ArrayList<>();
    private final List<FriendHit> friendHits = new ArrayList<>();

    public VelaDashboardScreen(Screen parent, VelaServices services) {
        this(parent, services, rememberedSection(services));
    }

    public VelaDashboardScreen(Screen parent, VelaServices services, VelaSection initialSection) {
        super(Component.literal("Vela Client"));
        this.parent = parent;
        this.services = services;
        controls.animate("scrollbar-reveal",0);
        this.markerPanel=new MarkerPanel(services);
        this.groups=new ModuleSettingGroups(services);
        this.section = initialSection;
        this.expandedModules.addAll(services.configs().current().expandedModules);this.friendSettings=services.configs().current().friendOptionsOpen;
    }

    private static VelaSection rememberedSection(VelaServices services){try{return VelaSection.valueOf(services.configs().current().lastSection);}catch(Exception e){return VelaSection.UTILITIES;}}
    private final Map<Rect,String> favoriteHits=new java.util.LinkedHashMap<>();
    /** Search results show their tab; clicking it opens the tab, scrolls to the card and flashes it. */
    private final Map<Rect,ClientModule> locationHits=new java.util.LinkedHashMap<>();
    private String revealModule,flashModule;
    private long flashUntil;
    private static final long FLASH_NANOS=1_200_000_000L;
    private static final int LOCATION_PAD=6,LOCATION_GAP=6;
    private static VelaSection sectionOf(ClientModule module){for(var s:VelaSection.values())if(s.category()==module.category())return s;return VelaSection.UTILITIES;}
    private void jumpTo(ClientModule module){
        section=sectionOf(module);sideReveal=sideScroll=sideScrollTarget=0;friendFocused=configKeyFocused=configNameFocused=false;
        search="";searchEdit.set("");searchFocused=false;targetScroll=scroll=0;
        revealModule=flashModule=module.id();flashUntil=System.nanoTime()+FLASH_NANOS;
        dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.TAB);persistView();
    }
    private float flash(ClientModule module){
        if(!module.id().equals(flashModule))return 0;
        float left=(flashUntil-System.nanoTime())/(float)FLASH_NANOS;
        if(left<=0){flashModule=null;return 0;}
        return Math.min(1,left*2.5f);
    }
    private void persistView(){services.configs().current().lastSection=section.name();services.configs().current().expandedModules=new java.util.LinkedHashSet<>(expandedModules);services.configs().current().friendOptionsOpen=friendSettings;services.configs().requestSave();}
    private void saveBind(){if(binding!=null){binding.bind(draftKey,draftLabel,draftMode);services.configs().requestSave();}}
    @Override
    public void renderVela(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        float dt = clock.step(); frameDt=dt; pointerX=mouseX; pointerY=mouseY;
        entrance = Motion.approach(entrance, 1, reducedMotion() ? 60 : 19.2f, dt);
        scroll = reducedMotion() ? targetScroll : Motion.scroll(scroll, targetScroll, dt);
        sideReveal=controls.animate("side-expansion",sideOpen()?1:0);
        sideScroll=reducedMotion()?sideScrollTarget:Motion.scroll(sideScroll,sideScrollTarget,dt);

        g.fill(0, 0, width, height, VelaTheme.OVERLAY);
        Layout l = layout();
        int lift = 0;
        l = new Layout(l.x, l.y + lift, l.w, l.h, l.sidebar);
        int shift=Math.min(l.x-8,Math.max(0,l.x+l.w+196-width));
        l=new Layout(l.x-Math.round(Math.max(0,shift)*sideReveal),l.y,l.w,l.h,l.sidebar);
        VelaTheme.setPrimary(VelaTheme.parseHex(services.configs().current().accentColor));
        drawWindow(g, l, mouseX, mouseY, dt);
        drawSidePanel(g,l);
        drawPopup(g,mouseX,mouseY);
        drawFriendSuggestions(g);
        boolean clickable=gearHit.contains(mouseX,mouseY)||actionHit.contains(mouseX,mouseY)||navHits.values().stream().anyMatch(r->r.contains(mouseX,mouseY))||cardHits.stream().anyMatch(h->h.card.contains(mouseX,mouseY))||settingHits.stream().anyMatch(h->h.visible.contains(mouseX,mouseY))||popupHits.stream().anyMatch(h->h.rect.contains(mouseX,mouseY))||profileHits.stream().anyMatch(h->h.rect.contains(mouseX,mouseY))||friendHits.stream().anyMatch(h->h.rect.contains(mouseX,mouseY))||locationHits.keySet().stream().anyMatch(r->r.contains(mouseX,mouseY));
        clickable|=section==VelaSection.FRIENDS&&friendOptionsHit.contains(mouseX,mouseY)||section==VelaSection.CONFIGS&&configOptionsHit.contains(mouseX,mouseY)||sideOpen()&&(sideCloseHit.contains(mouseX,mouseY)||importHit.contains(mouseX,mouseY));
        dev.sqwat.vela.ui.render.UiCursor.hand(clickable);
        if(section==VelaSection.MARKERS){dev.sqwat.vela.ui.render.UiCursor.hand(clickable||markerPanel.hovered(mouseX,mouseY));if(markerPanel.textHovered(mouseX,mouseY))g.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.IBEAM);}
    }

    private void drawWindow(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY, float dt) {
        var chrome=new dev.sqwat.vela.ui.render.DashboardChrome.Layout(l.x,l.y,l.w,l.h,l.sidebar);
        var canvas=new dev.sqwat.vela.ui.render.GameUiCanvas(g);
        float[] hovers=new float[VelaSection.values().length];
        navHits.clear();cardHits.clear();settingHits.clear();profileHits.clear();friendHits.clear();favoriteHits.clear();locationHits.clear();
        for(VelaSection entry:VelaSection.values()){
            Rect hit=chrome.navigation(entry.ordinal());navHits.put(entry,hit);
            hovers[entry.ordinal()]=animate("nav-"+entry.name(),hit.contains(mouseX,mouseY),dt);
        }
        searchHit=chrome.search();gearHit=chrome.settings();contentHit=chrome.content();actionHit=new Rect(0,0,0,0);
        dev.sqwat.vela.ui.render.DashboardChrome.draw(canvas,chrome,section.ordinal(),section.description(),null,searchFocused,hovers,gearHit.contains(mouseX,mouseY),VelaTheme.PRIMARY);
        drawerButton(g,gearHit,"settings",false);
        if(section==VelaSection.HUD){
            int dx=l.x+l.sidebar+42,dy=l.y+10;
            int extra=Math.round(VelaDraw.textWidth("Dragmode",VelaDraw.Weight.MEDIUM,8))+4;
            float dh=animate("drag-header",new Rect(dx,dy,18+Math.round(hover.getOrDefault("drag-header",0f)*extra),18).contains(mouseX,mouseY),dt);
            dragModeHit=new Rect(dx,dy,18+Math.round(dh*extra),18);
            VelaDraw.roundedRect(g,dx,dy,dragModeHit.width(),18,5,VelaTheme.SURFACE);
            dev.sqwat.vela.ui.render.VelaIcons.draw(g,"move",dx+4,dy+4,10,VelaTheme.mix(VelaTheme.TEXT_MUTED,VelaTheme.PRIMARY,dh));
            if(dh>.01f){dev.sqwat.vela.ui.render.UiClip.enable(g,dx+18,dy,dx+dragModeHit.width(),dy+18);
                VelaDraw.text(g,"Dragmode",dx+18,dy+5,(VelaTheme.TEXT_SECONDARY&0xFFFFFF)|(Math.round(dh*255)<<24),VelaDraw.Weight.MEDIUM,8);g.disableScissor();}
        }
        searchEdit.set(search);dev.sqwat.vela.ui.render.TextFieldDraw.draw(g,searchEdit,new Rect(searchHit.x()+20,searchHit.y()+4,searchHit.width()-26,14),searchFocused,"Поиск…");
        dev.sqwat.vela.ui.render.UiClip.enable(g,contentHit.x()-1,contentHit.y()-1,contentHit.x()+contentHit.width()+1,contentHit.y()+contentHit.height()+1);
        if(section==VelaSection.HUD)drawHudModules(g,contentHit,mouseX,mouseY,dt);
        else if(section==VelaSection.MARKERS){int total=markerPanel.draw(g,new Rect(contentHit.x(),contentHit.y()-Math.round(scroll),contentHit.width(),contentHit.height()),mouseX,mouseY,search);scrollLimit=Math.max(0,total-contentHit.height());targetScroll=Math.clamp(targetScroll,0,scrollLimit);}
        else if(section.category()!=null)drawModules(g,contentHit,mouseX,mouseY,dt);
        else if(section==VelaSection.FRIENDS)drawFriends(g,contentHit);
        else drawConfigs(g,contentHit);
        g.disableScissor();
        float scrollbarReveal=controls.animate("scrollbar-reveal",scrollLimit>0?1:0);
        if(scrollLimit>0){int thumb=Math.max(18,Math.round(contentHit.height()*contentHit.height()/(contentHit.height()+scrollLimit)));int sy=contentHit.y()+Math.round((contentHit.height()-thumb)*scroll/scrollLimit);Rect bar=ControlChrome.verticalReveal(new Rect(contentHit.x()+contentHit.width()+4,sy,2,thumb),scrollbarReveal);if(bar.height()>0)VelaDraw.roundedRect(g,bar.x(),bar.y(),bar.width(),bar.height(),1,VelaTheme.PRIMARY);}
        drawToast(g,l);
    }

    private void drawModules(GuiGraphicsExtractor g, Rect content, int mouseX, int mouseY, float dt) {
        moduleContentHit=content;
        cardHits.clear();
        settingHits.clear();
        List<ClientModule> modules = search.isBlank() ? services.modules().find(section.category(), search) : services.modules().findAll(search);
        modules=modules.stream().filter(ClientModule::available).sorted(java.util.Comparator.comparing(m->!services.configs().current().favorites.contains(m.id()))).toList();
        if (modules.isEmpty()) {
            scrollLimit = targetScroll = scroll = 0;
            drawEmpty(g, content, search.isBlank() ? "В этом разделе пока нет модулей" : "Ничего не найдено");
            return;
        }
        int gap = 8;
        int columnWidth = (content.width() - gap) / 2;
        int[] y = {content.y() - Math.round(scroll), content.y() - Math.round(scroll)};
        dev.sqwat.vela.ui.render.UiClip.enable(g,content.x()-1, content.y()-1, content.x() + content.width()+1, content.y() + content.height()+1);
        for (int index = 0; index < modules.size(); index++) {
            ClientModule module = modules.get(index);
            int column = index % 2;
            boolean expanded = expandedModules.contains(module.id());
            float expand=controls.animate("expand-"+module.id(),expanded?1:0);
            int fullExtra=animatedSettingsExtra(module);
            int cardHeight=44+Math.round(expand*fullExtra);
            // Height already animates: a second position spring made following cards overlap live controls.
            int tx=content.x()+column*(columnWidth+gap),ty=y[column];
            float[] p=modulePositions.computeIfAbsent(module.id(),key->new float[]{tx,ty+scroll});
            if(System.nanoTime()<moduleReorderUntil&&!reducedMotion()){p[0]=Motion.approach(p[0],tx,24,frameDt);p[1]=Motion.approach(p[1],ty+scroll,24,frameDt);}else{p[0]=tx;p[1]=ty+scroll;}
            Rect card = new Rect(Math.round(p[0]),Math.round(p[1]-scroll),columnWidth,cardHeight);
            if(module.id().equals(revealModule)&&search.isBlank()){targetScroll=Math.max(0,ty+Math.round(scroll)-content.y()-8);revealModule=null;}
            if (card.y() + card.height() >= content.y() && card.y() <= content.y() + content.height()) {
                drawModuleCard(g, module, card, mouseX, mouseY, dt, expanded);
                cardHits.add(new CardHit(module, card, new Rect(dev.sqwat.vela.ui.hud.HudGeometry.toggleX(card.x(),card.width(),!visibleSettings(module).isEmpty()), card.y() + 13, 20, 18)));
            }
            y[column] += cardHeight + gap;
        }
        g.disableScissor();
        int max = Math.max(y[0], y[1]) + Math.round(scroll) - content.y() - content.height();
        scrollLimit = Math.max(0, max);
        targetScroll = Math.max(0, Math.min(targetScroll, scrollLimit));
    }

    private Rect dragModeHit=new Rect(0,0,0,0);
    private void drawDescriptionHint(GuiGraphicsExtractor g,int mx,int my){
        if(sideOpen()||hasModal()||!contentHit.contains(mx,my))return;
        for(var hit:cardHits){
            Rect r=hit.card;
            if(!new Rect(r.x()+8,r.y()+20,r.width()-90,22).contains(mx,my))continue;
            String text=dev.sqwat.vela.ui.Lang.tr(hit.module.description());int available=r.width()-90;
            if(VelaDraw.textWidth(text,VelaDraw.Weight.REGULAR,8)<=available)return;
            java.util.List<String> lines=new java.util.ArrayList<>();String line="";
            for(String word:text.split("\\s+")){String next=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&VelaDraw.textWidth(next,VelaDraw.Weight.REGULAR,8)>260){lines.add(line);line=word;}else line=next;}if(!line.isEmpty())lines.add(line);
            int w=Math.min(width-16,276),h=lines.size()*12+14,x=Math.clamp(mx,8,width-w-8),y=Math.clamp(my+15,8,Math.max(8,height-h-8));
            VelaDraw.roundedOutline(g,x,y,w,h,6,VelaTheme.BORDER_HOVER,VelaTheme.SURFACE_DEEP);
            for(int i=0;i<lines.size();i++)VelaDraw.text(g,lines.get(i),x+8,y+7+i*12,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
            return;
        }
    }
    private void drawHudModules(GuiGraphicsExtractor g, Rect content, int mouseX, int mouseY, float dt) {
        drawModules(g,content,mouseX,mouseY,dt);
    }

    private void drawModuleCard(GuiGraphicsExtractor g,ClientModule module,Rect card,int mouseX,int mouseY,float dt,boolean expanded) {
        boolean hovered=card.contains(mouseX,mouseY);
        float h=animate("card-"+module.id(),hovered,dt);
        VelaDraw.roundedRect(g,card.x(),card.y(),card.width(),card.height(),7,
            VelaTheme.mix(VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,h),VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.PRIMARY,.18f),flash(module)));
        dev.sqwat.vela.ui.render.UiClip.enable(g,card.x()+2,card.y()+2,card.x()+card.width()-2,card.y()+card.height()-2);
        int toggleX=dev.sqwat.vela.ui.hud.HudGeometry.toggleX(card.x(),card.width(),!visibleSettings(module).isEmpty());
        Rect favorite=new Rect(toggleX-18,card.y()+14,15,16);
        favoriteHits.put(favorite,module.id());
        controls.favorite(g,"module-favorite-"+module.id(),new Rect(favorite.x()+3,favorite.y()+3,9,9),services.configs().current().favorites.contains(module.id()),favorite.contains(mouseX,mouseY));
        int labelX=card.x()+8,labelWidth=toggleX-card.x()-41;
        String key=(services.configs().current().alwaysShowBinds||hovered&&!expanded)&&module.boundKey()>=0?dev.sqwat.vela.ui.input.KeyLabels.of(module.boundKey(),module.keybind()):"";
        float keyWidth=key.isEmpty()?0:VelaDraw.textWidth(key,VelaDraw.Weight.REGULAR,7)+6;
        // Search results carry their tab as a chip: same padding on every side, centred on the title line.
        String location=search.isBlank()?"":dev.sqwat.vela.ui.Lang.tr("в ")+sectionOf(module).title();
        int chipText=location.isEmpty()?0:(int)Math.ceil(VelaDraw.textWidth(location,VelaDraw.Weight.MEDIUM,8)),chipWidth=chipText+LOCATION_PAD*2;
        float locationWidth=location.isEmpty()?0:chipWidth+LOCATION_GAP;
        String title=VelaDraw.ellipsize(module.name(),labelWidth-keyWidth-locationWidth,VelaDraw.Weight.MEDIUM);
        dev.sqwat.vela.ui.render.SearchText.draw(g,title,search,labelX,card.y()+8,module.enabled()?VelaTheme.TEXT:VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,28f/3);
        float afterTitle=labelX+VelaDraw.textWidth(title,VelaDraw.Weight.MEDIUM)+LOCATION_GAP;
        if(!key.isEmpty()){VelaDraw.text(g,key,afterTitle,card.y()+10,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7);afterTitle+=keyWidth;}
        if(!location.isEmpty()){
            Rect chip=new Rect(Math.round(afterTitle),card.y()+6,chipWidth,14);
            float over=animate("location-"+module.id(),chip.contains(mouseX,mouseY),dt);
            VelaDraw.roundedRect(g,chip.x(),chip.y(),chip.width(),chip.height(),5,VelaTheme.mix(0x14FFFFFF,(VelaTheme.PRIMARY&0xFFFFFF)|0x38000000,over));
            int color=VelaTheme.mix(0xFFDDE3EF,VelaTheme.TEXT,over);
            VelaDraw.text(g,location,chip.x()+LOCATION_PAD,chip.y()+3,color,VelaDraw.Weight.MEDIUM,8);
            if(over>.02f)g.fill(chip.x()+LOCATION_PAD,chip.y()+11,chip.x()+LOCATION_PAD+Math.round(chipText*over),chip.y()+12,color);
            locationHits.put(chip,module);
        }
        String remaining=dev.sqwat.vela.ui.Lang.tr(module.description()).strip();int split=remaining.length(); // translated before splitting into lines
        while(split>0&&VelaDraw.textWidth(remaining.substring(0,split),VelaDraw.Weight.REGULAR,8)>labelWidth)split=remaining.offsetByCodePoints(split,-1);
        if(split<remaining.length()){
            int space=remaining.lastIndexOf(' ',split);if(space>0)split=space;
            VelaDraw.text(g,remaining.substring(0,split),labelX,card.y()+21,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8);
            VelaDraw.text(g,remaining.substring(split).strip(),labelX,card.y()+31,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8);
        }else VelaDraw.text(g,remaining,labelX,card.y()+23,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8);
        if(module.available())VelaDraw.toggle(g,toggleX,card.y()+16,controls.animate("toggle-"+module.id(),module.enabled()?1:0),dev.sqwat.vela.ui.hud.HudGeometry.toggleHit(toggleX,card.y()+16).contains(mouseX,mouseY));
        else{
            Rect blocked=new Rect(toggleX,card.y()+12,24,20);
            VelaDraw.roundedOutline(g,blocked.x()+5,blocked.y()+4,12,12,6,VelaTheme.TEXT_MUTED,0);
            VelaDraw.text(g,"/",blocked.x()+9,blocked.y()+5,VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,9);
            if(blocked.contains(pointerX,pointerY)){g.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.NOT_ALLOWED);showHint(g,"Недоступно: установите Simple Voice Chat",blocked.x()-120,blocked.y()+23);}
        }
        if(!visibleSettings(module).isEmpty()){
            float expansion=controls.animate("expand-"+module.id(),expanded?1:0);
            g.pose().pushMatrix();g.pose().translate(card.x()+card.width()-15.5f,card.y()+21.5f);g.pose().rotate((float)Math.PI*expansion);
            dev.sqwat.vela.ui.render.VelaIcons.draw(g,"chevron",-6,-6,13,expanded?VelaTheme.PRIMARY:VelaTheme.TEXT_MUTED);g.pose().popMatrix();
        }
        if(card.height()>45){
            int first=settingHits.size();
            drawSettings(g,module,card.x()+8,card.y()+ModuleSettingsGeometry.CONTENT_OFFSET,card.width()-16);
            for(int i=settingHits.size()-1;i>=first;i--){
                SettingHit hit=settingHits.get(i);Rect visible=ModuleSettingsGeometry.visibleHit(hit.rect(),card,moduleContentHit);
                if(visible.width()==0||visible.height()==0)settingHits.remove(i);
                else settingHits.set(i,new SettingHit(hit.setting(),hit.rect(),visible));
            }
        }
        g.disableScissor();
        // Submit the complete frame outside the content clip, after its contents.
        float flash=flash(module);
        new dev.sqwat.vela.ui.render.GameUiCanvas(g).border(card.x(),card.y(),card.width(),card.height(),7,
            VelaTheme.mix(VelaTheme.mix(VelaTheme.BORDER,VelaTheme.BORDER_HOVER,h),VelaTheme.PRIMARY,flash));
    }

    private void drawSettings(GuiGraphicsExtractor g, ClientModule module, int x, int y, int width) {
        g.fill(x, y - 4, x + width, y - 3, VelaTheme.BORDER);
        List<Setting<?>> settings = visibleSettings(module);
        if (settings.isEmpty()) {
            VelaDraw.text(g, font, "Дополнительных настроек нет", x, y + 7, VelaTheme.TEXT_MUTED, VelaDraw.Weight.REGULAR);
            return;
        }
        int cursor = y + ModuleSettingsGeometry.FIRST_ROW_OFFSET;
        for (Setting<?> setting : settings) {
            int rowHeight = ModuleSettingsGeometry.rowHeight(setting);
            float reveal=settingReveal(module,setting);
            if(reveal<.002f)continue;
            g.pose().pushMatrix();g.pose().translate(x,cursor);g.pose().scale(1,reveal);g.pose().translate(-x,-cursor);
            Rect row = ModuleSettingsGeometry.row(x,cursor,width,setting);
            if(!(setting instanceof dev.sqwat.vela.module.setting.ActionSetting))dev.sqwat.vela.ui.render.VelaIcons.draw(g,dev.sqwat.vela.ui.render.SettingIcons.of(setting),x,cursor-1,10,VelaTheme.TEXT_MUTED);
            if(setting instanceof dev.sqwat.vela.module.setting.KeySetting key){
                VelaDraw.text(g,key.name(),x+15,cursor,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
                Rect hit=new Rect(x+width-70,cursor-4,70,18);controls.button(g,"key-setting-"+module.id()+key.id(),hit,capturingSetting==key?"Нажмите…":dev.sqwat.vela.ui.input.KeyLabels.of(key.value(),"None"),hit.contains(pointerX,pointerY));
                if(setting.visible()&&reveal>.97f)settingHits.add(new SettingHit(key,hit));g.pose().popMatrix();cursor+=Math.round(rowHeight*reveal);continue;
            }
            if(setting instanceof dev.sqwat.vela.module.setting.ActionSetting action){
                Rect button=new Rect(x,cursor,width,26);controls.button(g,module.id()+"-editor",button,action.name(),button.contains(pointerX,pointerY));
                if(setting.visible()&&reveal>.97f)settingHits.add(new SettingHit(action,button));g.pose().popMatrix();cursor+=Math.round(rowHeight*reveal);continue;
            }
            if(setting instanceof dev.sqwat.vela.module.setting.TextSetting text){
                VelaDraw.text(g,text.name(),x+15,cursor,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
                Rect field=new Rect(x,cursor+14,width,23),inner=new Rect(x+7,cursor+19,width-14,14);
                VelaDraw.roundedRect(g,field.x(),field.y(),field.width(),field.height(),5,VelaTheme.SURFACE_DEEP);
                var edit=editingText==text?settingTextEdit:new dev.sqwat.vela.ui.input.TextEdit();
                if(editingText!=text)edit.set(text.value());
                dev.sqwat.vela.ui.render.TextFieldDraw.draw(g,edit,inner,editingText==text,"Псевдоним");
                textSettingHits.put(text,inner);
                if(setting.visible()&&reveal>.97f)settingHits.add(new SettingHit(text,field));
                g.pose().popMatrix();cursor+=Math.round(rowHeight*reveal);continue;
            }
            if(setting instanceof dev.sqwat.vela.module.setting.ColorSetting color){
                VelaDraw.text(g,color.name(),x+15,cursor,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
                VelaDraw.roundedOutline(g,x+width-15,cursor-3,14,14,7,VelaTheme.BORDER,0xFF000000|color.value());
                if(setting.visible()&&reveal>.97f)settingHits.add(new SettingHit(color,row));
                g.pose().popMatrix();cursor+=Math.round(rowHeight*reveal);continue;
            }
            if(setting instanceof ChoiceSetting select){
                VelaDraw.text(g,setting.name(),x+15,cursor,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
                if(select instanceof BooleanGroup group&&group.segmented()){
                    Rect box=new Rect(x,cursor+14,width,21);
                    VelaDraw.roundedRect(g,box.x()-3,box.y()-3,box.width()+6,box.height()+6,7,0xFF252C37);
                    VelaDraw.roundedRect(g,box.x(),box.y(),box.width(),box.height(),5,0xFF191E28);
                    for(int i=0;i<group.flags().size();i++){
                        var flag=group.flags().get(i);
                        Rect segment=dev.sqwat.vela.ui.render.SegmentedControl.segment(box,i,group.flags().size());
                        float active=controls.animate("segment-"+module.id()+"-"+flag.id(),flag.value()?1:0);
                        dev.sqwat.vela.ui.render.SegmentedControl.draw(new dev.sqwat.vela.ui.render.GameUiCanvas(g),segment,flag.name(),active,controls.animate("segment-hover-"+module.id()+"-"+flag.id(),segment.contains(pointerX,pointerY)?1:0));
                        if(setting.visible()&&reveal>.97f)settingHits.add(new SettingHit(flag,segment));
                    }
                    g.pose().popMatrix();cursor+=Math.round(rowHeight*reveal);continue;
                }
                Rect control=new Rect(x,cursor+14,width,21);
                VelaDraw.roundedRect(g,control.x(),control.y(),control.width(),21,5,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,.35f));
                String label=select instanceof BooleanGroup group?group.label():groups.isMetrics(select)?String.join(", ",groups.metrics().options().stream().filter(o->groups.metric(o).value()).toList()):select.value();
                if(label.isBlank())label="Не выбраны";
                dev.sqwat.vela.ui.render.VelaIcons.draw(g,"contact_options",x+6,cursor+19,11,VelaTheme.TEXT_MUTED);
                VelaDraw.text(g,VelaDraw.ellipsize(label,width-48,VelaDraw.Weight.REGULAR,8),x+23,cursor+20,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
                g.pose().pushMatrix();g.pose().translate(x+width-12,cursor+24);g.pose().rotate((float)Math.PI*controls.animate("choice-arrow-"+module.id()+"-"+setting.id(),choice==setting?1:0));dev.sqwat.vela.ui.render.VelaIcons.draw(g,"chevron",-5,-5,10,VelaTheme.TEXT_MUTED);g.pose().popMatrix();
                if(setting.visible()&&reveal>.97f)settingHits.add(new SettingHit(setting,control));
                if(choice==setting&&setting.visible()){int ph=Math.min(88,select.options().size()*20+8);popupRect=new Rect(x,Math.max(6,Math.min(this.height-ph-6,cursor+38)),Math.min(width,156),ph);}
                else if(choice==setting){choice=null;popupHits.clear();}
                g.pose().popMatrix();cursor+=Math.round(rowHeight*reveal);continue;
            }
            clippedText(g,setting.name(),x+15,cursor,width-(setting instanceof BooleanSetting?35:95),VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR);
            String value = setting instanceof BooleanSetting b ? (b.value() ? "Вкл" : "Выкл")
                : setting instanceof NumberSetting n ? format(n.value().floatValue()) : setting instanceof ChoiceSetting c ? c.value() : String.valueOf(setting.value());
            value=VelaDraw.ellipsize(value,76,VelaDraw.Weight.MEDIUM);
            if(setting instanceof BooleanSetting b) {
                controls.checkbox(g,"setting-"+module.id()+"-"+setting.id(),ModuleSettingsGeometry.checkbox(x,cursor,width),b.value());
            } else {
                if(setting instanceof dev.sqwat.vela.module.setting.TextSetting)VelaDraw.roundedOutline(g,x+width-79,cursor-4,79,18,4,VelaTheme.BORDER,VelaTheme.SURFACE_DEEP);
                VelaDraw.text(g,font,value,x+width-5-VelaDraw.textWidth(value,VelaDraw.Weight.MEDIUM),cursor,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);
            }
            if(setting instanceof NumberSetting number) {
                Rect track=new Rect(x+3,cursor+12,width-6,13);
                controls.slider(g,"slider-"+module.id()+"-"+setting.id(),new Rect(track.x(),track.y()+5,track.width(),3),(float)number.fraction());
                if(setting.visible()&&reveal>.97f)settingHits.add(new SettingHit(setting,track));
            } else if(setting.visible()&&reveal>.97f)settingHits.add(new SettingHit(setting,row));
            g.pose().popMatrix();cursor += Math.round(rowHeight*reveal);
        }
    }

    private float settingReveal(ClientModule module,Setting<?> setting){return controls.animate("setting-reveal-"+module.id()+"-"+setting.id(),setting.visible()?1:0);}
    private int animatedSettingsExtra(ClientModule module){
        var settings=visibleSettings(module);int rows=0;
        for(var setting:settings)rows+=Math.round(ModuleSettingsGeometry.rowHeight(setting)*settingReveal(module,setting));
        return ModuleSettingsGeometry.CONTENT_OFFSET+ModuleSettingsGeometry.FIRST_ROW_OFFSET-ModuleSettingsGeometry.HEADER_HEIGHT+rows;
    }

    private String friendInput="";
    private final Map<Rect,String> suggestionHits=new java.util.LinkedHashMap<>();
    private java.util.List<String> suggestions=java.util.List.of();
    private String suggestionQuery="";
    private long suggestionRefresh;
    private int suggestionOffset,suggestionSelection=-1;
    private Rect suggestionPanel=new Rect(0,0,0,0);
    private final Map<String,String> suggestionLabels=new HashMap<>();
    private float suggestionVisualOffset;
    private void drawFriendSuggestions(GuiGraphicsExtractor g){
        suggestionHits.clear();suggestionPanel=new Rect(0,0,0,0);
        if(section!=VelaSection.FRIENDS||!friendFocused||minecraft.getConnection()==null)return;
        long now=System.nanoTime();
        if(!suggestionQuery.equals(friendInput)||now>=suggestionRefresh){
            if(!suggestionQuery.equals(friendInput)){suggestionOffset=0;suggestionVisualOffset=0;suggestionSelection=-1;}
            suggestionQuery=friendInput;suggestionRefresh=now+250_000_000L;
            var online=minecraft.getConnection().getOnlinePlayers();
            suggestions=dev.sqwat.vela.friend.NameSuggestions.matching(online.stream().map(p->p.getProfile().name()).toList(),friendInput,100);
            suggestionLabels.clear();for(var info:online)if(suggestions.contains(info.getProfile().name()))suggestionLabels.put(info.getProfile().name(),info.getProfile().name());
        }
        if(suggestions.isEmpty())return;
        int x=friendInputHit.x(),y=friendInputHit.y()+friendInputHit.height()+3,w=friendInputHit.width();
        int count=Math.min(5,suggestions.size());suggestionOffset=Math.clamp(suggestionOffset,0,suggestions.size()-count);
        suggestionVisualOffset=Motion.approach(suggestionVisualOffset,suggestionOffset,18,frameDt);
        suggestionPanel=new Rect(x,y,w,count*24+6);
        g.nextStratum();VelaDraw.roundedOutline(g,x,y,w,suggestionPanel.height(),6,VelaTheme.BORDER,VelaTheme.SURFACE);
        dev.sqwat.vela.ui.render.UiClip.enable(g,x+3,y+3,x+w-3,y+3+count*24);
        int first=(int)Math.floor(suggestionVisualOffset);
        for(int index=first;index<Math.min(suggestions.size(),first+count+1);index++){
            var r=new Rect(x+3,y+3+Math.round((index-suggestionVisualOffset)*24),w-9,24);String name=suggestions.get(index);
            float h=controls.animate("suggestion-"+name,r.contains(pointerX,pointerY)||index==suggestionSelection?1:0);
            VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),4,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,h));
            dev.sqwat.vela.ui.hud.PlayerAvatars.draw(g,name,r.x()+4,r.y()+4,16);
            VelaDraw.text(g,VelaDraw.ellipsize(suggestionLabels.getOrDefault(name,name),w-38,VelaDraw.Weight.REGULAR,8),r.x()+25,r.y()+8,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);if(r.y()>=y+3&&r.y()+24<=y+3+count*24)suggestionHits.put(r,name);
        }
        g.disableScissor();
        if(suggestions.size()>count){int track=count*24,thumb=Math.max(12,track*count/suggestions.size());int sy=y+3+Math.round((track-thumb)*suggestionVisualOffset/(suggestions.size()-count));VelaDraw.roundedRect(g,x+w-5,sy,2,thumb,1,VelaTheme.PRIMARY);}
    }
    private final dev.sqwat.vela.ui.input.TextEdit friendEdit=new dev.sqwat.vela.ui.input.TextEdit();
    private boolean friendFocused,friendSettings;
    private final BooleanGroup friendEffects=new BooleanGroup("friend-effects","Визуалы",List.of(
        new BooleanSetting("china_hat","ChinaHat","",true),new BooleanSetting("dash_trail","DashTrail","",true),new BooleanSetting("jump_circle","JumpCircle","",true)),false);
    private Rect friendEffectsHit=new Rect(0,0,0,0);
    private Rect friendInputHit=new Rect(0,0,0,0),friendOptionsHit=friendInputHit,friendVisualHit=friendInputHit;
    private void drawFriends(GuiGraphicsExtractor g, Rect content) {
        var cfg=services.configs().current();
        friendVisualHit=new Rect(0,0,0,0);
        friendEffectsHit=new Rect(0,0,0,0);
        int addY=content.y();
        VelaDraw.roundedOutline(g,content.x(),addY,content.width(),83,7,VelaTheme.BORDER,VelaTheme.SURFACE);
        friendOptionsHit=new Rect(content.x()+content.width()-35,addY+9,23,23);
        drawerButton(g,friendOptionsHit,"contact_options",friendSettings);
        dev.sqwat.vela.ui.render.VelaIcons.draw(g,"friends",content.x()+9,addY+14,19,VelaTheme.PRIMARY);
        VelaDraw.text(g,"Добавление друзей",content.x()+35,addY+10,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);
        VelaDraw.text(g,"Добавляйте друзей и отмечайте избранных",content.x()+35,addY+25,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
        Rect addButton=new Rect(content.x()+content.width()-85,addY+48,67,24);
        float addProgress=actionProgress(addButton,"Добавить");
        friendInputHit=new Rect(content.x()+10,addY+48,content.width()-102,24);
        VelaDraw.roundedRect(g,friendInputHit.x(),friendInputHit.y(),friendInputHit.width(),24,5,VelaTheme.SURFACE_DEEP);
        friendEdit.set(friendInput);dev.sqwat.vela.ui.render.TextFieldDraw.draw(g,friendEdit,new Rect(friendInputHit.x()+8,friendInputHit.y()+5,friendInputHit.width()-16,14),friendFocused,"Ник игрока");
        actionHit=dev.sqwat.vela.ui.render.ActionGeometry.hit(addButton,addProgress);
        primaryButton(g,addButton,"Добавить",addProgress);
        int top=addY+91,bottom=content.y()+content.height(),cw=(content.width()-8)/2;
        var friends=services.friends().all().stream().filter(f->search.isBlank()||f.name().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))).toList();
        dev.sqwat.vela.ui.render.UiClip.enable(g,content.x(),top,content.x()+content.width(),bottom);
        int index=0;
        for(var friend:friends){
            Rect card=animatedFriendCard(friend.id().toString(),new Rect(content.x()+(index%2)*(cw+8),top+(index/2)*48-Math.round(scroll),cw,40));index++;
            if(!rowIntersects(card,top,bottom))continue;
            float h=animate("friend-"+friend.id(),card.contains(pointerX,pointerY),frameDt);
            VelaDraw.roundedOutline(g,card.x(),card.y(),cw,40,7,VelaTheme.BORDER,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,h));
            dev.sqwat.vela.ui.hud.PlayerAvatars.draw(g,friend.name(),card.x()+8,card.y()+9,22);
            boolean online=minecraft.getConnection()!=null&&minecraft.getConnection().getOnlinePlayers().stream().anyMatch(info->info.getProfile().name().equalsIgnoreCase(friend.name()));
            dev.sqwat.vela.ui.render.SearchText.draw(g,VelaDraw.ellipsize(friend.name(),cw-83,VelaDraw.Weight.MEDIUM,9),search,card.x()+37,card.y()+9,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);
            VelaDraw.text(g,online?"В сети":"Не в сети",card.x()+37,card.y()+24,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
            Rect star=new Rect(card.x()+cw-42,card.y()+11,18,20),remove=new Rect(card.x()+cw-22,card.y()+11,18,20);
            controls.favorite(g,"friend-favorite-"+friend.id(),new Rect(star.x()+4,star.y()+5,10,10),friend.favorite(),star.contains(pointerX,pointerY));
            dev.sqwat.vela.ui.render.VelaIcons.draw(g,"trash",remove.x()+4,remove.y()+5,10,remove.contains(pointerX,pointerY)?VelaTheme.PRIMARY:VelaTheme.TEXT_MUTED);
            if(card.y()>=top&&card.y()+40<=bottom){friendHits.add(new FriendHit(friend.id(),star,true));friendHits.add(new FriendHit(friend.id(),remove,false));}
        }
        g.disableScissor();scrollLimit=Math.max(0,((friends.size()+1)/2)*48-(bottom-top));targetScroll=Math.min(targetScroll,scrollLimit);
    }
    private void addFriend(){if(services.friends().add(friendInput)){friendInput="";showToast("Друг добавлен");}else showToast("Введите новый корректный ник");}
    
    private String configKey="",configName="",renameSource;
    private boolean configNameFocused;
    private final dev.sqwat.vela.ui.input.TextEdit nameEdit=new dev.sqwat.vela.ui.input.TextEdit();
    private Rect configNameHit=new Rect(0,0,0,0);
    private boolean configKeyFocused;
    private Rect configKeyHit=new Rect(0,0,0,0),importHit=configKeyHit;
    private void drawConfigs(GuiGraphicsExtractor g,Rect content) {
        int blockY=content.y(),cx=content.x(),cw=content.width();
        configNameHit=configKeyHit=importHit=new Rect(0,0,0,0);
        VelaDraw.roundedOutline(g,cx,blockY,cw,43,9,VelaTheme.BORDER,VelaTheme.SURFACE);
        dev.sqwat.vela.ui.render.VelaIcons.draw(g,"configs",cx+11,blockY+10,19,VelaTheme.PRIMARY);
        VelaDraw.text(g,"Создание конфигурации",cx+38,blockY+10,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,10);
        VelaDraw.text(g,"Сохраняйте настройки и делитесь ими с друзьями",cx+38,blockY+26,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
        configOptionsHit=new Rect(cx+cw-35,blockY+10,23,23);
        drawerButton(g,configOptionsHit,"contact_options",configPanelOpen);
        configFolderHit=new Rect(cx+cw-63,blockY+10,23,23);
        drawerButton(g,configFolderHit,"folder",false);
        int listTop=blockY+55,y=listTop-Math.round(scroll);
        var profiles=services.configs().profiles().stream().filter(p->search.isBlank()||p.contains(search.toLowerCase(java.util.Locale.ROOT))).toList();
        dev.sqwat.vela.ui.render.UiClip.enable(g,content.x()-1,listTop,content.x()+content.width()+1,content.y()+content.height()-20);
        for(String name:profiles){
            boolean active=name.equalsIgnoreCase(services.configs().current().activeProfile);
            Rect card=new Rect(content.x(),y,content.width(),43);
            if(!rowIntersects(card,listTop,content.y()+content.height()-20)){y+=49;continue;}
            VelaDraw.roundedOutline(g,card.x(),card.y(),card.width(),43,7,VelaTheme.BORDER,active?VelaTheme.PRIMARY_SOFT:VelaTheme.SURFACE);
            if(name.equals(renameSource)){
                configNameHit=new Rect(card.x()+5,y+3,Math.max(40,card.width()-125),21);
                VelaDraw.roundedRect(g,configNameHit.x(),configNameHit.y(),configNameHit.width(),21,4,VelaTheme.SURFACE_DEEP);
                nameEdit.set(configName);dev.sqwat.vela.ui.render.TextFieldDraw.draw(g,nameEdit,new Rect(configNameHit.x()+8,configNameHit.y()+5,configNameHit.width()-16,14),configNameFocused,"Название");
            }else dev.sqwat.vela.ui.render.SearchText.draw(g,VelaDraw.ellipsize(titleCase(name),card.width()-125,VelaDraw.Weight.MEDIUM,9),search,card.x()+10,y+9,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);
            VelaDraw.text(g,savedLabel(name),card.x()+10,y+25,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
            profileHits.add(new ProfileHit(name,new Rect(card.x()+10,Math.max(y+3,listTop),Math.max(1,card.width()-130),Math.max(0,Math.min(y+23,content.y()+content.height()-20)-Math.max(y+3,listTop))),5));
            String[] icons={"copy","folder","trash","download","save"};
            for(int i=0;i<5;i++){
                Rect hit=new Rect(card.x()+card.width()-111+i*21,y+11,18,22);
                dev.sqwat.vela.ui.render.VelaIcons.draw(g,icons[i],hit.x()+3,hit.y()+5,12,hit.contains(pointerX,pointerY)?VelaTheme.PRIMARY:VelaTheme.TEXT_MUTED);
                if(hit.contains(pointerX,pointerY))showHint(g,new String[]{"Копировать ключ","Открыть папку","Удалить","Загрузить","Сохранить"}[i],hit.x(),hit.y()+23);
                final int action=i;
                if(rowIntersects(card,listTop,content.y()+content.height()-20))profileHits.add(new ProfileHit(name,hit,action));
            }
            y+=49;
        }
        g.disableScissor();
        scrollLimit=Math.max(0,profiles.size()*49-(content.y()+content.height()-20-listTop));targetScroll=Math.max(0,Math.min(targetScroll,scrollLimit));
    }

    private final Map<String,Long> savedTimes=new HashMap<>();
    private long savedTimesRefresh;
    private String savedLabel(String name){
        long now=System.currentTimeMillis();
        if(now>savedTimesRefresh){savedTimes.clear();for(String profile:services.configs().profiles())try{savedTimes.put(profile,java.nio.file.Files.getLastModifiedTime(services.configs().profilePath(profile)).toMillis());}catch(java.io.IOException ignored){}savedTimesRefresh=now+1000;}
        Long time=savedTimes.get(name);if(time==null)return "Время сохранения неизвестно";
        long minutes=Math.max(0,(now-time)/60000);return minutes==0?"Сохранено только что":minutes<60?dev.sqwat.vela.ui.Lang.tr("Сохранено ")+minutes+dev.sqwat.vela.ui.Lang.tr(" мин. назад"):minutes<1440?dev.sqwat.vela.ui.Lang.tr("Сохранено ")+minutes/60+dev.sqwat.vela.ui.Lang.tr(" ч. назад"):dev.sqwat.vela.ui.Lang.tr("Сохранено ")+minutes/1440+dev.sqwat.vela.ui.Lang.tr(" д. назад");
    }
    private void drawerButton(GuiGraphicsExtractor g,Rect rect,String icon,boolean open){controls.iconButton(g,"drawer:"+section+":"+icon,rect,icon,open,rect.contains(pointerX,pointerY));}
    private boolean sideOpen(){return section==VelaSection.MARKERS?markerPanel.settingsOpen():section==VelaSection.FRIENDS?friendSettings:section==VelaSection.CONFIGS&&configPanelOpen;}
    private int sidePanelHeight(){return section==VelaSection.MARKERS?markerPanel.settingsHeight():section==VelaSection.CONFIGS?244:166;}
    private void closeSide(){markerPanel.closeSettings();friendSettings=false;configPanelOpen=false;renameSource=null;configNameFocused=configKeyFocused=false;persistView();}
    private void drawSidePanel(GuiGraphicsExtractor g,Layout l){
        sideHit=sideCloseHit=new Rect(0,0,0,0);
        if(sideReveal<.005f||(section!=VelaSection.FRIENDS&&section!=VelaSection.CONFIGS&&section!=VelaSection.MARKERS))return;
        sideHit=dev.sqwat.vela.ui.render.SidePanelGeometry.place(width,height,new Rect(l.x,l.y,l.w,l.h),sidePanelHeight(),sideReveal);
        int pw=sideHit.width(),px=sideHit.x(),py=sideHit.y(),ph=sideHit.height();
        dev.sqwat.vela.ui.render.UiClip.enable(g,px-1,py-1,px+Math.round(pw*sideReveal)+1,py+ph+1);
        VelaDraw.roundedOutline(g,px,py,pw,ph,9,VelaTheme.BORDER,VelaTheme.SURFACE);
        VelaDraw.roundedRect(g,px+12,py+14,3,14,1,VelaTheme.PRIMARY);
        VelaDraw.text(g,section==VelaSection.MARKERS?"Настройки меток":section==VelaSection.FRIENDS?"Настройки друзей":"Конфигурации",px+23,py+16,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);
        sideCloseHit=new Rect(px+pw-29,py+9,22,22);
        drawerButton(g,sideCloseHit,"x",false);
        VelaDraw.roundedRect(g,px+12,py+39,pw-24,1,0,VelaTheme.BORDER);
        if(section==VelaSection.MARKERS){sideScrollTarget=Math.min(sideScrollTarget,Math.max(0,sidePanelHeight()-ph));dev.sqwat.vela.ui.render.UiClip.enable(g,px,py+43,px+pw,py+ph-1);markerPanel.drawSettings(g,new Rect(px+4,py+43-Math.round(sideScroll),pw-8,ph-47),pointerX,pointerY);g.disableScissor();g.disableScissor();return;}
        int maxScroll=Math.max(0,sidePanelHeight()-ph);
        sideScrollTarget=Math.min(sideScrollTarget,maxScroll);
        dev.sqwat.vela.ui.render.UiClip.enable(g,px,py+43,px+pw,py+ph-1);
        py-=Math.round(sideScroll);
        if(section==VelaSection.FRIENDS){
            var cfg=services.configs().current();
            friendVisualHit=new Rect(px+10,py+116,pw-20,38);
            VelaDraw.roundedRect(g,px+10,py+50,pw-20,58,6,VelaTheme.SURFACE_DEEP);
            VelaDraw.text(g,"Визуалы друзей",px+19,py+60,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
            VelaDraw.roundedRect(g,px+10,py+116,pw-20,38,6,VelaTheme.SURFACE_DEEP);
            VelaDraw.text(g,"Не атаковать друзей",px+19,py+130,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
            VelaDraw.toggle(g,"friend-protection",px+pw-37,py+129,cfg.protectFriends,friendVisualHit.contains(pointerX,pointerY));
            friendEffects.flags().get(0).set(cfg.friendChinaHat);
            friendEffects.flags().get(1).set(cfg.friendDashTrail);
            friendEffects.flags().get(2).set(cfg.friendJumpCircle);
            friendEffectsHit=new Rect(px+16,py+79,pw-32,21);
            VelaDraw.roundedRect(g,friendEffectsHit.x(),friendEffectsHit.y(),friendEffectsHit.width(),21,5,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,.35f));
            String label=friendEffects.label();
            dev.sqwat.vela.ui.render.VelaIcons.draw(g,"contact_options",px+22,py+84,11,VelaTheme.TEXT_MUTED);
            VelaDraw.text(g,VelaDraw.ellipsize(label.isBlank()?"Не выбраны":label,pw-80,VelaDraw.Weight.REGULAR,8),px+39,py+85,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
            g.pose().pushMatrix();g.pose().translate(px+pw-28,py+89);g.pose().rotate((float)Math.PI*controls.animate("choice-arrow-friends",choice==friendEffects?1:0));
            dev.sqwat.vela.ui.render.VelaIcons.draw(g,"chevron",-5,-5,10,VelaTheme.TEXT_MUTED);g.pose().popMatrix();
            if(choice==friendEffects)popupRect=new Rect(px+16,Math.max(6,Math.min(height-74,py+103)),pw-32,68);
        }else{
            dev.sqwat.vela.ui.render.PanelChrome.group(new dev.sqwat.vela.ui.render.GameUiCanvas(g),new Rect(px+7,py+46,pw-14,96),renameSource==null?"Новый профиль":"Переименование","Модули, оформление и расположение HUD");
            dev.sqwat.vela.ui.render.PanelChrome.group(new dev.sqwat.vela.ui.render.GameUiCanvas(g),new Rect(px+7,py+147,pw-14,90),"Импорт по ключу","Применить сохранённые настройки");
            configNameHit=new Rect(px+12,py+86,pw-24,23);
            configKeyHit=new Rect(px+12,py+179,pw-24,23);
            for(Rect field:new Rect[]{configNameHit,configKeyHit})VelaDraw.roundedRect(g,field.x(),field.y(),field.width(),field.height(),5,VelaTheme.SURFACE_DEEP);
            nameEdit.set(configName);dev.sqwat.vela.ui.render.TextFieldDraw.draw(g,nameEdit,new Rect(configNameHit.x()+8,configNameHit.y()+5,configNameHit.width()-16,14),configNameFocused,"Название профиля");
            keyEdit.set(configKey);dev.sqwat.vela.ui.render.TextFieldDraw.draw(g,keyEdit,new Rect(configKeyHit.x()+8,configKeyHit.y()+5,configKeyHit.width()-16,14),configKeyFocused,"Вставьте ключ");
            actionHit=primaryButton(g,new Rect(px+12,py+115,88,23),renameSource==null?"Создать":"Сохранить");
            importHit=primaryButton(g,new Rect(px+12,py+208,72,23),"Импорт");
        }
        g.disableScissor();
        g.disableScissor();
    }

    private void drawToast(GuiGraphicsExtractor g, Layout l) {
    }

    @Override
    public boolean clickVela(MouseButtonEvent event, boolean doubled) {
        if(editingText!=null){
            boolean inside=false;
            for(var hit:settingHits)if(hit.setting()==editingText&&hit.visible().contains(event.x(),event.y())){inside=true;break;}
            if(!inside)finishTextSetting();
        }
        if(capturingSetting!=null){capturingSetting.set(dev.sqwat.vela.ui.input.BindInput.mouse(event.button()));capturingSetting=null;services.configs().requestSave();return true;}
        if(section==VelaSection.MARKERS&&markerPanel.captureMouse(event.button()))return true;
        if(binding!=null&&capturing){draftKey=dev.sqwat.vela.ui.input.BindInput.mouse(event.button());draftLabel=dev.sqwat.vela.ui.input.KeyLabels.of(draftKey,"");capturing=false;saveBind();return true;}
        double mx = event.x(), my = event.y();
        if(rgbPicker.open()){if(event.button()==0&&rgbPicker.click(mx,my))services.configs().requestSave();return true;}
        if(event.button()==0&&section==VelaSection.FRIENDS&&friendFocused){for(var hit:suggestionHits.entrySet())if(hit.getKey().contains(mx,my)){friendInput=hit.getValue();friendEdit.set(friendInput);suggestionHits.clear();return true;}}
        if(section==VelaSection.MARKERS&&event.button()==0&&contentHit.contains(mx,my)&&markerPanel.click(mx,my)){searchFocused=false;return true;}
        if (binding != null || choice != null || contextModule!=null) {
            if(choice!=null&&event.button()==0){
                for(var hit:settingHits)if(hit.setting()==choice&&hit.visible().contains(mx,my)){choice=null;popupHits.clear();return true;}
            }
            for (PopupHit hit : popupHits) if (event.button()==0 && hit.rect.contains(mx,my)) { hit.action.run();dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.TOGGLE); return true; }
            if(choice==null&&popupRect.contains(mx,my))return true;
            binding=null;choice=null;contextModule=null;capturing=false;return true;
        }
        if (event.button()==GLFW.GLFW_MOUSE_BUTTON_MIDDLE && section.category()!=null && moduleContentHit.contains(mx,my)) {
            for(CardHit hit:cardHits) if(hit.card.contains(mx,my)) {
                binding=hit.module; choice=null; contextModule=null;hover.put("bind-entrance",0f);
                draftKey=binding.boundKey();draftLabel=dev.sqwat.vela.ui.input.KeyLabels.of(draftKey,binding.keybind());draftMode=binding.bindMode();capturing=true;
                searchFocused=false;return true;
            }
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.clickVela(event, doubled);
        if(sideHit.contains(mx,my)){
            if(!sideOpen()||sideReveal<.95f)return true;
            if(sideCloseHit.contains(mx,my)){closeSide();return true;}
            if(my<sideHit.y()+43)return true;
            if(section==VelaSection.MARKERS){markerPanel.click(mx,my);return true;}
            if(my<sideHit.y()+43)return true;
            if(section==VelaSection.FRIENDS){
                var cfg=services.configs().current();
                if(friendEffectsHit.contains(mx,my)){
                    choice=friendEffects;choiceScroll=choiceScrollTarget=0;binding=null;hover.put("choice-open",0f);return true;
                }
                if(friendVisualHit.contains(mx,my)){
                    cfg.protectFriends=!cfg.protectFriends;
                    services.configs().requestSave();
                    dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.TOGGLE);
                }
                return true;
            }
            configNameFocused=configNameHit.contains(mx,my);configKeyFocused=configKeyHit.contains(mx,my);searchFocused=friendFocused=false;
            if(configNameFocused||configKeyFocused){
                var edit=configNameFocused?nameEdit:keyEdit;var field=configNameFocused?configNameHit:configKeyHit;
                edit.set(configNameFocused?configName:configKey);
                dev.sqwat.vela.ui.render.TextFieldDraw.click(edit,new Rect(field.x()+8,field.y()+5,field.width()-16,14),mx,false);
                if(doubled)edit.selectWord();return true;
            }
            if(actionHit.contains(mx,my)){createConfig();return true;}
            if(importHit.contains(mx,my)){try{String imported=services.configs().importProfile(configKey);configKey="";showToast(dev.sqwat.vela.ui.Lang.tr("Активирован ")+imported);}catch(Exception error){showToast(error.getMessage());}return true;}
            return true;
        }
        if(section==VelaSection.CONFIGS&&configFolderHit.contains(mx,my)){net.minecraft.util.Util.getPlatform().openUri(services.configs().profilesDirectory().toUri());return true;}
        if(section==VelaSection.CONFIGS&&configOptionsHit.contains(mx,my)){configPanelOpen=!configPanelOpen;configNameFocused=configKeyFocused=false;dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.EXPAND);return true;}
        configNameFocused=configKeyFocused=false;
        if(section==VelaSection.HUD&&dragModeHit.contains(mx,my)){minecraft.gui.setScreen(new HudPositionScreen(this,services.hudLayout()));return true;}
        if(gearHit.contains(mx,my)){dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.EXPAND);minecraft.gui.setScreen(new VelaSettingsScreen(this,services));return true;}
        for (Map.Entry<VelaSection, Rect> entry : navHits.entrySet()) {
            if (entry.getValue().contains(mx, my)) {
                section = entry.getKey();sideReveal=sideScroll=sideScrollTarget=0;friendFocused=false;configKeyFocused=false;configNameFocused=false;
                dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.TAB);
                searchFocused = false;
                targetScroll = scroll = 0;
                persistView();
                return true;
            }
        }
        friendFocused=section==VelaSection.FRIENDS&&friendInputHit.contains(mx,my);
        if(friendFocused){searchFocused=configKeyFocused=configNameFocused=false;friendEdit.set(friendInput);dev.sqwat.vela.ui.render.TextFieldDraw.click(friendEdit,new Rect(friendInputHit.x()+8,friendInputHit.y()+5,friendInputHit.width()-16,14),mx,false);if(doubled)friendEdit.selectAll();return true;}
        if(section==VelaSection.FRIENDS&&friendOptionsHit.contains(mx,my)){friendSettings=!friendSettings;persistView();dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.EXPAND);return true;}
        searchFocused = searchHit.contains(mx, my);
        if (searchFocused){configKeyFocused=configNameFocused=friendFocused=false;searchEdit.set(search);dev.sqwat.vela.ui.render.TextFieldDraw.click(searchEdit,new Rect(searchHit.x()+20,searchHit.y()+4,searchHit.width()-26,14),mx,false);if(doubled)searchEdit.selectAll();return true;}
        if (!contentHit.contains(mx,my)) return true;
        if(section==VelaSection.HUD&&dragModeHit.contains(mx,my)){minecraft.gui.setScreen(new HudPositionScreen(this,services.hudLayout()));return true;}
        if (section.category() != null) {
            if(!moduleContentHit.contains(mx,my))return true;
            for(var location:locationHits.entrySet())if(location.getKey().contains(mx,my)){jumpTo(location.getValue());return true;}
            for(var favorite:favoriteHits.entrySet())if(favorite.getKey().contains(mx,my)){var f=services.configs().current().favorites;if(!f.add(favorite.getValue()))f.remove(favorite.getValue());moduleReorderUntil=System.nanoTime()+400_000_000L;services.configs().requestSave();dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.FAVORITE);return true;}
            for (SettingHit hit : settingHits) {
                if (!hit.visible().contains(mx, my)) continue;
                dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.TOGGLE);
                if(hit.setting() instanceof dev.sqwat.vela.module.setting.ActionSetting action){
                    if(action.run())return true;
                    if(services.modules().byId("shaders") instanceof dev.sqwat.vela.module.visual.Shaders shaders&&shaders.settings().contains(hit.setting())){
                        minecraft.gui.setScreen(new ShaderEditorScreen(this,services,shaders));return true;
                    }
                    if(services.modules().byId("view_model") instanceof dev.sqwat.vela.module.visual.ViewModel model&&model.settings().contains(hit.setting())){
                        minecraft.gui.setScreen(new ViewModelEditorScreen(this,services,model));return true;
                    }
                    for(var module:services.modules().all())if(module instanceof dev.sqwat.vela.module.utility.CommandModule commands&&module.settings().contains(hit.setting())){
                        minecraft.gui.setScreen(new CommandEditorScreen(this,services,commands.interval()));break;
                    }
                }
                else if(hit.setting() instanceof dev.sqwat.vela.module.setting.TextSetting text){
                    if(editingText!=text)settingTextEdit.set(text.value());editingText=text;searchFocused=friendFocused=configNameFocused=configKeyFocused=false;
                    var inner=textSettingHits.get(text);if(inner!=null)dev.sqwat.vela.ui.render.TextFieldDraw.click(settingTextEdit,inner,mx,false);
                    if(doubled)settingTextEdit.selectAll();return true;
                }
                else if(hit.setting() instanceof dev.sqwat.vela.module.setting.ColorSetting color){rgbPicker.show(color,hit.rect(),width,height);}
                else if(hit.setting() instanceof NumberSetting number){dragging=number;dragTrack=hit.rect;dragging.setFraction((mx-dragTrack.x())/dragTrack.width());}
                else if(hit.setting() instanceof dev.sqwat.vela.module.setting.KeySetting key){capturingSetting=key;searchFocused=false;}
                else if(hit.setting() instanceof ChoiceSetting select){choice=select;choiceScroll=choiceScrollTarget=0;binding=null;hover.put("choice-open",0f);hover.put("inline-choice",0f);}
                else if(hit.setting() instanceof BooleanSetting toggle){
                    toggle.toggle();
                }
                services.configs().requestSave();
                return true;
            }
            for (CardHit hit : cardHits) {
                if (hit.toggle().contains(mx, my)) {
                    hit.module().toggle();
                    dev.sqwat.vela.ui.UiSounds.play(services,hit.module().enabled());
                    services.configs().requestSave();
                    return true;
                }
                if (hit.card().contains(mx, my) && my<hit.card().y()+44 && !visibleSettings(hit.module()).isEmpty()) {
                    dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.EXPAND);
                    if(!expandedModules.add(hit.module().id()))expandedModules.remove(hit.module().id());persistView();
                    return true;
                }
            }
        } else if (actionHit.contains(mx, my)) {
            if(section==VelaSection.FRIENDS)addFriend();
            else if(section==VelaSection.CONFIGS)createConfig();
            return true;
        }
        if(section==VelaSection.HUD&&dragModeHit.contains(mx,my)){minecraft.gui.setScreen(new HudPositionScreen(this,services.hudLayout()));return true;}
        if (section == VelaSection.FRIENDS) for(FriendHit hit:friendHits) if(hit.rect.contains(mx,my)){if(hit.favorite)services.friends().toggleFavorite(hit.id);else services.friends().remove(hit.id);dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.FAVORITE);return true;}
        if (section == VelaSection.CONFIGS) {
            configNameFocused=configNameHit.contains(mx,my);
            if(configNameFocused){configKeyFocused=false;searchFocused=false;nameEdit.set(configName);dev.sqwat.vela.ui.render.TextFieldDraw.click(nameEdit,new Rect(configNameHit.x()+8,configNameHit.y()+5,configNameHit.width()-16,14),mx,false);return true;}
            configKeyFocused=configKeyHit.contains(mx,my);
            if(configKeyFocused){searchFocused=false;keyEdit.set(configKey);dev.sqwat.vela.ui.render.TextFieldDraw.click(keyEdit,new Rect(configKeyHit.x()+8,configKeyHit.y()+6,configKeyHit.width()-16,14),mx,false);if(doubled)keyEdit.selectAll();return true;}
            if(importHit.contains(mx,my)){try{String imported=services.configs().importProfile(configKey);configKey="";showToast(dev.sqwat.vela.ui.Lang.tr("Активирован ")+imported);}catch(Exception error){showToast(error.getMessage());}return true;}
            for (ProfileHit hit : profileHits) {
                if (hit.rect().contains(mx, my)) {
                    try {
                        if(hit.action()==5){if(doubled){renameSource=hit.name();configName=hit.name();nameEdit.set(configName);nameEdit.selectAll();configPanelOpen=false;configNameFocused=true;configKeyFocused=searchFocused=false;}return true;}
                        if(hit.action()==0){minecraft.keyboardHandler.setClipboard(services.configs().shareProfile(hit.name()));showToast("Ключ конфигурации скопирован");}
                        else if(hit.action()==1){net.minecraft.util.Util.getPlatform().openUri(services.configs().profilesDirectory().toUri());}
                        else if(hit.action()==3){services.configs().selectProfile(hit.name());showToast("Профиль загружен");}
                        else if(hit.action()==4){services.configs().saveToProfile(hit.name());showToast("Профиль сохранён");}
                        else minecraft.gui.setScreen(new VelaConfirmScreen(this,hit.name(),()->{try{services.configs().archiveProfile(hit.name());showToast("Конфигурация удалена");return "";}catch(java.io.IOException error){return error.getMessage();}}));
                    }catch(java.io.IOException error){showToast(error.getMessage());}
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean scrollVela(double mouseX, double mouseY, double horizontal, double vertical) {
        if(rgbPicker.open())return true;
        if(friendFocused&&suggestionPanel.contains(mouseX,mouseY)){suggestionOffset=Math.clamp(suggestionOffset-(int)Math.signum(vertical),0,Math.max(0,suggestions.size()-5));return true;}
        if(choice!=null){if(popupRect.contains(mouseX,mouseY))choiceScrollTarget=Math.clamp(choiceScrollTarget-(float)vertical*20,0,Math.max(0,choice.options().size()*20+8-popupRect.height()));return true;}
        if(sideHit.contains(mouseX,mouseY)){
            int maximum=Math.max(0,sidePanelHeight()-sideHit.height());
            sideScrollTarget=Math.max(0,Math.min(maximum,sideScrollTarget-(float)vertical*20));return true;
        }
        if(dragging!=null || binding!=null || contextModule!=null || choice!=null || !contentHit.contains(mouseX,mouseY))return true;
        targetScroll = Math.max(0, Math.min(scrollLimit, targetScroll - (float) vertical * 34));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if(editingText!=null){
            if(event.key()==GLFW.GLFW_KEY_ENTER||event.key()==GLFW.GLFW_KEY_ESCAPE){finishTextSetting();return true;}
            dev.sqwat.vela.ui.input.GameTextInput.key(settingTextEdit,event,24);return true;
        }
        if(capturingSetting!=null){if(event.key()!=GLFW.GLFW_KEY_ESCAPE){capturingSetting.set(dev.sqwat.vela.ui.input.BindInput.clears(event.key())?-1:event.key());services.configs().requestSave();}capturingSetting=null;return true;}
        if(rgbPicker.open()){if(event.key()==GLFW.GLFW_KEY_ESCAPE)rgbPicker.close();return true;}
        if(friendFocused&&section==VelaSection.FRIENDS&&!suggestions.isEmpty()){
            if(event.key()==GLFW.GLFW_KEY_DOWN||event.key()==GLFW.GLFW_KEY_UP){suggestionSelection=Math.clamp(suggestionSelection+(event.key()==GLFW.GLFW_KEY_DOWN?1:-1),0,suggestions.size()-1);suggestionOffset=Math.clamp(suggestionOffset,Math.max(0,suggestionSelection-4),suggestionSelection);return true;}
            if(event.key()==GLFW.GLFW_KEY_TAB||event.key()==GLFW.GLFW_KEY_ENTER&&suggestionSelection>=0){friendInput=suggestions.get(Math.clamp(suggestionSelection,0,suggestions.size()-1));friendEdit.set(friendInput);suggestions=java.util.List.of();suggestionSelection=-1;return true;}
        }
        if(section==VelaSection.MARKERS&&markerPanel.clearBinding(event))return true;
        if(section==VelaSection.MARKERS&&!searchFocused&&markerPanel.key(event))return true;
        if(event.key()==GLFW.GLFW_KEY_ESCAPE&&sideOpen()&&binding==null&&choice==null){closeSide();return true;}
        if(configNameFocused&&event.key()==GLFW.GLFW_KEY_ENTER){createConfig();return true;}
        if(configNameFocused&&event.key()==GLFW.GLFW_KEY_ESCAPE){configNameFocused=false;if(renameSource!=null){renameSource=null;configName="";}return true;}
        if(binding==null&&choice==null&&(searchFocused||configKeyFocused||configNameFocused||friendFocused)){
            var edit=friendFocused?friendEdit:searchFocused?searchEdit:configNameFocused?nameEdit:keyEdit;edit.set(friendFocused?friendInput:searchFocused?search:configNameFocused?configName:configKey);
            boolean handled=dev.sqwat.vela.ui.input.GameTextInput.key(edit,event,friendFocused?16:searchFocused?256:configNameFocused?32:90000);
            if(handled){if(friendFocused)friendInput=edit.text();else if(searchFocused)search=edit.text();else if(configNameFocused)configName=edit.text();else configKey=edit.text();return true;}
        }
        if(friendFocused){
            boolean ctrl=(event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0;
            if(ctrl&&event.key()==services.configs().current().searchKey){friendFocused=false;searchFocused=true;return true;}
            if(event.key()==GLFW.GLFW_KEY_ESCAPE){friendFocused=false;return true;}
            if(event.key()==GLFW.GLFW_KEY_ENTER){addFriend();return true;}
            if(event.key()==GLFW.GLFW_KEY_BACKSPACE&&!friendInput.isEmpty())friendInput=friendInput.substring(0,friendInput.length()-1);
            if(ctrl&&event.key()==GLFW.GLFW_KEY_A)friendInput="";
            if(ctrl&&event.key()==GLFW.GLFW_KEY_V){friendInput+=minecraft.keyboardHandler.getClipboard().replaceAll("[^A-Za-z0-9_]","");friendInput=friendInput.substring(0,Math.min(16,friendInput.length()));}
            return true;
        }
        if(contextModule!=null){if(event.key()==GLFW.GLFW_KEY_ESCAPE)contextModule=null;return true;}
        if(section==VelaSection.CONFIGS&&configKeyFocused){
            if(event.key()==GLFW.GLFW_KEY_ESCAPE){configKeyFocused=false;return true;}
            if((event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0&&event.key()==GLFW.GLFW_KEY_A){configKey="";return true;}
            if(event.key()==GLFW.GLFW_KEY_BACKSPACE)configKey=configKey.isEmpty()?"":configKey.substring(0,configKey.length()-1);
            if((event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0&&event.key()==GLFW.GLFW_KEY_V){String pasted=minecraft.keyboardHandler.getClipboard().strip();if(pasted.length()<=90000)configKey=pasted;else showToast("Ключ слишком длинный");}
            return true;
        }
        if(binding!=null){
            if(event.key()==GLFW.GLFW_KEY_ESCAPE){capturing=false;binding=null;return true;}
            if(event.key()==GLFW.GLFW_KEY_DELETE){draftKey=-1;draftLabel="None";saveBind();capturing=false;return true;}
            if(capturing){draftKey=event.key();draftLabel=dev.sqwat.vela.ui.input.KeyLabels.of(draftKey,GLFW.glfwGetKeyName(draftKey,0));capturing=false;saveBind();}
            return true;
        }
        if((event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0&&event.key()==services.configs().current().searchKey){searchFocused=true;return true;}
        if(choice!=null){if(event.key()==GLFW.GLFW_KEY_ESCAPE)choice=null;return true;}
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (searchFocused) { searchFocused = false; return true; }
            onClose();
            return true;
        }
        if (searchFocused) {
            if((event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0&&event.key()==GLFW.GLFW_KEY_A){search="";return true;}
            if((event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0&&event.key()==GLFW.GLFW_KEY_V){search=(search+minecraft.keyboardHandler.getClipboard().replaceAll("[\\p{Cntrl}]",""));if(search.length()>64)search=search.substring(0,64);return true;}
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) search = search.substring(0, search.offsetByCodePoints(search.length(), -1));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if(editingText!=null){if(event.isAllowedChatCharacter())settingTextEdit.insert(event.codepointAsString(),24);return true;}
        if(section==VelaSection.MARKERS&&!searchFocused&&event.isAllowedChatCharacter()&&markerPanel.character(event.codepointAsString()))return true;
        if((searchFocused||configKeyFocused||configNameFocused||friendFocused)&&event.isAllowedChatCharacter()){var edit=friendFocused?friendEdit:searchFocused?searchEdit:configNameFocused?nameEdit:keyEdit;edit.set(friendFocused?friendInput:searchFocused?search:configNameFocused?configName:configKey);edit.insert(event.codepointAsString(),friendFocused?16:searchFocused?256:configNameFocused?32:90000);if(friendFocused)friendInput=edit.text();else if(searchFocused)search=edit.text();else if(configNameFocused)configName=edit.text();else configKey=edit.text();return true;}
        if(section==VelaSection.CONFIGS&&configKeyFocused){if(event.isAllowedChatCharacter()&&configKey.length()<90000)configKey+=event.codepointAsString();return true;}
        if(friendFocused){if(event.isAllowedChatCharacter()&&friendInput.length()<16&&event.codepointAsString().matches("[A-Za-z0-9_]"))friendInput+=event.codepointAsString();return true;}
        if (searchFocused && event.isAllowedChatCharacter() && search.codePointCount(0, search.length()) < 32) {
            search += event.codepointAsString();
            return true;
        }
        return super.charTyped(event);
    }

    public boolean hasModal(){return rgbPicker.open()||binding!=null||contextModule!=null||choice!=null;}
    @Override public void onClose() { finishTextSetting();persistView(); dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.CLOSE); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private Layout layout() {
        var l=dev.sqwat.vela.ui.render.DashboardChrome.layout(width,height);
        return new Layout(l.x(),l.y(),l.width(),l.height(),l.sidebar());
    }
    @Override protected float interfaceScale(){return services.configs().current().uiScale;}

    private boolean reducedMotion() { return services.configs().current().reducedMotion || !services.configs().current().animations; }
    private float animate(String key, boolean active, float dt) {
        float current = hover.getOrDefault(key, 0f);
        float next = Motion.approach(current, active ? 1 : 0, reducedMotion() ? 60 : 10.8f, dt);
        hover.put(key, next);
        return next;
    }
    private void createConfig(){
        String name=configName.strip();
        if(!name.matches("[A-Za-z0-9_-]{1,32}")){showToast("Имя: латиница, цифры, _ и -");return;}
        if(renameSource!=null){try{services.configs().renameProfile(renameSource,name);renameSource=null;configName="";nameEdit.set("");savedTimesRefresh=0;showToast(dev.sqwat.vela.ui.Lang.tr("Конфиг переименован · ")+name);}catch(java.io.IOException error){showToast(error.getMessage());}return;}
        if(services.configs().profiles().contains(name.toLowerCase(java.util.Locale.ROOT))){showToast("Такой конфиг уже есть");return;}
        services.configs().createProfile(name);configName="";nameEdit.set("");showToast(dev.sqwat.vela.ui.Lang.tr("Конфиг создан · ")+name);
    }
    private void showToast(String message) {String[] lines=message.split(" · ",2);dev.sqwat.vela.ui.Notifications.show(lines[0],lines.length>1?lines[1]:"",section==VelaSection.CONFIGS?"configs":section==VelaSection.FRIENDS?"friends":"settings");}
    private Rect primaryButton(GuiGraphicsExtractor g, Rect rect, String label) {
        float progress=actionProgress(rect,label);
        primaryButton(g,rect,label,progress);
        return dev.sqwat.vela.ui.render.ActionGeometry.hit(rect,progress);
    }
    private float actionProgress(Rect rect,String label) {
        String key="action-"+label+"-"+rect.x()+"-"+rect.y();
        Rect visible=dev.sqwat.vela.ui.render.ActionGeometry.hit(rect,hover.getOrDefault(key,0f));
        return animate(key,visible.contains(pointerX,pointerY),frameDt);
    }
    private void primaryButton(GuiGraphicsExtractor g,Rect rect,String label,float h){dev.sqwat.vela.ui.render.UiControls.drawAction(g,rect,label,h);}
    private void showHint(GuiGraphicsExtractor g,String text,int x,int y){int w=Math.round(VelaDraw.textWidth(text,VelaDraw.Weight.REGULAR,7))+12;x=Math.min(x,contentHit.x()+contentHit.width()-w);VelaDraw.roundedRect(g,x,y,w,18,4,VelaTheme.SURFACE_DEEP);VelaDraw.text(g,text,x+6,y+5,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7);}
    private void drawEmpty(GuiGraphicsExtractor g, Rect area, String label) {
        dev.sqwat.vela.ui.render.DashboardChrome.empty(new dev.sqwat.vela.ui.render.GameUiCanvas(g),area,
            label.contains("модулей")?"Пока нет модулей":label,
            label.contains("модулей")?"Добавленные функции появятся в этом разделе":"Попробуйте изменить поисковый запрос");
    }
    private static void drawSearchIcon(GuiGraphicsExtractor g, int x, int y, int color) {
        g.outline(x, y, 7, 7, color);
        g.fill(x + 6, y + 6, x + 10, y + 7, color);
    }
    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
    private static String ellipsis(String value, int max) { return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…"; }
    private static String titleCase(String value) { return value.isBlank() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1); }
    private static String format(double value) { return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.2f", value); }
    private static boolean rowIntersects(Rect rect, int top, int bottom) { return rect.y() + rect.height() >= top && rect.y() <= bottom; }
    private static void cycleSetting(Setting<?> setting) {
        if (setting instanceof BooleanSetting value) value.toggle();
        else if (setting instanceof NumberSetting value) {
            double next = value.value() + value.step();
            value.set(next > value.max() ? value.min() : next);
        } else if (setting instanceof ChoiceSetting value) {
            int current = value.options().indexOf(value.value());
            value.set(value.options().get((current + 1) % value.options().size()));
        }
    }


    @Override public boolean dragVela(MouseButtonEvent event,double dx,double dy) {
        if(rgbPicker.open()){if(event.button()==0&&rgbPicker.drag(event.x(),event.y()))services.configs().requestSave();return true;}
        if(section==VelaSection.MARKERS&&markerPanel.drag(event.x(),event.y()))return true;
        if(event.button()==0&&(searchFocused||configKeyFocused||configNameFocused||friendFocused)){
            var edit=friendFocused?friendEdit:searchFocused?searchEdit:configNameFocused?nameEdit:keyEdit;
            Rect r=friendFocused?new Rect(friendInputHit.x()+8,friendInputHit.y()+5,friendInputHit.width()-16,14):searchFocused?new Rect(searchHit.x()+20,searchHit.y()+4,searchHit.width()-26,14):configNameFocused?new Rect(configNameHit.x()+8,configNameHit.y()+5,configNameHit.width()-16,14):new Rect(configKeyHit.x()+8,configKeyHit.y()+5,configKeyHit.width()-16,14);
            dev.sqwat.vela.ui.render.TextFieldDraw.click(edit,r,event.x(),true);return true;
        }
        if(dragging!=null && event.button()==0) {
            dragging.setFraction((event.x()-dragTrack.x())/dragTrack.width());services.configs().requestSave();dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.SLIDER);return true;
        }
        return super.dragVela(event,dx,dy);
    }
    @Override public boolean releaseVela(MouseButtonEvent event) {
        rgbPicker.release();
        markerPanel.release();
        if(dragging!=null){dragging=null;services.configs().requestSave();return true;}return super.releaseVela(event);
    }
    private void clippedText(GuiGraphicsExtractor g,String text,int x,int y,int max,int color,VelaDraw.Weight weight) {
        dev.sqwat.vela.ui.render.UiClip.enable(g,x,y-1,x+Math.max(1,max),y+14);VelaDraw.text(g,font,text,x,y,color,weight);g.disableScissor();
    }
    private void drawPopup(GuiGraphicsExtractor g,int mx,int my) {
        if(rgbPicker.open()){rgbPicker.draw(g,frameDt);return;}
        if(choice!=null){
            g.nextStratum();popupHits.clear();
            float reveal=animate("choice-open",true,frameDt);
            dev.sqwat.vela.ui.render.UiClip.enable(g,popupRect.x()-1,popupRect.y()-1,popupRect.x()+popupRect.width()+1,popupRect.y()+Math.round(popupRect.height()*reveal)+1);
            VelaDraw.roundedRect(g,popupRect.x(),popupRect.y(),popupRect.width(),popupRect.height(),7,VelaTheme.SURFACE);
            var select=choice;
            choiceScroll=dev.sqwat.vela.ui.animation.Motion.approach(choiceScroll,choiceScrollTarget,24,frameDt);
            dev.sqwat.vela.ui.render.UiClip.enable(g,popupRect.x()+3,popupRect.y()+4,popupRect.x()+popupRect.width()-3,popupRect.y()+popupRect.height()-4);
            int y=popupRect.y()+4-Math.round(choiceScroll);
            for(String option:select.options()){
                Rect row=new Rect(popupRect.x()+4,y,popupRect.width()-8,18);
                popupRow(g,row,option,mx,my,()->{
                    if(select instanceof BooleanGroup group)group.toggle(option);else if(groups.isMetrics(select))groups.metric(option).toggle();else select.set(option);
                    if(select==friendEffects){var cfg=services.configs().current();cfg.friendChinaHat=friendEffects.flags().get(0).value();cfg.friendDashTrail=friendEffects.flags().get(1).value();cfg.friendJumpCircle=friendEffects.flags().get(2).value();}
                    services.configs().requestSave();
                });y+=20;
            }g.disableScissor();
            popupHits.removeIf(hit->hit.rect.y()<popupRect.y()+4||hit.rect.y()+hit.rect.height()>popupRect.y()+popupRect.height()-4);
            if(select.options().size()*20>popupRect.height()-8){
                int available=popupRect.height()-8,thumb=Math.max(12,available*available/(select.options().size()*20));
                int offset=Math.round((available-thumb)*choiceScroll/Math.max(1,select.options().size()*20-available));
                VelaDraw.roundedRect(g,popupRect.x()+popupRect.width()-3,popupRect.y()+4+offset,2,thumb,1,VelaTheme.TEXT_MUTED);
            }
            g.disableScissor();if(reveal<.95f)popupHits.clear();return;
        }
        popupHits.clear();if(binding==null&&contextModule==null)return;g.nextStratum();
        if(binding!=null){
            float lift=1-animate("bind-entrance",true,frameDt);
            g.fill(0,0,width,height,(Math.round(184*(1-lift))<<24)|0x05070A);popupRect=new Rect((width-176)/2,(height-128)/2+Math.round(lift*6),176,128);
            dev.sqwat.vela.ui.render.UiClip.enable(g,popupRect.x()-1,popupRect.y()-1,popupRect.x()+177,popupRect.y()+Math.round(129*(1-lift)));
            VelaDraw.roundedOutline(g,popupRect.x(),popupRect.y(),176,128,8,VelaTheme.BORDER,VelaTheme.SURFACE);
            int x=popupRect.x()+13,y=popupRect.y()+12;
            VelaDraw.text(g,"Бинд",x,y,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,10);
            Rect input=new Rect(x,y+24,150,29);
            popupButton(g,input,capturing?"Нажмите клавишу…":draftKey<0?"Назначить":draftLabel,()->capturing=true,capturing);
            VelaDraw.text(g,"Режим",x,y+64,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
            popupButton(g,new Rect(x,y+78,73,25),"Toggle",()->{draftMode=ClientModule.BindMode.TOGGLE;saveBind();},draftMode==ClientModule.BindMode.TOGGLE);
            popupButton(g,new Rect(x+77,y+78,73,25),"Hold",()->{draftMode=ClientModule.BindMode.HOLD;saveBind();},draftMode==ClientModule.BindMode.HOLD);
            g.disableScissor();if(lift>.05f)popupHits.clear();
            return;
        }
        float reveal=choice==null?1:animate("choice-open",true,frameDt);
        dev.sqwat.vela.ui.render.UiClip.enable(g,popupRect.x()-1,popupRect.y()-1,popupRect.x()+popupRect.width()+1,popupRect.y()+Math.round(popupRect.height()*reveal)+1);
        VelaDraw.roundedOutline(g,popupRect.x(),popupRect.y(),popupRect.width(),popupRect.height(),7,VelaTheme.BORDER,VelaTheme.SURFACE);
        int y=popupRect.y()+4;
        if(contextModule!=null){
            ClientModule module=contextModule;
            popupRow(g,new Rect(popupRect.x()+4,y,popupRect.width()-8,23),"Назначить клавишу",mx,my,()->{binding=module;hover.put("bind-entrance",0f);draftKey=module.boundKey();draftLabel=dev.sqwat.vela.ui.input.KeyLabels.of(draftKey,module.keybind());draftMode=module.bindMode();capturing=false;contextModule=null;});
            popupRow(g,new Rect(popupRect.x()+4,y+23,popupRect.width()-8,23),dev.sqwat.vela.ui.Lang.tr("Режим: ")+(module.bindMode()==ClientModule.BindMode.HOLD?"Hold":"Toggle"),mx,my,()->{module.bind(module.boundKey(),module.keybind(),module.bindMode()==ClientModule.BindMode.HOLD?ClientModule.BindMode.TOGGLE:ClientModule.BindMode.HOLD);services.configs().requestSave();contextModule=null;});
        }else {
            ChoiceSetting select=choice;
            for(String option:select.options()){Rect row=new Rect(popupRect.x()+4,y,popupRect.width()-8,23);popupRow(g,row,option,mx,my,()->{select.set(option);choice=null;services.configs().requestSave();});y+=24;}
        }
        g.disableScissor();
        if(reveal<.95f)popupHits.clear();
    }
    private void popupButton(GuiGraphicsExtractor g,Rect r,String label,Runnable action,boolean selected){
        VelaDraw.roundedOutline(g,r.x(),r.y(),r.width(),r.height(),6,VelaTheme.BORDER,selected?VelaTheme.PRIMARY_SOFT:VelaTheme.SURFACE_DEEP);
        VelaDraw.label(g,label,r,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);popupHits.add(new PopupHit(r,action));
    }
    private void popupRow(GuiGraphicsExtractor g,Rect r,String label,int mx,int my,Runnable action){
        if(label.equals("Назначить клавишу")){controls.button(g,"assign-key",r,label,r.contains(mx,my));popupHits.add(new PopupHit(r,action));return;}
        float h=animate("choice-"+label,r.contains(mx,my),frameDt);
        boolean selected=choice instanceof BooleanGroup group?group.selected(label):groups.isMetrics(choice)?groups.metric(label).value():choice!=null&&label.equals(choice.value());
        float selectedAmount=controls.animate("option-selected-"+(choice==null?"context":choice.id())+"-"+label,selected?1:0);
        VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),3,VelaTheme.mix(VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,h),VelaTheme.PRIMARY,selectedAmount*.16f));
        if(selectedAmount>.005f){VelaDraw.roundedRect(g,r.x()+2,r.y()+4,2,r.height()-8,1,(Math.round(255*selectedAmount)<<24)|(VelaTheme.PRIMARY&0xFFFFFF));dev.sqwat.vela.ui.render.VelaIcons.draw(g,"check",r.x()+r.width()-15,r.y()+(r.height()-10)/2,10,(Math.round(255*selectedAmount)<<24)|(VelaTheme.PRIMARY&0xFFFFFF));}
        VelaDraw.text(g,VelaDraw.ellipsize(label,r.width()-28,VelaDraw.Weight.REGULAR,8),r.x()+8,r.y()+(r.height()-8)/2,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);popupHits.add(new PopupHit(r,action));
    }
    private record PopupHit(Rect rect,Runnable action){}
    private record FriendHit(java.util.UUID id,Rect rect,boolean favorite){}

    private record Layout(int x, int y, int w, int h, int sidebar) {}
    private record CardHit(ClientModule module, Rect card, Rect toggle) {}
    private record SettingHit(Setting<?> setting, Rect rect,Rect visible) {
        SettingHit(Setting<?> setting,Rect rect){this(setting,rect,rect);}
    }
    private record ProfileHit(String name, Rect rect,int action) {}
}
