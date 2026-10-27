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

/** Dashboard sections other than modules: friends, configs, the side panel and popups. */
abstract class DashboardPanels extends DashboardModules {
    protected void drawFriendSuggestions(GuiGraphicsExtractor g){
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
    protected void drawFriends(GuiGraphicsExtractor g, Rect content) {
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
            dev.sqwat.vela.ui.render.UiKit.iconAction(g,remove,"trash",10,controls.animate("friend-remove-"+friend.id(),remove.contains(pointerX,pointerY)?1:0),VelaTheme.DANGER);
            if(card.y()>=top&&card.y()+40<=bottom){friendHits.add(new FriendHit(friend.id(),star,true));friendHits.add(new FriendHit(friend.id(),remove,false));}
        }
        g.disableScissor();scrollLimit=Math.max(0,((friends.size()+1)/2)*48-(bottom-top));targetScroll=Math.min(targetScroll,scrollLimit);
    }
    protected void drawConfigs(GuiGraphicsExtractor g,Rect content) {
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
                dev.sqwat.vela.ui.render.UiKit.iconAction(g,hit,icons[i],12,controls.animate("profile-action-"+i,hit.contains(pointerX,pointerY)?1:0),VelaTheme.PRIMARY);
                if(hit.contains(pointerX,pointerY))showHint(g,new String[]{"Копировать ключ","Открыть папку","Удалить","Загрузить","Сохранить"}[i],hit.x(),hit.y()+23);
                final int action=i;
                if(rowIntersects(card,listTop,content.y()+content.height()-20))profileHits.add(new ProfileHit(name,hit,action));
            }
            y+=49;
        }
        g.disableScissor();
        scrollLimit=Math.max(0,profiles.size()*49-(content.y()+content.height()-20-listTop));targetScroll=Math.max(0,Math.min(targetScroll,scrollLimit));
    }
    protected void drawSidePanel(GuiGraphicsExtractor g,Layout l){
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
    protected void drawPopup(GuiGraphicsExtractor g,int mx,int my) {
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
    protected void popupButton(GuiGraphicsExtractor g,Rect r,String label,Runnable action,boolean selected){
        VelaDraw.roundedOutline(g,r.x(),r.y(),r.width(),r.height(),6,VelaTheme.BORDER,selected?VelaTheme.PRIMARY_SOFT:VelaTheme.SURFACE_DEEP);
        VelaDraw.label(g,label,r,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);popupHits.add(new PopupHit(r,action));
    }
    protected void popupRow(GuiGraphicsExtractor g,Rect r,String label,int mx,int my,Runnable action){
        if(label.equals("Назначить клавишу")){controls.button(g,"assign-key",r,label,r.contains(mx,my));popupHits.add(new PopupHit(r,action));return;}
        float h=animate("choice-"+label,r.contains(mx,my),frameDt);
        boolean selected=choice instanceof BooleanGroup group?group.selected(label):groups.isMetrics(choice)?groups.metric(label).value():choice!=null&&label.equals(choice.value());
        float selectedAmount=controls.animate("option-selected-"+(choice==null?"context":choice.id())+"-"+label,selected?1:0);
        VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),3,VelaTheme.mix(VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,h),VelaTheme.PRIMARY,selectedAmount*.16f));
        if(selectedAmount>.005f){VelaDraw.roundedRect(g,r.x()+2,r.y()+4,2,r.height()-8,1,(Math.round(255*selectedAmount)<<24)|(VelaTheme.PRIMARY&0xFFFFFF));dev.sqwat.vela.ui.render.VelaIcons.draw(g,"check",r.x()+r.width()-15,r.y()+(r.height()-10)/2,10,(Math.round(255*selectedAmount)<<24)|(VelaTheme.PRIMARY&0xFFFFFF));}
        VelaDraw.text(g,VelaDraw.ellipsize(label,r.width()-28,VelaDraw.Weight.REGULAR,8),r.x()+8,r.y()+(r.height()-8)/2,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);popupHits.add(new PopupHit(r,action));
    }
    protected DashboardPanels(Screen parent, VelaServices services, VelaSection initialSection) {
        super(parent, services, initialSection);
    }

}
