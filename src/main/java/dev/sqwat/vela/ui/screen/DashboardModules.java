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

/**
 * Dashboard modules. By default a panel layout: the tab's modules as a compact list on the left and the selected
 * module's settings in their own pane on the right, like the menus of competitive shooters. The previous layout (two
 * columns of cards that expand in place) stays available as a setting.
 */
abstract class DashboardModules extends DashboardBase {
    /** The module whose settings the right pane shows (panel layout). */
    protected String selectedModule;
    protected float paneScroll,paneTarget,paneLimit;
    protected Rect paneHit=new Rect(0,0,0,0),listHit=paneHit;
    protected static final int ROW=30,ROW_GAP=4,PANE_HEADER=50;

    protected boolean panelLayout(){return services.configs().current().panelModuleMenu;}
    /** The tab's modules (or every match while searching): favourites first, then A–Z by the name as shown. */
    protected List<ClientModule> shownModules(){
        List<ClientModule> modules = search.isBlank() ? services.modules().find(section.category(), search) : services.modules().findAll(search);
        var favorites=services.configs().current().favorites;
        return modules.stream().filter(ClientModule::available).sorted(java.util.Comparator.<ClientModule,Boolean>comparing(m->!favorites.contains(m.id()))
            .thenComparing(m->dev.sqwat.vela.ui.Lang.tr(m.name()),String.CASE_INSENSITIVE_ORDER)).toList();
    }
    protected void selectModule(ClientModule module){
        if(module.id().equals(selectedModule))return;
        selectedModule=module.id();paneScroll=paneTarget=0;
        dev.sqwat.vela.ui.UiSounds.play(services,dev.sqwat.vela.ui.UiSounds.Event.EXPAND);
    }

    protected void drawModules(GuiGraphicsExtractor g, Rect content, int mouseX, int mouseY, float dt) {
        moduleContentHit=content;
        cardHits.clear();
        settingHits.clear();
        if(panelLayout()){drawModulePanels(g,content,mouseX,mouseY,dt);return;}
        listHit=paneHit=new Rect(0,0,0,0);
        List<ClientModule> modules=shownModules();
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
            int fullExtra=expand>0?animatedSettingsExtra(module):0; // collapsed cards skip measuring every setting
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
    private void drawModulePanels(GuiGraphicsExtractor g,Rect content,int mouseX,int mouseY,float dt){
        List<ClientModule> modules=shownModules();
        if(modules.isEmpty()){
            listHit=paneHit=new Rect(0,0,0,0);
            scrollLimit=targetScroll=scroll=0;
            drawEmpty(g,content,search.isBlank()?"В этом разделе пока нет модулей":"Ничего не найдено");
            return;
        }
        int gap=10,listW=Math.clamp(Math.round(content.width()*.4f),150,230);
        Rect list=listHit=new Rect(content.x(),content.y(),listW,content.height());
        Rect pane=paneHit=new Rect(content.x()+listW+gap,content.y(),content.width()-listW-gap,content.height());
        // A search result clicked elsewhere, or a selection no longer in this list, moves the selection.
        ClientModule selected=null;
        if(revealModule!=null){for(var m:modules)if(m.id().equals(revealModule))selected=m;}
        if(selected==null)for(var m:modules)if(m.id().equals(selectedModule))selected=m;
        if(selected==null)selected=modules.getFirst();
        if(!selected.id().equals(selectedModule)){selectedModule=selected.id();paneScroll=paneTarget=0;}
        // The list.
        int index=modules.indexOf(selected);
        if(selected.id().equals(revealModule)){targetScroll=Math.max(0,index*(ROW+ROW_GAP)-list.height()/2+ROW);revealModule=null;}
        dev.sqwat.vela.ui.render.UiClip.enable(g,list.x()-1,list.y()-1,list.x()+list.width()+1,list.y()+list.height()+1);
        int y=list.y()-Math.round(scroll);
        for(var module:modules){
            Rect row=new Rect(list.x(),y,list.width(),ROW);
            y+=ROW+ROW_GAP;
            if(row.y()+row.height()<list.y()||row.y()>list.y()+list.height())continue;
            drawModuleRow(g,module,row,module==selected,mouseX,mouseY,dt);
        }
        g.disableScissor();
        scrollLimit=Math.max(0,modules.size()*(ROW+ROW_GAP)-ROW_GAP-list.height());
        targetScroll=Math.clamp(targetScroll,0,scrollLimit);
        drawModulePane(g,selected,pane,mouseX,mouseY,dt);
    }
    private void drawModuleRow(GuiGraphicsExtractor g,ClientModule module,Rect row,boolean selected,int mouseX,int mouseY,float dt){
        boolean hovered=row.contains(mouseX,mouseY)&&listHit.contains(mouseX,mouseY);
        float h=animate("row-"+module.id(),hovered,dt),sel=controls.animate("row-selected-"+module.id(),selected?1:0),on=controls.animate("toggle-"+module.id(),module.enabled()?1:0);
        int base=VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,h);
        VelaDraw.roundedRect(g,row.x(),row.y(),row.width(),row.height(),7,VelaTheme.mix(VelaTheme.mix(base,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.PRIMARY,.2f),sel),VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.PRIMARY,.3f),flash(module)));
        // Accent bar on the left edge of the selected row, growing from its middle.
        int bar=Math.round((row.height()-12)*sel);
        if(bar>0)VelaDraw.roundedRect(g,row.x()+3,row.y()+(row.height()-bar)/2,3,bar,1,VelaTheme.PRIMARY);
        // Dot: lit while the module is on.
        int dot=VelaTheme.mix(0xFF3A4254,VelaTheme.PRIMARY,on);
        VelaDraw.roundedRect(g,row.x()+11,row.y()+row.height()/2-2,5,5,2,dot);
        int toggleX=row.x()+row.width()-29,textX=row.x()+22;
        Rect favorite=new Rect(toggleX-17,row.y()+7,15,16);
        boolean fav=services.configs().current().favorites.contains(module.id());
        if(fav||hovered){
            favoriteHits.put(favorite,module.id());
            controls.favorite(g,"module-favorite-"+module.id(),new Rect(favorite.x()+3,favorite.y()+3,9,9),fav,favorite.contains(mouseX,mouseY));
        }
        int labelWidth=favorite.x()-textX-4;
        String sub=!search.isBlank()?dev.sqwat.vela.ui.Lang.tr("в ")+sectionOf(module).title()
            :module.boundKey()>=0?dev.sqwat.vela.ui.input.KeyLabels.of(module.boundKey(),module.keybind()):"";
        int titleColor=VelaTheme.mix(VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,Math.max(on,h)),0xFFFFFFFF,sel*.4f);
        String title=VelaDraw.ellipsize(module.name(),labelWidth,VelaDraw.Weight.MEDIUM);
        if(sub.isEmpty())dev.sqwat.vela.ui.render.SearchText.draw(g,title,search,textX,row.y()+10,titleColor,VelaDraw.Weight.MEDIUM,28f/3);
        else{
            dev.sqwat.vela.ui.render.SearchText.draw(g,title,search,textX,row.y()+5,titleColor,VelaDraw.Weight.MEDIUM,28f/3);
            VelaDraw.text(g,VelaDraw.ellipsize(sub,labelWidth,VelaDraw.Weight.REGULAR,7),textX,row.y()+18,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7);
        }
        VelaDraw.toggle(g,toggleX,row.y()+9,on,dev.sqwat.vela.ui.hud.HudGeometry.toggleHit(toggleX,row.y()+9).contains(mouseX,mouseY));
        cardHits.add(new CardHit(module,row,dev.sqwat.vela.ui.hud.HudGeometry.toggleHit(toggleX,row.y()+9)));
    }
    private void drawModulePane(GuiGraphicsExtractor g,ClientModule module,Rect pane,int mouseX,int mouseY,float dt){
        float flash=flash(module);
        VelaDraw.roundedRect(g,pane.x(),pane.y(),pane.width(),pane.height(),9,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.PRIMARY,.16f),flash));
        // Header: name, description, favourite and the module's switch.
        int toggleX=pane.x()+pane.width()-33,x=pane.x()+14;
        Rect favorite=new Rect(toggleX-19,pane.y()+11,15,16);
        favoriteHits.put(favorite,module.id());
        controls.favorite(g,"pane-favorite-"+module.id(),new Rect(favorite.x()+3,favorite.y()+3,9,9),services.configs().current().favorites.contains(module.id()),favorite.contains(mouseX,mouseY));
        int textW=favorite.x()-x-6;
        VelaDraw.text(g,VelaDraw.ellipsize(module.name(),textW,VelaDraw.Weight.SEMIBOLD,11),x,pane.y()+11,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,11);
        String description=dev.sqwat.vela.ui.Lang.tr(module.description()).strip();
        // Short descriptions stay in the header; a longer one moves, whole, to the top of the scrolled area, so the
        // wheel over the pane reads it to the end instead of it being cut off after two lines.
        List<String> lines=descriptionLines(description,textW);
        boolean inHeader=lines.size()<=2;
        if(inHeader){
            if(lines.size()==2){
                VelaDraw.text(g,lines.get(0),x,pane.y()+26,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8);
                VelaDraw.text(g,lines.get(1),x,pane.y()+36,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8);
            }else if(!lines.isEmpty())VelaDraw.text(g,lines.getFirst(),x,pane.y()+28,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8);
        }
        Rect toggle=dev.sqwat.vela.ui.hud.HudGeometry.toggleHit(toggleX,pane.y()+14);
        VelaDraw.toggle(g,toggleX,pane.y()+14,controls.animate("toggle-"+module.id(),module.enabled()?1:0),toggle.contains(mouseX,mouseY));
        cardHits.add(new CardHit(module,new Rect(pane.x(),pane.y(),pane.width(),PANE_HEADER-4),toggle));
        String key=module.boundKey()>=0?dev.sqwat.vela.ui.input.KeyLabels.of(module.boundKey(),module.keybind()):dev.sqwat.vela.ui.Lang.tr("СКМ — бинд");
        float keyW=VelaDraw.textWidth(key,VelaDraw.Weight.MEDIUM,7)+10;
        int kx=Math.round(pane.x()+pane.width()-12-keyW);
        VelaDraw.roundedRect(g,kx,pane.y()+32,Math.round(keyW),12,4,0x14FFFFFF);
        VelaDraw.text(g,key,kx+5,pane.y()+34,VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,7);
        // Settings, scrolled on their own.
        Rect area=new Rect(pane.x()+12,pane.y()+PANE_HEADER,pane.width()-24,pane.height()-PANE_HEADER-6);
        paneScroll=reducedMotion()?paneTarget:Motion.scroll(paneScroll,paneTarget,dt);
        int first=settingHits.size();
        dev.sqwat.vela.ui.render.UiClip.enable(g,area.x()-2,area.y()-4,area.x()+area.width()+2,area.y()+area.height());
        int lead=0;
        if(!inHeader){
            List<String> full=descriptionLines(description,area.width());
            int top=area.y()+2-Math.round(paneScroll);
            for(int i=0;i<full.size();i++)VelaDraw.text(g,full.get(i),area.x(),top+i*10,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8);
            lead=full.size()*10+8;
        }
        drawSettings(g,module,area.x(),area.y()+4+lead-Math.round(paneScroll),area.width());
        g.disableScissor();
        for(int i=settingHits.size()-1;i>=first;i--){
            SettingHit hit=settingHits.get(i);Rect r=hit.rect();
            int top=Math.max(r.y(),area.y()),bottom=Math.min(r.y()+r.height(),area.y()+area.height());
            if(bottom<=top)settingHits.remove(i);
            else settingHits.set(i,new SettingHit(hit.setting(),r,new Rect(r.x(),top,r.width(),bottom-top)));
        }
        int total=ModuleSettingsGeometry.FIRST_ROW_OFFSET+8+lead;
        for(var setting:visibleSettings(module))total+=Math.round(ModuleSettingsGeometry.rowHeight(setting)*settingReveal(module,setting));
        paneLimit=Math.max(0,total-area.height());
        paneTarget=Math.clamp(paneTarget,0,paneLimit);
        if(paneLimit>0){
            int thumb=Math.max(16,Math.round(area.height()*area.height()/(float)(area.height()+paneLimit)));
            int sy=area.y()+Math.round((area.height()-thumb)*paneScroll/paneLimit);
            VelaDraw.roundedRect(g,pane.x()+pane.width()-5,sy,2,thumb,1,(VelaTheme.PRIMARY&0xFFFFFF)|0x99000000);
        }
        new dev.sqwat.vela.ui.render.GameUiCanvas(g).border(pane.x(),pane.y(),pane.width(),pane.height(),9,VelaTheme.mix(VelaTheme.BORDER,VelaTheme.PRIMARY,flash));
    }
    // Where each card's description breaks into its second line, by text and width: measuring it again every frame
    // (one shorter prefix at a time) was most of the cost of scrolling through the cards.
    private final Map<String,Integer> descriptionSplits=new java.util.HashMap<>();
    private final java.util.Map<String,List<String>> descriptionLines=new java.util.HashMap<>();
    /** The description word-wrapped to {@code width}, cached: measuring it every frame costs a lot of text widths. */
    private List<String> descriptionLines(String text,int width){
        if(descriptionLines.size()>256)descriptionLines.clear();
        return descriptionLines.computeIfAbsent(width+"|"+text,key->text.isEmpty()?List.of():List.copyOf(dev.sqwat.vela.ui.render.UiKit.wrap(text,width,VelaDraw.Weight.REGULAR,8)));
    }
    private int descriptionSplit(String text,int width){
        if(descriptionSplits.size()>512)descriptionSplits.clear();
        return descriptionSplits.computeIfAbsent(width+"|"+text,key->{
            int split=text.length();
            while(split>0&&VelaDraw.textWidth(text.substring(0,split),VelaDraw.Weight.REGULAR,8)>width)split=text.offsetByCodePoints(split,-1);
            if(split<text.length()){int space=text.lastIndexOf(' ',split);if(space>0)split=space;}
            return split;
        });
    }
    protected void drawDescriptionHint(GuiGraphicsExtractor g,int mx,int my){
        if(panelLayout()||sideOpen()||hasModal()||!contentHit.contains(mx,my))return;
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
        String remaining=dev.sqwat.vela.ui.Lang.tr(module.description()).strip(); // translated before splitting into lines
        int split=descriptionSplit(remaining,labelWidth);
        if(split<remaining.length()){
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
                String label=select instanceof BooleanGroup group?group.label():select.value();
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
            if(setting.isExperimental())VelaDraw.text(g,"EXPERIMENTAL",x+15+VelaDraw.textWidth(setting.name(),VelaDraw.Weight.REGULAR)+5,cursor+1.5f,VelaTheme.TEXT_MUTED,VelaDraw.Weight.SEMIBOLD,6f);
            String value = setting instanceof BooleanSetting b ? (b.value() ? "Вкл" : "Выкл")
                : setting instanceof NumberSetting n ? n.display() : setting instanceof ChoiceSetting c ? c.value() : String.valueOf(setting.value());
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
