package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.utility.CommandEntries;
import dev.sqwat.vela.ui.input.TextEdit;
import dev.sqwat.vela.ui.animation.*;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** One editor shared by key-triggered and timed messages. Drafts are saved explicitly. */
public final class CommandEditorScreen extends VelaScreen {
    private record Action(Rect rect,Runnable run){}
    private final VelaScreen parent;
    private final VelaServices services;
    private final boolean interval;
    private final UiControls controls=new UiControls();
    private final FrameClock clock=new FrameClock();
    private final TextEdit[] fields={new TextEdit(),new TextEdit(),new TextEdit()};
    private final List<Action> actions=new ArrayList<>();
    private final Map<Rect,Integer> inputs=new LinkedHashMap<>();
    private final dev.sqwat.vela.ui.input.CommandDraft draft=new dev.sqwat.vela.ui.input.CommandDraft();
    private int focus=-1,key=-1;
    private boolean enabled=true,capturing,closing,draggingScroll,scrollToEnd;
    private float reveal,scroll,targetScroll,formReveal=1;
    private Rect scrollTrack=new Rect(0,0,0,0);
    private Rect scrollThumb=new Rect(0,0,0,0);
    private double scrollGrab;
    private int scrollLimit;
    private Rect list=new Rect(0,0,0,0);
    private String status="";
    public CommandEditorScreen(VelaScreen parent,VelaServices services,boolean interval){
        super(Component.literal(interval?"Interval Commands":"Macros"));this.parent=parent;this.services=services;this.interval=interval;fresh();
    }
    @Override protected float interfaceScale(){return services.configs().current().uiScale;}
    private void fresh(){draft.fresh();fields[0].set("");fields[1].set("");fields[2].set("60");key=-1;enabled=true;focus=0;capturing=false;status="";}
    private void transition(Runnable action){capturing=false;focus=-1;action.run();formReveal=0;}
    private void select(CommandEntries.Entry e){draft.select(e.id());fields[0].set(e.name());fields[1].set(e.text());fields[2].set(Integer.toString(e.seconds()));key=e.key();enabled=e.enabled();focus=-1;capturing=false;status="";}
    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        parent.renderAsBackdrop(g,width,height,delta);
        float dt=clock.step();reveal=Motion.approach(reveal,closing?0:1,18,dt);
        formReveal=Motion.approach(formReveal,1,18,dt);
        if(closing&&reveal<.01f){minecraft.gui.setScreen(parent);return;}
        int w=Math.min(354,width-24),h=Math.min(266,height-12),x=(width-w)/2,y=(height-h)/2;
        g.nextStratum();g.fill(0,0,width,height,0xB805070A);
        UiClip.enable(g,x-1,y-1,x+w+1,y+Math.round((h+2)*reveal));
        VelaDraw.roundedOutline(g,x,y,w,h,9,VelaTheme.BORDER,VelaTheme.WINDOW);
        actions.clear();inputs.clear();
        VelaIcons.draw(g,interval?"cooldowns":"compose",x+12,y+11,14,VelaTheme.PRIMARY);
        VelaDraw.text(g,interval?"Interval Commands":"Macros",x+32,y+13,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,10);
        Rect close=new Rect(x+w-31,y+8,20,20);controls.iconButton(g,"editor-close",close,"x",false,close.contains(mx,my));actions.add(new Action(close,this::onClose));
        VelaDraw.hairline(g,x,y+35,w,VelaTheme.BORDER);
        int lw=Math.min(125,w/3),formX=x+lw+23,formW=w-lw-36;
        list=new Rect(x+10,y+54,lw,h-96);
        var entries=services.commands().all().stream().filter(e->e.interval()==interval).toList();
        scrollLimit=Math.max(0,entries.size()*34-list.height());if(scrollToEnd){targetScroll=scrollLimit;scrollToEnd=false;}targetScroll=Math.clamp(targetScroll,0,scrollLimit);
        if(draggingScroll)scroll=targetScroll;else scroll=Motion.scroll(Math.clamp(scroll,0,scrollLimit),targetScroll,dt);
        UiClip.enable(g,list.x(),list.y(),list.x()+list.width(),list.y()+list.height());
        g.pose().pushMatrix();g.pose().translate(0,-scroll);
        boolean oldSubpixel=VelaDraw.subpixelText(Math.abs(scroll-targetScroll)>.01f||draggingScroll);
        int rowY=list.y();
        for(var e:entries){
            Rect row=new Rect(list.x(),rowY,lw-9,30);float active=controls.animate("entry-"+e.id(),e.id().equals(draft.selected())?1:0);
            VelaDraw.roundedRect(g,row.x(),row.y(),row.width(),30,5,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.PRIMARY_SOFT,active));
            VelaDraw.text(g,VelaDraw.ellipsize(e.name(),row.width()-12,VelaDraw.Weight.MEDIUM,8),row.x()+6,row.y()+5,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
            String label=!e.enabled()?"Выключено":interval?e.seconds()+" сек":dev.sqwat.vela.ui.input.KeyLabels.of(e.key(),"Не назначено");
            VelaDraw.text(g,label,row.x()+6,row.y()+18,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7);
            int visualY=row.y()-Math.round(scroll);
            int top=Math.max(visualY,list.y()),bottom=Math.min(visualY+30,list.y()+list.height());
            if(bottom>top)actions.add(new Action(new Rect(row.x(),top,row.width(),bottom-top),()->{if(!e.id().equals(draft.selected()))transition(()->select(e));}));
            rowY+=34;
        }
        VelaDraw.subpixelText(oldSubpixel);g.pose().popMatrix();
        if(entries.isEmpty())VelaDraw.text(g,"Записей пока нет",list.x()+4,list.y()+9,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8);
        scrollTrack=new Rect(list.x()+lw-7,list.y(),7,list.height());
        if(scrollLimit>0){int thumb=Math.max(14,list.height()*list.height()/(entries.size()*34));scrollThumb=new Rect(scrollTrack.x(),list.y()+Math.round((list.height()-thumb)*scroll/scrollLimit),scrollTrack.width(),thumb);VelaDraw.roundedRect(g,scrollTrack.x()+2,list.y(),3,list.height(),1,VelaTheme.SURFACE_DEEP);VelaDraw.roundedRect(g,scrollTrack.x()+2,scrollThumb.y(),3,thumb,1,VelaTheme.TEXT_MUTED);}
        g.disableScissor();
        Rect create=new Rect(x+10,y+h-33,lw-5,23);
        UiControls.drawAction(g,create,"Создать",controls.animate("create-hover",create.contains(mx,my)?1:0));actions.add(new Action(create,()->transition(this::fresh)));
        g.pose().pushMatrix();g.pose().translate(0,(1-formReveal)*3);
        int firstField=y+(h<246?54:60),fieldGap=h<246?35:41,thirdField=firstField+fieldGap*2;
        field(g,0,new Rect(formX,firstField,formW,24),"Название");
        field(g,1,new Rect(formX,firstField+fieldGap,formW,24),"Команда /… или сообщение");
        if(interval)field(g,2,new Rect(formX,thirdField,formW,24),"Интервал, секунды");
        else{
            VelaDraw.text(g,"Клавиша",formX,thirdField-10,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
            button(g,new Rect(formX,thirdField,formW,24),capturing?"Нажмите клавишу…":key<0?"Назначить клавишу":dev.sqwat.vela.ui.input.KeyLabels.of(key,""),mx,my,()->{capturing=true;focus=-1;});
        }
        int toggleY=thirdField+(h<246?31:36);
        Rect toggle=new Rect(formX+formW-13,toggleY,11,11);controls.checkbox(g,"entry-enabled",toggle,enabled);
        VelaDraw.text(g,interval?"Таймер включён":"Макрос включён",formX,toggleY+2,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
        actions.add(new Action(new Rect(formX,toggleY-3,formW,19),()->enabled=!enabled));
        if(!status.isEmpty())VelaDraw.text(g,VelaDraw.ellipsize(status,formW,VelaDraw.Weight.REGULAR,7),formX,y+h-48,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7);
        int half=(formW-6)/2;
        button(g,new Rect(formX,y+h-33,half,23),draft.selected()==null?"Добавить":"Сохранить",mx,my,this::save);
        if(draft.selected()!=null)button(g,new Rect(formX+half+6,y+h-33,half,23),"Удалить",mx,my,()->transition(()->{try{services.commands().remove(draft.selected());fresh();}catch(RuntimeException ex){status=ex.getMessage();}}));
        g.pose().popMatrix();
        g.disableScissor();
    }
    private void field(GuiGraphicsExtractor g,int index,Rect r,String label){
        VelaDraw.text(g,label,r.x(),r.y()-10,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
        VelaDraw.roundedOutline(g,r.x(),r.y(),r.width(),r.height(),5,VelaTheme.mix(VelaTheme.BORDER,VelaTheme.PRIMARY,controls.animate("field-focus-"+index,focus==index?1:0)),VelaTheme.SURFACE_DEEP);
        TextFieldDraw.draw(g,fields[index],new Rect(r.x()+7,r.y()+5,r.width()-14,14),focus==index,index==1?"/spawn":"");inputs.put(r,index);
    }
    private void button(GuiGraphicsExtractor g,Rect r,String label,int mx,int my,Runnable action){controls.button(g,"button:"+r.x()+":"+r.y(),r,label,r.contains(mx,my));actions.add(new Action(r,action));}
    private void save(){try{
        boolean created=draft.save(services.commands(),interval,fields[0].text().strip(),fields[1].text().strip(),key,interval?Integer.parseInt(fields[2].text()):60,enabled);
        if(created){transition(()->{fresh();scrollToEnd=true;status="Добавлено — можно создать следующую";});}else status="Изменения сохранены";
    }catch(NumberFormatException ex){status="Интервал должен быть целым числом";}catch(RuntimeException ex){status=ex.getMessage();}}
    @Override protected boolean clickVela(MouseButtonEvent event,boolean doubled){
        if(event.button()!=0||closing||reveal<.95f)return true;
        if(scrollLimit>0&&scrollTrack.contains(event.x(),event.y())){draggingScroll=true;boolean grabbed=scrollThumb.contains(event.x(),event.y());scrollGrab=grabbed?event.y()-scrollThumb.y():scrollThumb.height()/2d;if(grabbed)targetScroll=scroll;else scrollTo(event.y());return true;}
        for(var e:inputs.entrySet())if(e.getKey().contains(event.x(),event.y())){focus=e.getValue();capturing=false;Rect r=e.getKey();TextFieldDraw.click(fields[focus],new Rect(r.x()+7,r.y()+5,r.width()-14,14),event.x(),false);if(doubled)fields[focus].selectAll();return true;}
        focus=-1;for(var action:actions)if(action.rect.contains(event.x(),event.y())){action.run.run();return true;}return true;
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(closing)return true;
        if(capturing){if(event.key()!=GLFW.GLFW_KEY_ESCAPE)key=dev.sqwat.vela.ui.input.BindInput.clears(event.key())?-1:event.key();capturing=false;return true;}
        if(event.key()==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        if(focus>=0){dev.sqwat.vela.ui.input.GameTextInput.key(fields[focus],event,focus==0?40:focus==1?256:5);return true;}return true;
    }
    @Override public boolean charTyped(CharacterEvent event){if(!closing&&focus>=0&&event.isAllowedChatCharacter())fields[focus].insert(event.codepointAsString(),focus==0?40:focus==1?256:5);return true;}
    @Override protected boolean scrollVela(double x,double y,double horizontal,double vertical){if(list.contains(x,y))targetScroll=Math.clamp(targetScroll-(float)vertical*28,0,scrollLimit);return true;}
    private void scrollTo(double y){targetScroll=Math.clamp((float)(y-scrollTrack.y()-scrollGrab)/Math.max(1,scrollTrack.height()-scrollThumb.height()),0,1)*scrollLimit;}
    @Override protected boolean dragVela(MouseButtonEvent e,double dx,double dy){if(draggingScroll){scrollTo(e.y());return true;}return false;}
    @Override protected boolean releaseVela(MouseButtonEvent e){draggingScroll=false;return true;}
    @Override public void onClose(){closing=true;capturing=false;}
    @Override public boolean isPauseScreen(){return false;}
}
