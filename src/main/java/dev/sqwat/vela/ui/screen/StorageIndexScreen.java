package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.marker.MarkerRuntime;
import dev.sqwat.vela.module.utility.StorageIndex;
import dev.sqwat.vela.module.utility.StorageStore;
import dev.sqwat.vela.ui.input.GameTextInput;
import dev.sqwat.vela.ui.input.TextEdit;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/**
 * Storage Index window: items across every remembered container on the left, where the selected item lies on the
 * right. Typing always searches; a marker can be placed on any container.
 */
public final class StorageIndexScreen extends VelaScreen {
    private static final int ROW=22,CARD=40;
    private final StorageIndex index;
    private final TextEdit search=new TextEdit();
    private final UiControls controls=new UiControls();
    private final Map<String,ItemStack> icons=new HashMap<>();
    private List<StorageStore.Hit> hits=List.of();
    private String lastQuery=null;
    private int selected;
    private float listScroll,detailScroll;
    private Rect listArea=new Rect(0,0,0,0),detailArea=listArea;
    private final List<Map.Entry<Rect,Integer>> rowHits=new ArrayList<>();
    private final List<Map.Entry<Rect,Runnable>> actions=new ArrayList<>();

    public StorageIndexScreen(StorageIndex index){super(Component.literal("Storage Index"));this.index=index;}
    @Override public boolean isPauseScreen(){return false;}

    private ItemStack icon(String id){return icons.computeIfAbsent(id,k->{var item=BuiltInRegistries.ITEM.getValue(Identifier.tryParse(k));return item==null?ItemStack.EMPTY:new ItemStack(item);});}
    private String nameOf(String id){var stack=icon(id);return stack.isEmpty()?id:stack.getHoverName().getString();}
    private void refresh(){
        if(search.text().equals(lastQuery))return;
        lastQuery=search.text();hits=index.store().search(lastQuery,this::nameOf);selected=0;listScroll=detailScroll=0;
    }

    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        refresh();rowHits.clear();actions.clear();
        g.fill(0,0,width,height,VelaTheme.OVERLAY);
        int w=Math.min(width-40,580),h=Math.min(height-40,350);
        var window=new Rect((width-w)/2,(height-h)/2,w,h);
        var store=index.store();
        String world=index.world()==null?"":index.world().replace('\\','/');
        if(world.endsWith("/"))world=world.substring(0,world.length()-1);
        String server=world.isEmpty()?"":" · "+world.substring(world.lastIndexOf('/')+1); // singleplayer: folder name, not the full path
        var content=UiKit.window(g,window,"Хранилища",store.size()+" "+plural(store.size(),"контейнер","контейнера","контейнеров")+server);
        UiKit.searchField(g,new Rect(window.x()+window.width()-214,window.y()+12,200,UiKit.FIELD_H),search,true,"Поиск предмета…");
        if(store.size()==0){UiKit.empty(g,content,"search","Пока пусто","Откройте сундук, бочку или шалкер — содержимое запомнится");return;}
        if(hits.isEmpty()){UiKit.empty(g,content,"search","Ничего не найдено","Попробуйте другое название или id предмета");return;}
        int split=Math.round(content.width()*.46f);
        listArea=new Rect(content.x(),content.y()+14,split-6,content.height()-14);
        detailArea=new Rect(content.x()+split+6,content.y()+14,content.width()-split-6,content.height()-14);
        UiKit.caption(g,"Предметы",listArea.x(),content.y());
        UiKit.caption(g,"Где лежит",detailArea.x(),content.y());
        drawList(g,mx,my);
        drawDetail(g,mx,my);
    }
    private void drawList(GuiGraphicsExtractor g,int mx,int my){
        listScroll=Math.clamp(listScroll,0,Math.max(0,hits.size()*ROW-listArea.height()));
        UiClip.enable(g,listArea.x(),listArea.y(),listArea.x()+listArea.width(),listArea.y()+listArea.height());
        try{
            int first=Math.max(0,(int)(listScroll/ROW));
            for(int i=first;i<hits.size();i++){
                int y=listArea.y()+i*ROW-Math.round(listScroll);if(y>listArea.y()+listArea.height())break;
                var hit=hits.get(i);var r=new Rect(listArea.x(),y,listArea.width(),ROW-2);
                boolean over=r.contains(mx,my)&&listArea.contains(mx,my);
                float hover=controls.animate("row-"+i,over?1:0),active=controls.animate("sel-"+i,i==selected?1:0);
                if(hover>.01f||active>.01f)VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),5,VelaTheme.mix(VelaTheme.mix(0x00000000,0x14FFFFFF,hover),(VelaTheme.PRIMARY&0xFFFFFF)|0x30000000,active));
                if(hover>.02f&&active<.98f)new GameUiCanvas(g).border(r.x(),r.y(),r.width(),r.height(),5,UiKit.hoverBorder(hover*(1-active)));
                g.item(icon(hit.item()),r.x()+3,r.y()+2);
                String total="×"+hit.total();float tw=VelaDraw.textWidth(total,VelaDraw.Weight.MEDIUM,8f);
                VelaDraw.text(g,VelaDraw.ellipsize(hit.name(),r.width()-tw-34,VelaDraw.Weight.MEDIUM,8.5f),r.x()+24,r.y()+6,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8.5f);
                VelaDraw.text(g,total,r.x()+r.width()-6-tw,r.y()+6.5f,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,8f);
                rowHits.add(Map.entry(r,i));
            }
        }finally{g.disableScissor();}
    }
    private void drawDetail(GuiGraphicsExtractor g,int mx,int my){
        if(selected>=hits.size())return;
        var hit=hits.get(selected);var mc=minecraft;
        String dimension=mc.level==null?"":MarkerRuntime.dimension(mc);
        var containers=hit.containers();
        detailScroll=Math.clamp(detailScroll,0,Math.max(0,containers.size()*(CARD+4)-detailArea.height()));
        UiClip.enable(g,detailArea.x(),detailArea.y(),detailArea.x()+detailArea.width(),detailArea.y()+detailArea.height());
        try{
            for(int i=0;i<containers.size();i++){
                int y=detailArea.y()+i*(CARD+4)-Math.round(detailScroll);
                if(y+CARD<detailArea.y())continue;if(y>detailArea.y()+detailArea.height())break;
                var c=containers.get(i);var r=new Rect(detailArea.x(),y,detailArea.width(),CARD);
                UiKit.card(g,r,controls.animate("card-"+i,r.contains(mx,my)?1:0),0);
                g.item(kindIcon(c.kind()),r.x()+6,r.y()+5);
                var key=c.key();boolean ender=c.kind()==StorageStore.Kind.ENDER;
                String title=ender?"Эндер-сундук":key.x()+" "+key.y()+" "+key.z();
                VelaDraw.text(g,title,r.x()+28,r.y()+6,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8.5f);
                var details=new StringBuilder(ender?dev.sqwat.vela.ui.Lang.tr("общий для всех"):dimensionName(key.dimension()));
                if(!ender&&key.dimension().equals(dimension)&&mc.player!=null)
                    details.append(" · ").append(Math.round(Math.sqrt(mc.player.distanceToSqr(key.x()+.5,key.y()+.5,key.z()+.5)))).append(dev.sqwat.vela.ui.Lang.tr(" м"));
                details.append(" · ").append(UiKit.ago(System.currentTimeMillis()-c.seenAt()));
                if(c.items().stream().anyMatch(s->s.item().equals(hit.item())&&s.nested()))details.append(dev.sqwat.vela.ui.Lang.tr(" · в шалкере"));
                VelaDraw.text(g,VelaDraw.ellipsize(details.toString(),r.width()-90,VelaDraw.Weight.REGULAR,7.5f),r.x()+28,r.y()+18,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
                String count="×"+c.total(hit.item());float cw=VelaDraw.textWidth(count,VelaDraw.Weight.SEMIBOLD,9f);
                VelaDraw.text(g,count,r.x()+r.width()-8-cw,r.y()+6,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,9f);
                int ax=r.x()+r.width()-8;
                String forget="Забыть";int fw=UiKit.chipWidth(forget);ax-=fw;
                var forgetRect=new Rect(ax,r.y()+CARD-UiKit.CHIP_H-5,fw,UiKit.CHIP_H);
                UiKit.chip(g,forgetRect.x(),forgetRect.y(),forget,0,controls.animate("forget-"+i,forgetRect.contains(mx,my)?1:0),true);
                actions.add(Map.entry(forgetRect,()->{index.forget(key);lastQuery=null;}));
                if(!ender&&key.dimension().equals(dimension)){
                    String mark="Метка";int mw=UiKit.chipWidth(mark);ax-=mw+4;
                    var markRect=new Rect(ax,forgetRect.y(),mw,UiKit.CHIP_H);
                    UiKit.chip(g,markRect.x(),markRect.y(),mark,1,controls.animate("mark-"+i,markRect.contains(mx,my)?1:0),true);
                    actions.add(Map.entry(markRect,()->mark(c,hit)));
                }
            }
        }finally{g.disableScissor();}
    }
    private void mark(StorageStore.Container c,StorageStore.Hit hit){
        var services=dev.sqwat.vela.VelaClient.services();if(services==null||minecraft.level==null)return;
        var key=c.key();
        try{services.markers().addStorage(hit.name(),MarkerRuntime.world(minecraft),MarkerRuntime.dimension(minecraft),key.x(),key.y(),key.z(),VelaTheme.PRIMARY,hit.item());}
        catch(IllegalArgumentException full){dev.sqwat.vela.ui.Notifications.show("Метка не создана",full.getMessage(),"markers");return;}
        onClose();
    }
    private static ItemStack kindIcon(StorageStore.Kind kind){
        return new ItemStack(switch(kind){case BARREL->Items.BARREL;case SHULKER->Items.SHULKER_BOX;case ENDER->Items.ENDER_CHEST;case OTHER->Items.HOPPER;default->Items.CHEST;});
    }
    private static String dimensionName(String dimension){
        return switch(dimension){case "minecraft:overworld"->dev.sqwat.vela.ui.Lang.pick("Overworld","Мир");case "minecraft:the_nether"->dev.sqwat.vela.ui.Lang.pick("Nether","Незер");case "minecraft:the_end"->dev.sqwat.vela.ui.Lang.pick("End","Край");default->dimension;};
    }
    private static String plural(int n,String one,String few,String many){
        if(dev.sqwat.vela.ui.Lang.english())return dev.sqwat.vela.ui.Lang.tr(n==1?one:many);
        int m10=n%10,m100=n%100;
        return m10==1&&m100!=11?one:m10>=2&&m10<=4&&(m100<12||m100>14)?few:many;
    }

    @Override protected boolean clickVela(MouseButtonEvent e,boolean doubled){
        for(var a:actions)if(a.getKey().contains(e.x(),e.y())&&detailArea.contains(e.x(),e.y())){a.getValue().run();return true;}
        for(var r:rowHits)if(r.getKey().contains(e.x(),e.y())&&listArea.contains(e.x(),e.y())){selected=r.getValue();detailScroll=0;return true;}
        return true;
    }
    @Override protected boolean scrollVela(double x,double y,double h,double v){
        if(listArea.contains(x,y))listScroll-=(float)v*ROW*2;
        else if(detailArea.contains(x,y))detailScroll-=(float)v*(CARD+4)*2;
        return true;
    }
    @Override public boolean keyPressed(KeyEvent event){
        switch(event.key()){
            case GLFW.GLFW_KEY_ESCAPE->{onClose();return true;}
            case GLFW.GLFW_KEY_DOWN->{selected=Math.min(hits.size()-1,selected+1);detailScroll=0;ensureVisible();return true;}
            case GLFW.GLFW_KEY_UP->{selected=Math.max(0,selected-1);detailScroll=0;ensureVisible();return true;}
            default->{GameTextInput.key(search,event,48);return true;}
        }
    }
    private void ensureVisible(){
        float top=selected*ROW,bottom=top+ROW;
        if(top<listScroll)listScroll=top;else if(bottom>listScroll+listArea.height())listScroll=bottom-listArea.height();
    }
    @Override public boolean charTyped(CharacterEvent event){
        if(event.isAllowedChatCharacter())search.insert(event.codepointAsString(),48);
        return true;
    }
}
