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

/** Dashboard module cards and their expandable settings. */
abstract class DashboardModules extends DashboardBase {

    protected void drawModules(GuiGraphicsExtractor g, Rect content, int mouseX, int mouseY, float dt) {
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
    protected void drawDescriptionHint(GuiGraphicsExtractor g,int mx,int my){
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
    protected void drawHudModules(GuiGraphicsExtractor g, Rect content, int mouseX, int mouseY, float dt) {
        drawModules(g,content,mouseX,mouseY,dt);
    }

    protected void drawModuleCard(GuiGraphicsExtractor g,ClientModule module,Rect card,int mouseX,int mouseY,float dt,boolean expanded) {
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

    protected void drawSettings(GuiGraphicsExtractor g, ClientModule module, int x, int y, int width) {
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
                Rect hit=new Rect(x+width-70,cursor-4,70,18);controls.button(g,"key-setting-"+module.id()+key.id(),hit,capturingSetting==key?dev.sqwat.vela.ui.input.BindCapture.prompt("Нажмите…"):dev.sqwat.vela.ui.input.KeyLabels.of(key.value(),"None"),hit.contains(pointerX,pointerY));
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

    protected float settingReveal(ClientModule module,Setting<?> setting){return controls.animate("setting-reveal-"+module.id()+"-"+setting.id(),setting.visible()?1:0);}
    protected int animatedSettingsExtra(ClientModule module){
        var settings=visibleSettings(module);int rows=0;
        for(var setting:settings)rows+=Math.round(ModuleSettingsGeometry.rowHeight(setting)*settingReveal(module,setting));
        return ModuleSettingsGeometry.CONTENT_OFFSET+ModuleSettingsGeometry.FIRST_ROW_OFFSET-ModuleSettingsGeometry.HEADER_HEIGHT+rows;
    }
    protected DashboardModules(Screen parent, VelaServices services, VelaSection initialSection) {
        super(parent, services, initialSection);
    }

}
