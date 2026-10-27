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
        int w=Math.min(372,width-24),h=Math.min(262,height-12),x=(width-w)/2,y=(height-h)/2;
        g.nextStratum();g.fill(0,0,width,height,(Math.round(0xB8*reveal)<<24)|0x05070A);
        g.pose().pushMatrix();g.pose().translate(0,(1-reveal)*8);
        UiClip.enable(g,x-1,y-1,x+w+1,y+h+1);
        VelaDraw.roundedOutline(g,x,y,w,h,10,VelaTheme.BORDER,VelaTheme.WINDOW);
        actions.clear();inputs.clear();
        // Header: icon tile, title and what the entries do.
        VelaDraw.roundedRect(g,x+12,y+11,22,22,6,VelaTheme.PRIMARY_SOFT);
        VelaIcons.draw(g,interval?"cooldowns":"compose",x+17,y+16,12,VelaTheme.PRIMARY);
        VelaDraw.text(g,interval?"Interval Commands":"Macros",x+42,y+11,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,10);
        VelaDraw.text(g,interval?"Сообщения и команды по таймеру":"Сообщения и команды по клавише",x+42,y+24,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
        Rect close=new Rect(x+w-30,y+12,20,20);controls.iconButton(g,"editor-close",close,"x",false,close.contains(mx,my));actions.add(new Action(close,this::onClose));
        VelaDraw.hairline(g,x,y+43,w,VelaTheme.BORDER);
        // Left: the list with its own header and a + button.
        int lw=Math.min(132,w/3),formX=x+lw+26,formW=w-lw-38;
        var entries=services.commands().all().stream().filter(e->e.interval()==interval).toList();
        UiKit.caption(g,dev.sqwat.vela.ui.Lang.pick("Entries · ","Записи · ")+entries.size(),x+12,y+54);
        Rect add=new Rect(x+lw-6,y+50,16,16);
        float addHover=controls.animate("create-hover",add.contains(mx,my)?1:0);
        VelaDraw.roundedRect(g,add.x(),add.y(),16,16,5,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.PRIMARY,.25f+.5f*addHover));
        if(addHover>.02f)new dev.sqwat.vela.ui.render.GameUiCanvas(g).border(add.x(),add.y(),16,16,5,(VelaTheme.PRIMARY&0xFFFFFF)|(Math.round(255*addHover)<<24));
        VelaIcons.draw(g,"plus",add.x()+4,add.y()+4,8,VelaTheme.TEXT);
        actions.add(new Action(add,()->transition(this::fresh)));
        list=new Rect(x+10,y+70,lw,h-80);
        g.fill(x+lw+16,y+52,x+lw+17,y+h-12,VelaTheme.BORDER);
        scrollLimit=Math.max(0,entries.size()*31-list.height());if(scrollToEnd){targetScroll=scrollLimit;scrollToEnd=false;}targetScroll=Math.clamp(targetScroll,0,scrollLimit);
        if(draggingScroll)scroll=targetScroll;else scroll=Motion.scroll(Math.clamp(scroll,0,scrollLimit),targetScroll,dt);
        UiClip.enable(g,list.x(),list.y(),list.x()+list.width(),list.y()+list.height());
        g.pose().pushMatrix();g.pose().translate(0,-scroll);
        boolean oldSubpixel=VelaDraw.subpixelText(Math.abs(scroll-targetScroll)>.01f||draggingScroll);
        int rowY=list.y();
        for(var e:entries){
            Rect row=new Rect(list.x(),rowY,lw-8,27);
            float active=controls.animate("entry-"+e.id(),e.id().equals(draft.selected())?1:0);
            float hover=controls.animate("entry-hover-"+e.id(),row.contains(mx,my+Math.round(scroll))&&list.contains(mx,my)?1:0);
            UiKit.card(g,row,hover,active);
            VelaDraw.roundedRect(g,row.x()+7,row.y()+11,5,5,3,e.enabled()?VelaTheme.POSITIVE:VelaTheme.TEXT_MUTED);
            VelaDraw.text(g,VelaDraw.ellipsize(e.name(),row.width()-26,VelaDraw.Weight.MEDIUM,8),row.x()+18,row.y()+4,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
            String label=!e.enabled()?"Выключено":interval?e.seconds()+dev.sqwat.vela.ui.Lang.tr(" сек"):dev.sqwat.vela.ui.input.KeyLabels.of(e.key(),"Не назначено");
            VelaDraw.text(g,label,row.x()+18,row.y()+15,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7);
            int visualY=row.y()-Math.round(scroll);
            int top=Math.max(visualY,list.y()),bottom=Math.min(visualY+27,list.y()+list.height());
            if(bottom>top)actions.add(new Action(new Rect(row.x(),top,row.width(),bottom-top),()->{if(!e.id().equals(draft.selected()))transition(()->select(e));}));
            rowY+=31;
        }
        VelaDraw.subpixelText(oldSubpixel);g.pose().popMatrix();
        if(entries.isEmpty()){
            VelaDraw.centeredText(g,"Записей пока нет",list.x()+list.width()/2f,list.y()+28,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,8);
            VelaDraw.centeredText(g,dev.sqwat.vela.ui.Lang.pick("Press + to add one","Нажмите +, чтобы добавить"),list.x()+list.width()/2f,list.y()+41,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
        }
        scrollTrack=new Rect(list.x()+lw-6,list.y(),6,list.height());
        if(scrollLimit>0){int thumb=Math.max(14,list.height()*list.height()/(entries.size()*31));scrollThumb=new Rect(scrollTrack.x(),list.y()+Math.round((list.height()-thumb)*scroll/scrollLimit),scrollTrack.width(),thumb);VelaDraw.roundedRect(g,scrollTrack.x()+2,scrollThumb.y(),2,thumb,1,VelaTheme.TEXT_MUTED);}
        g.disableScissor();
        UiClip.enable(g,x-1,y-1,x+w+1,y+h+1);
        // Right: the form.
        g.pose().pushMatrix();g.pose().translate(0,(1-formReveal)*3);
        UiKit.caption(g,draft.selected()==null?dev.sqwat.vela.ui.Lang.pick("New entry","Новая запись"):dev.sqwat.vela.ui.Lang.pick("Editing","Редактирование"),formX,y+54);
        int fy=y+80;
        field(g,0,new Rect(formX,fy,formW,22),"Название");fy+=36;
        field(g,1,new Rect(formX,fy,formW,22),"Команда /… или сообщение");fy+=36;
        if(interval){
            Rect seconds=new Rect(formX,fy,formW,22);
            field(g,2,seconds,"Интервал, секунды");
            // Unit inside the field, right-aligned.
            String unit=dev.sqwat.vela.ui.Lang.tr(" сек").strip();
            VelaDraw.text(g,unit,seconds.x()+seconds.width()-9-VelaDraw.textWidth(unit,VelaDraw.Weight.REGULAR,8),fy+7,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8);
        }else{
            VelaDraw.text(g,"Клавиша",formX,fy-10,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7.5f);
            Rect keyRect=new Rect(formX,fy,formW,22);
            float kh=controls.animate("key-hover",keyRect.contains(mx,my)?1:0);
            VelaDraw.roundedOutline(g,keyRect.x(),keyRect.y(),keyRect.width(),keyRect.height(),5,capturing?VelaTheme.PRIMARY:VelaTheme.mix(VelaTheme.BORDER,VelaTheme.BORDER_HOVER,kh),VelaTheme.mix(VelaTheme.SURFACE_DEEP,VelaTheme.SURFACE_HOVER,kh));
            String keyLabel=capturing?dev.sqwat.vela.ui.input.BindCapture.prompt("Нажмите клавишу…"):key<0?"Назначить клавишу":dev.sqwat.vela.ui.input.KeyLabels.of(key,"");
            VelaDraw.centeredText(g,keyLabel,keyRect.x()+keyRect.width()/2f,keyRect.y()+7,key<0&&!capturing?VelaTheme.TEXT_MUTED:VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
            actions.add(new Action(keyRect,()->{capturing=true;focus=-1;}));
        }
        // On/off switch on its own row: label left, switch right, the whole row clickable.
        fy+=30;
        Rect toggle=new Rect(formX,fy,formW,20);
        float th=controls.animate("toggle-row-hover",toggle.contains(mx,my)?1:0);
        if(th>.01f){VelaDraw.roundedRect(g,toggle.x()-4,toggle.y(),toggle.width()+8,toggle.height(),5,VelaTheme.mix(0x00000000,0x10FFFFFF,th));new dev.sqwat.vela.ui.render.GameUiCanvas(g).border(toggle.x()-4,toggle.y(),toggle.width()+8,toggle.height(),5,UiKit.hoverBorder(th));}
        VelaDraw.text(g,interval?"Таймер включён":"Макрос включён",toggle.x(),toggle.y()+6,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
        VelaDraw.toggle(g,formX+formW-24,toggle.y()+2,controls.animate("entry-enabled",enabled?1:0),toggle.contains(mx,my));
        actions.add(new Action(toggle,()->enabled=!enabled));
        // Footer: status on the left, actions on the right.
        int by=y+h-30;
        String primary=draft.selected()==null?"Добавить":"Сохранить";
        int pw=Math.round(VelaDraw.textWidth(primary,VelaDraw.Weight.SEMIBOLD,8.5f))+28;
        Rect save=new Rect(formX+formW-pw,by,pw,20);
        float sh=controls.animate("save-hover",save.contains(mx,my)?1:0);
        VelaDraw.roundedRect(g,save.x(),save.y(),save.width(),save.height(),6,VelaTheme.mix(VelaTheme.PRIMARY,0xFFFFFFFF,sh*.12f));
        VelaDraw.centeredText(g,primary,save.x()+save.width()/2f,save.y()+6,VelaTheme.onPrimary(),VelaDraw.Weight.SEMIBOLD,8.5f);
        actions.add(new Action(save,this::save));
        int statusRight=save.x()-8;
        if(draft.selected()!=null){
            String del="Удалить";int dw=Math.round(VelaDraw.textWidth(del,VelaDraw.Weight.MEDIUM,8.5f))+24;
            Rect delete=new Rect(save.x()-6-dw,by,dw,20);
            float dh=controls.animate("delete-hover",delete.contains(mx,my)?1:0);
            VelaDraw.roundedOutline(g,delete.x(),delete.y(),delete.width(),delete.height(),6,VelaTheme.mix(0x80FF6178,VelaTheme.DANGER,dh),VelaTheme.mix(VelaTheme.WINDOW,0x30FF6178,dh));
            VelaDraw.centeredText(g,del,delete.x()+delete.width()/2f,delete.y()+6,VelaTheme.DANGER,VelaDraw.Weight.MEDIUM,8.5f);
            actions.add(new Action(delete,()->transition(()->{try{services.commands().remove(draft.selected());fresh();}catch(RuntimeException ex){status=ex.getMessage();}})));
            statusRight=delete.x()-8;
        }
        if(!status.isEmpty())VelaDraw.text(g,VelaDraw.ellipsize(status,statusRight-formX,VelaDraw.Weight.REGULAR,7.5f),formX,by+6,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7.5f);
        g.pose().popMatrix();
        g.disableScissor();
        g.pose().popMatrix();
    }
    private void field(GuiGraphicsExtractor g,int index,Rect r,String label){
        VelaDraw.text(g,label,r.x(),r.y()-10,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7.5f);
        VelaDraw.roundedOutline(g,r.x(),r.y(),r.width(),r.height(),5,VelaTheme.mix(VelaTheme.BORDER,VelaTheme.PRIMARY,controls.animate("field-focus-"+index,focus==index?1:0)),VelaTheme.SURFACE_DEEP);
        TextFieldDraw.draw(g,fields[index],new Rect(r.x()+7,r.y()+4,r.width()-14,14),focus==index,index==1?"/spawn":"");inputs.put(r,index);
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
    @Override public boolean keyReleased(KeyEvent event){
        int code=dev.sqwat.vela.ui.input.BindCapture.release(event.key());
        if(capturing&&code!=dev.sqwat.vela.ui.input.BindCapture.PENDING){key=code;capturing=false;return true;}
        return super.keyReleased(event);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(closing)return true;
        if(capturing){
            if(event.key()!=GLFW.GLFW_KEY_ESCAPE){
                int code=dev.sqwat.vela.ui.input.BindInput.clears(event.key())?-1:dev.sqwat.vela.ui.input.BindCapture.press(event.key(),event.modifiers());
                if(code==dev.sqwat.vela.ui.input.BindCapture.PENDING)return true;
                key=code;
            }
            dev.sqwat.vela.ui.input.BindCapture.reset();capturing=false;return true;
        }
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
