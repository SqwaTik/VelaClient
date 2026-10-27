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

/** The Vela dashboard: frame rendering and input dispatch. State and drawing are in DashboardBase/Modules/Panels. */
public final class VelaDashboardScreen extends DashboardPanels {

    public VelaDashboardScreen(Screen parent, VelaServices services) {
        this(parent, services, rememberedSection(services));
    }

    public VelaDashboardScreen(Screen parent, VelaServices services, VelaSection initialSection) {
        super(parent, services, initialSection);
    }
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
        boolean clickable=gearHit.contains(mouseX,mouseY)||versionHit.contains(mouseX,mouseY)||actionHit.contains(mouseX,mouseY)||navHits.values().stream().anyMatch(r->r.contains(mouseX,mouseY))||cardHits.stream().anyMatch(h->h.card.contains(mouseX,mouseY))||settingHits.stream().anyMatch(h->h.visible.contains(mouseX,mouseY))||popupHits.stream().anyMatch(h->h.rect.contains(mouseX,mouseY))||profileHits.stream().anyMatch(h->h.rect.contains(mouseX,mouseY))||friendHits.stream().anyMatch(h->h.rect.contains(mouseX,mouseY))||locationHits.keySet().stream().anyMatch(r->r.contains(mouseX,mouseY));
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
        versionHit=new Rect(l.x+l.sidebar/2-24,l.y+l.h-19,48,14);
        dev.sqwat.vela.ui.render.DashboardChrome.versionHover=animate("version",versionHit.contains(mouseX,mouseY),dt);
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
        // Switching tabs (also from a search result) slides the new content up a little and fades it in.
        float switchT=reducedMotion()?1:Math.clamp((System.nanoTime()-sectionSwitchAt)/2.8e8f,0,1),switchEase=1-(1-switchT)*(1-switchT)*(1-switchT);
        g.pose().pushMatrix();g.pose().translate(0,(1-switchEase)*8);
        dev.sqwat.vela.ui.render.UiClip.enable(g,contentHit.x()-1,contentHit.y()-1,contentHit.x()+contentHit.width()+1,contentHit.y()+contentHit.height()+1);
        if(section==VelaSection.HUD)drawHudModules(g,contentHit,mouseX,mouseY,dt);
        else if(section==VelaSection.MARKERS){int total=markerPanel.draw(g,new Rect(contentHit.x(),contentHit.y()-Math.round(scroll),contentHit.width(),contentHit.height()),mouseX,mouseY,search);scrollLimit=Math.max(0,total-contentHit.height());targetScroll=Math.clamp(targetScroll,0,scrollLimit);}
        else if(section.category()!=null)drawModules(g,contentHit,mouseX,mouseY,dt);
        else if(section==VelaSection.FRIENDS)drawFriends(g,contentHit);
        else drawConfigs(g,contentHit);
        g.disableScissor();
        g.pose().popMatrix();
        if(switchEase<1){
            dev.sqwat.vela.ui.render.UiClip.enable(g,contentHit.x()-1,contentHit.y()-1,contentHit.x()+contentHit.width()+1,contentHit.y()+contentHit.height()+1);
            g.fill(contentHit.x()-1,contentHit.y()-1,contentHit.x()+contentHit.width()+1,contentHit.y()+contentHit.height()+1,(Math.round((1-switchEase)*255)<<24)|(VelaTheme.WINDOW&0xFFFFFF));
            g.disableScissor();
        }
        float scrollbarReveal=controls.animate("scrollbar-reveal",scrollLimit>0?1:0);
        if(scrollLimit>0){int thumb=Math.max(18,Math.round(contentHit.height()*contentHit.height()/(contentHit.height()+scrollLimit)));int sy=contentHit.y()+Math.round((contentHit.height()-thumb)*scroll/scrollLimit);Rect bar=ControlChrome.verticalReveal(new Rect(contentHit.x()+contentHit.width()+4,sy,2,thumb),scrollbarReveal);if(bar.height()>0)VelaDraw.roundedRect(g,bar.x(),bar.y(),bar.width(),bar.height(),1,VelaTheme.PRIMARY);}
        drawToast(g,l);
    }

    @Override
    public boolean clickVela(MouseButtonEvent event, boolean doubled) {
        if(editingText!=null){
            boolean inside=false;
            for(var hit:settingHits)if(hit.setting()==editingText&&hit.visible().contains(event.x(),event.y())){inside=true;break;}
            if(!inside)finishTextSetting();
        }
        if(capturingSetting!=null){capturingSetting.set(dev.sqwat.vela.ui.input.BindInput.combo(dev.sqwat.vela.ui.input.BindInput.mouse(event.button()),event.modifiers()));capturingSetting=null;services.configs().requestSave();return true;}
        if(section==VelaSection.MARKERS&&markerPanel.captureMouse(event.button()))return true;
        if(binding!=null&&capturing){draftKey=dev.sqwat.vela.ui.input.BindInput.combo(dev.sqwat.vela.ui.input.BindInput.mouse(event.button()),event.modifiers());draftLabel=dev.sqwat.vela.ui.input.KeyLabels.of(draftKey,"");capturing=false;saveBind();return true;}
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
        if(versionHit.contains(mx,my)){dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.EXPAND);minecraft.gui.setScreen(new ChangelogScreen(this));return true;}
        for (Map.Entry<VelaSection, Rect> entry : navHits.entrySet()) {
            if (entry.getValue().contains(mx, my)) {
                if(section!=entry.getKey())sectionSwitchAt=System.nanoTime();
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
        if(capturingSetting!=null){if(event.key()!=GLFW.GLFW_KEY_ESCAPE){capturingSetting.set(dev.sqwat.vela.ui.input.BindInput.clears(event.key())?-1:dev.sqwat.vela.ui.input.BindInput.combo(event.key(),event.modifiers()));services.configs().requestSave();}capturingSetting=null;return true;}
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
            if(capturing){draftKey=dev.sqwat.vela.ui.input.BindInput.combo(event.key(),event.modifiers());draftLabel=dev.sqwat.vela.ui.input.KeyLabels.of(draftKey,GLFW.glfwGetKeyName(event.key(),0));capturing=false;saveBind();}
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
}
