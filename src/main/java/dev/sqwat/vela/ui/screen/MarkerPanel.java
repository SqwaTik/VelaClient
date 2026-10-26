package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.marker.MarkerRuntime;
import dev.sqwat.vela.ui.TextEdit;
import dev.sqwat.vela.ui.UiSounds;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Stateful editor sharing the dashboard's text, icon and color primitives. */
final class MarkerPanel {
    private record Action(Rect rect,Runnable run){}
    private final VelaServices services;
    private final UiControls controls=new UiControls();
    private final List<Action> actions=new ArrayList<>();
    private final Map<Rect,Integer> fields=new LinkedHashMap<>();
    private final TextEdit[] edits={new TextEdit(),new TextEdit(),new TextEdit(),new TextEdit(),new TextEdit()};
    private static final String[] ICONS={"markers","star","account","hud","utilities","visuals","friends","move","skull"};
    private boolean open,settings,picker,binding;
    private int focus=-1,icon,drag=-1;
    private float hue=.075f,sat=.8f,value=1,alpha=1;
    private Rect sv=new Rect(0,0,0,0),hueBar=sv,alphaBar=sv,limitBar=sv;
    private String message="";
    private String editingId;
    private String lastEditingId;
    private int pickerY,pickerX;
    private void edit(dev.sqwat.vela.marker.MarkerService.Point p){if(p.id().equals(editingId)){editingId=null;open=false;picker=false;return;}lastEditingId=p.id();editingId=p.id();open=true;picker=false;edits[0].set(p.name());edits[1].set(Long.toString((long)p.x()));edits[2].set(Long.toString((long)p.y()));edits[3].set(Long.toString((long)p.z()));float[] hsv=java.awt.Color.RGBtoHSB((p.color()>>16)&255,(p.color()>>8)&255,p.color()&255,null);hue=hsv[0];sat=hsv[1];value=hsv[2];alpha=(p.color()>>>24)/255f;icon=Math.max(0,List.of(ICONS).indexOf(p.icon()));syncHex();}
    boolean captureMouse(int button){if(!binding)return false;services.markers().state().key=dev.sqwat.vela.ui.BindInput.mouse(button);binding=false;save();return true;}
    boolean clearBinding(KeyEvent event){if(!binding||!dev.sqwat.vela.ui.BindInput.clears(event.key()))return false;services.markers().state().key=-1;binding=false;save();return true;}
    MarkerPanel(VelaServices services){this.services=services;edits[0].set(nextName());edits[4].set("FFA24C");coordinates();}
    private String nextName(){var c=Minecraft.getInstance();return c.level==null?"Метка 1":services.markers().nextName(MarkerRuntime.world(c),MarkerRuntime.dimension(c));}
    private void coordinates(){var c=Minecraft.getInstance();double[] xyz=c.player==null?new double[]{0,64,0}:new double[]{c.player.getX(),c.player.getY(),c.player.getZ()};for(int i=0;i<3;i++)edits[i+1].set(Long.toString((long)Math.floor(xyz[i])));}
    private int color(){return (Math.round(alpha*255)<<24)|(java.awt.Color.HSBtoRGB(hue,sat,value)&0xFFFFFF);}
    private void syncHex(){edits[4].set(String.format("%06X",color()&0xFFFFFF));}
    private void sound(UiSounds.Event event){UiSounds.play(services,event);}
    private void save(){if(drag==3)return;try{services.markers().save();}catch(IllegalStateException e){message=e.getMessage();}}
    private void label(GuiGraphicsExtractor g,String text,int x,int y){VelaDraw.text(g,text,x,y,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);}
    private void button(GuiGraphicsExtractor g,Rect r,String text,int mx,int my,Runnable action){float h=controls.animate("action:"+text+":"+editingId,r.contains(mx,my)?1:0);UiControls.drawAction(g,r,text,h);actions.add(new Action(ActionGeometry.hit(r,h),action));}
    private void iconButton(GuiGraphicsExtractor g,String id,Rect r,String icon,boolean selected,int mx,int my,Runnable action){controls.iconButton(g,id,r,icon,selected,r.contains(mx,my));actions.add(new Action(r,action));}
    boolean hovered(double x,double y){return actions.stream().anyMatch(a->a.rect.contains(x,y));}
    boolean textHovered(double x,double y){return fields.keySet().stream().anyMatch(r->r.contains(x,y));}
    private int check(GuiGraphicsExtractor g,int x,int y,int w,String name,boolean checked,int mx,int my,Runnable action){
        Rect r=new Rect(x,y,w,24);controls.checkbox(g,name,new Rect(x+w-15,y+5,13,13),checked);label(g,name,x,y+8);actions.add(new Action(r,()->{action.run();save();}));return y+25;
    }
    private void field(GuiGraphicsExtractor g,int index,Rect r,String placeholder){VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),6,VelaTheme.SURFACE_DEEP);TextFieldDraw.draw(g,edits[index],new Rect(r.x()+8,r.y()+5,r.width()-16,14),focus==index,placeholder);fields.put(r,index);}
    int draw(GuiGraphicsExtractor g,Rect area,int mx,int my,String query){
        actions.clear();fields.clear();int x=area.x(),y=area.y(),w=area.width();var state=services.markers().state();
        float expansion=controls.animate("editor-expansion",open&&editingId==null?1:0);
        VelaDraw.roundedOutline(g,x,y,w,47,8,VelaTheme.BORDER,VelaTheme.SURFACE);
        VelaIcons.draw(g,"settings",x+11,y+13,20,VelaTheme.PRIMARY);VelaDraw.text(g,"Настройки меток",x+40,y+10,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,10);label(g,"Глобальные параметры меток",x+40,y+28);
        VelaDraw.toggle(g,"markers-enabled",x+w-59,y+18,state.enabled,false);actions.add(new Action(new Rect(x+w-62,y+10,26,28),()->{state.enabled=!state.enabled;save();}));
        Rect gear=new Rect(x+w-31,y+12,22,23);iconButton(g,"marker-settings",gear,"contact_options",settings,mx,my,()->settings=!settings);y+=55;
        var creation=new MarkerGeometry.Creation(x,y,w,expansion);MarkerChrome.frame(new GameUiCanvas(g),creation.frame());
        VelaIcons.draw(g,"markers",x+11,y+13,16,VelaTheme.PRIMARY);VelaDraw.text(g,"Создание метки",x+35,y+10,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);label(g,"Создавайте новые метки",x+35,y+26);
        Rect expand=new Rect(x+w-30,y+9,22,24);controls.iconButton(g,"create-expand",expand,"chevron",open,expand.contains(mx,my));actions.add(new Action(expand,()->{open=editingId!=null||!open;editingId=null;picker=false;focus=-1;if(open){coordinates();edits[0].set(nextName());}}));
        if(expansion>.005f&&editingId==null){int visible=creation.editorHeight(),first=actions.size();UiClip.enable(g,x,y+44,x+w,y+44+visible);drawEditor(g,x,y+44,w,mx,my);g.disableScissor();if(expansion<.95f){while(actions.size()>first)actions.removeLast();fields.clear();}}
        button(g,creation.action(),"Создать",mx,my,()->{if(editingId!=null){editingId=null;coordinates();edits[0].set(nextName());}create();});y+=creation.height()+8;
        if(!message.isEmpty()){label(g,message,x+10,y+9);y+=25;}
        int row=0,listTop=y,cw=(w-8)/2;int[] columns={y,y};
        for(var p:List.copyOf(state.points)){
            if(!p.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;
            int column=row%2,cx=x+column*(cw+8),cy=columns[column];row++;
            float cardExpansion=controls.animate("card-expand:"+p.id(),p.id().equals(editingId)?1:0);int extra=Math.round((MarkerGeometry.EDITOR_HEIGHT+30)*cardExpansion);
            VelaDraw.roundedRect(g,cx,cy,cw,44+extra,7,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,controls.animate("card:"+p.id(),new Rect(cx,cy,cw,44).contains(mx,my)?1:0)));VelaIcons.draw(g,p.icon(),cx+8,cy+9,13,p.death()?VelaTheme.PRIMARY:p.color());SearchText.draw(g,VelaDraw.ellipsize(p.name(),cw-77,VelaDraw.Weight.MEDIUM,9),query,cx+27,cy+8,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);label(g,Math.round(p.x())+" / "+Math.round(p.y())+" / "+Math.round(p.z()),cx+8,cy+28);final String id=p.id();iconButton(g,"delete:"+id,new Rect(cx+cw-25,cy+6,20,23),"trash",false,mx,my,()->services.markers().remove(id));Rect setting=new Rect(cx+cw-49,cy+6,20,23);iconButton(g,"edit:"+id,setting,"contact_options",id.equals(editingId),mx,my,()->edit(p));
            columns[column]=cy+52+extra;
            if(extra>0&&(p.id().equals(editingId)||(editingId==null&&p.id().equals(lastEditingId)))){int first=actions.size();UiClip.enable(g,cx,cy+44,cx+cw,cy+44+extra);drawEditor(g,cx,cy+44,cw,mx,my);button(g,new Rect(cx+8,cy+44+MarkerGeometry.EDITOR_HEIGHT,cw-16,24),"Сохранить",mx,my,this::create);g.disableScissor();if(cardExpansion<.95f){while(actions.size()>first)actions.removeLast();fields.clear();}}
        }
        y=Math.max(columns[0],columns[1]);
        if(picker){int px=pickerX,py=pickerY;VelaDraw.roundedRect(g,px,py,132,158,8,VelaTheme.SURFACE_DEEP);sv=new Rect(px+8,py+8,116,90);
            for(int ix=0;ix<116;ix++)g.fillGradient(sv.x()+ix,sv.y(),sv.x()+ix+1,sv.y()+sv.height(),java.awt.Color.HSBtoRGB(hue,ix/115f,1),0xFF000000);
            VelaDraw.roundedOutline(g,sv.x()+Math.round(sat*115)-3,sv.y()+Math.round((1-value)*89)-3,7,7,4,0xFFFFFFFF,0);
            hueBar=new Rect(px+8,py+106,116,5);for(int i=0;i<116;i++)g.fill(hueBar.x()+i,hueBar.y(),hueBar.x()+i+1,hueBar.y()+5,java.awt.Color.HSBtoRGB(i/115f,1,1));
            alphaBar=new Rect(px+8,py+118,116,5);bar(g,alphaBar,alpha);field(g,4,new Rect(px+8,py+132,75,21),"RRGGBB");label(g,Math.round(alpha*100)+"%",px+92,py+139);
        }
        return y-area.y();
    }
    boolean settingsOpen(){return settings;}
    int settingsHeight(){var s=services.markers().state();return Math.round(controls.animate("settings-height",178+(s.death?25:0)+(s.limited?30:0)+(s.quick?28:0)));}
    void closeSettings(){settings=false;}
    void drawSettings(GuiGraphicsExtractor g,Rect area,int mx,int my){
        var state=services.markers().state();actions.clear();fields.clear();picker=false;
        float death=controls.animate("death-details",state.death?1:0),limit=controls.animate("limit-details",state.limited?1:0),quick=controls.animate("quick-details",state.quick?1:0);
        var groups=MarkerSettingsGeometry.place(area,death,limit,quick);
        settingGroup(g,groups.death(),"Метки смерти","Отмечать место вашей гибели",state.death,()->state.death=!state.death);
        if(death>.005f){Rect r=groups.death();int first=actions.size();UiClip.enable(g,r.x(),r.y()+31,r.x()+r.width(),r.y()+r.height());check(g,r.x()+7,r.y()+32,r.width()-14,"Только последняя",state.latest,mx,my,()->state.latest=!state.latest);g.disableScissor();if(death<.95f)while(actions.size()>first)actions.removeLast();}
        settingGroup(g,groups.limit(),"Лимит меток","Количество сохранённых точек",state.limited,()->state.limited=!state.limited);
        limitBar=new Rect(0,0,0,0);
        if(limit>.005f){Rect r=groups.limit();UiClip.enable(g,r.x(),r.y()+31,r.x()+r.width(),r.y()+r.height());label(g,Integer.toString(state.limit),r.x()+r.width()-26,r.y()+33);Rect track=new Rect(r.x()+8,r.y()+52,r.width()-16,5);bar(g,track,(state.limit-10)/240f,"limit");if(limit>.95f)limitBar=track;g.disableScissor();}
        settingGroup(g,groups.quick(),"Быстрая метка","Установить точку по взгляду",state.quick,()->state.quick=!state.quick);
        if(quick>.005f){Rect r=groups.quick();UiClip.enable(g,r.x(),r.y()+31,r.x()+r.width(),r.y()+r.height());label(g,"Клавиша",r.x()+8,r.y()+42);Rect key=new Rect(r.x()+r.width()-64,r.y()+34,56,23);controls.button(g,"quick-bind",key,binding?"…":state.key<0?"Нет":dev.sqwat.vela.ui.KeyLabels.of(state.key,"Клавиша"),key.contains(mx,my));if(quick>.95f)actions.add(new Action(key,()->binding=true));g.disableScissor();}
    }
    private void settingGroup(GuiGraphicsExtractor g,Rect r,String title,String description,boolean checked,Runnable toggle){
        MarkerChrome.group(new GameUiCanvas(g),r,title,description);
        Rect check=new Rect(r.x()+r.width()-22,r.y()+7,13,13);controls.checkbox(g,"group:"+title,check,checked);
        actions.add(new Action(new Rect(r.x(),r.y(),r.width(),31),()->{toggle.run();save();}));
    }
    private int drawEditor(GuiGraphicsExtractor g,int x,int y,int w,int mx,int my){
        var e=new MarkerGeometry.Editor(x,y,w);
        MarkerChrome.editor(new GameUiCanvas(g),e);
        field(g,0,e.name(),"Название");
        Rect circle=e.color();pickerX=circle.x()+circle.width()-132;pickerY=circle.y()+circle.height()+5;
        VelaDraw.roundedRect(g,circle.x()+5,circle.y()+5,12,12,6,color());
        label(g,String.format("#%06X",color()&0xFFFFFF),circle.x()+22,circle.y()+7);
        actions.add(new Action(circle,()->{picker=!picker;focus=-1;}));
        for(int i=0;i<3;i++)field(g,i+1,e.coordinate(i),new String[]{"X","Y","Z"}[i]);
        String scope=editingId==null?"create":editingId;
        for(int i=0;i<ICONS.length;i++){
            final int index=i;Rect r=e.icon(i,ICONS.length);float hover=controls.animate("icon-hover:"+scope+":"+i,r.contains(mx,my)?1:0);
            float selected=controls.animate("icon-selected:"+scope+":"+i,i==icon?1:0);
            VelaDraw.roundedRect(g,r.x()+1,r.y(),r.width()-2,r.height(),5,VelaTheme.mix((Math.round(hover*30)<<24)|0xFFFFFF,color(),selected));
            VelaIcons.draw(g,ICONS[i],r.x()+(r.width()-13)/2,r.y()+5,13,i==icon?0xFFFFFFFF:VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,hover));
            actions.add(new Action(r,()->icon=index));
        }
        return MarkerGeometry.EDITOR_HEIGHT;
    }
    private void bar(GuiGraphicsExtractor g,Rect r,float amount){controls.slider(g,r==limitBar?"limit":"alpha",r,amount);}
    private void bar(GuiGraphicsExtractor g,Rect r,float amount,String id){controls.slider(g,id,r,amount);}
    private void create(){var c=Minecraft.getInstance();if(c.level==null){message="Сначала войдите в мир";return;}try{double x=Double.parseDouble(edits[1].text()),y=Double.parseDouble(edits[2].text()),z=Double.parseDouble(edits[3].text());if(editingId!=null){services.markers().update(editingId,edits[0].text(),x,y,z,color(),ICONS[icon]);editingId=null;open=false;picker=false;message="Метка сохранена";}else{boolean automatic=edits[0].text().equals(nextName());services.markers().add(edits[0].text(),MarkerRuntime.world(c),MarkerRuntime.dimension(c),x,y,z,color(),ICONS[icon],false);if(automatic)edits[0].set(nextName());message="Метка создана";}sound(UiSounds.Event.FAVORITE);}catch(IllegalArgumentException e){message="Проверьте координаты и лимит меток";}}
    boolean click(double x,double y){
        drag=-1;
        if(picker){if(sv.contains(x,y))drag=0;else if(hueBar.contains(x,y))drag=1;else if(alphaBar.contains(x,y))drag=2;if(drag>=0){drag(x,y);return true;}}
        var state=services.markers().state();if(!picker&&settings&&state.limited&&limitBar.width()>0&&ControlChrome.sliderHit(limitBar).contains(x,y)){drag=3;drag(x,y);return true;}
        var entries=new ArrayList<>(fields.entrySet());Collections.reverse(entries);for(var e:entries)if(e.getKey().contains(x,y)){focus=e.getValue();TextFieldDraw.click(edits[focus],new Rect(e.getKey().x()+8,e.getKey().y()+5,e.getKey().width()-16,14),x,false);return true;}
        if(picker){picker=false;focus=-1;return true;}
        focus=-1;for(var a:actions)if(a.rect.contains(x,y)){a.run.run();sound(UiSounds.Event.EXPAND);return true;}if(settings){settings=false;return true;}return false;
    }
    boolean drag(double x,double y){if(drag<0)return false;switch(drag){case 0->{sat=(float)Math.clamp((x-sv.x())/sv.width(),0,1);value=1-(float)Math.clamp((y-sv.y())/sv.height(),0,1);}case 1->hue=(float)Math.clamp((x-hueBar.x())/hueBar.width(),0,1);case 2->alpha=(float)Math.clamp((x-alphaBar.x())/alphaBar.width(),0,1);case 3->{services.markers().state().limit=10+Math.round((float)Math.clamp((x-limitBar.x())/limitBar.width(),0,1)*24)*10;save();}}syncHex();sound(UiSounds.Event.SLIDER);return true;}
    void release(){boolean changed=drag==3;drag=-1;if(changed)save();}
    boolean key(KeyEvent e){
        if(binding&&e.key()==GLFW.GLFW_KEY_ESCAPE){binding=false;return true;}
        if(binding){services.markers().state().key=dev.sqwat.vela.ui.BindInput.clears(e.key())?-1:e.key();save();binding=false;return true;}
        if(focus<0)return false;
        if(e.key()==GLFW.GLFW_KEY_ESCAPE||e.key()==GLFW.GLFW_KEY_ENTER){focus=-1;return true;}
        dev.sqwat.vela.ui.GameTextInput.key(edits[focus],e,focus==0?40:16);parseHex();return true;
    }
    boolean character(String text){if(focus<0)return false;edits[focus].insert(text,focus==0?40:16);parseHex();return true;}
    private void parseHex(){if(focus!=4)return;try{String text=edits[4].text().replace("#","");if(text.length()!=6)return;int rgb=Integer.parseInt(text,16);float[] hsv=java.awt.Color.RGBtoHSB((rgb>>16)&255,(rgb>>8)&255,rgb&255,null);hue=hsv[0];sat=hsv[1];value=hsv[2];}catch(NumberFormatException ignored){}}
}
