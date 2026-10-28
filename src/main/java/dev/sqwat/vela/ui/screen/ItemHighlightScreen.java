package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.module.visual.ItemHighlight;
import dev.sqwat.vela.ui.Lang;
import dev.sqwat.vela.ui.animation.FrameClock;
import dev.sqwat.vela.ui.animation.Motion;
import dev.sqwat.vela.ui.input.GameTextInput;
import dev.sqwat.vela.ui.input.TextEdit;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Item Highlight's own items: every item of the game on the left (search by name or id, click to add), the added
 * ones on the right with their colour — a palette of eight and a hex field — and a remove button.
 */
public final class ItemHighlightScreen extends VelaScreen {
    private static final int CELL=22,ROW=30;
    private static final int[] PALETTE={0x756CFF,0x55CFFF,0x45D39A,0xF3BE52,0xFF8855,0xFF667C,0xB78AF3,0xF3F5FA};
    private final Screen parent;
    private final ItemHighlight module;
    private final TextEdit search=new TextEdit(),hex=new TextEdit();
    private final UiControls controls=new UiControls();
    private final FrameClock clock=new FrameClock();
    private final List<Map.Entry<Rect,Runnable>> actions=new ArrayList<>();
    private List<ItemStack> all=List.of(),shown=List.of();
    private String lastQuery;
    private String editing;
    private boolean hexFocused;
    private float gridScroll,gridTarget,listScroll,listTarget,entrance;
    private Rect grid=new Rect(0,0,0,0),list=grid;

    public ItemHighlightScreen(Screen parent,ItemHighlight module){super(Component.literal("Item Highlight"));this.parent=parent;this.module=module;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){}
    @Override protected void init(){
        if(!all.isEmpty())return;
        var items=new ArrayList<ItemStack>();
        for(var item:BuiltInRegistries.ITEM)if(item!=Items.AIR)items.add(new ItemStack(item));
        all=List.copyOf(items);
    }
    private static String id(ItemStack stack){return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();}
    private void filter(){
        if(search.text().equals(lastQuery))return;
        lastQuery=search.text();String q=lastQuery.strip().toLowerCase(Locale.ROOT);
        shown=q.isEmpty()?all:all.stream().filter(s->id(s).contains(q)||s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList();
        gridTarget=0;
    }

    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        float dt=clock.step();actions.clear();filter();
        entrance=Motion.approach(entrance,1,14,dt);float ease=Motion.easeOutCubic(entrance);
        if(parent instanceof VelaScreen backdrop)backdrop.renderAsBackdrop(g,width,height,delta);
        g.fill(0,0,width,height,(Math.round(0xC0*ease)<<24)|0x05070A);
        int w=Math.min(width-40,560),h=Math.min(height-40,330);
        var r=new Rect((width-w)/2,(height-h)/2+Math.round((1-ease)*10),w,h);
        var content=UiKit.window(g,r,"Подсветка предметов","Нажмите на предмет, чтобы добавить его со своим цветом");
        var close=new Rect(r.x()+r.width()-30,r.y()+10,20,20);
        UiKit.iconAction(g,close,"x",10,controls.animate("close",close.contains(mx,my)?1:0),VelaTheme.TEXT);
        actions.add(Map.entry(close,this::onClose));

        int split=Math.round(content.width()*.52f);
        // Left: every item, filtered by the search.
        UiKit.searchField(g,new Rect(content.x(),content.y(),split-8,UiKit.FIELD_H),search,!hexFocused,"Поиск предмета…");
        grid=new Rect(content.x(),content.y()+UiKit.FIELD_H+8,split-8,content.height()-UiKit.FIELD_H-8);
        int columns=Math.max(1,grid.width()/CELL);
        float maxGrid=Math.max(0,(float)Math.ceil(shown.size()/(double)columns)*CELL-grid.height());
        gridTarget=Math.clamp(gridTarget,0,maxGrid);gridScroll=Motion.approach(gridScroll,gridTarget,18,dt);
        var colors=module.custom().colors();
        UiClip.enable(g,grid.x(),grid.y(),grid.x()+grid.width(),grid.y()+grid.height());
        try{
            int first=Math.max(0,(int)(gridScroll/CELL))*columns;
            for(int i=first;i<shown.size();i++){
                int row=i/columns,col=i%columns;
                int x=grid.x()+col*CELL,y=grid.y()+row*CELL-Math.round(gridScroll);
                if(y>grid.y()+grid.height())break;
                var cell=new Rect(x,y,CELL-2,CELL-2);var stack=shown.get(i);String id=id(stack);
                boolean added=colors.containsKey(id),over=cell.contains(mx,my)&&grid.contains(mx,my);
                float hover=controls.animate("cell"+id,over?1:0);
                if(added)VelaDraw.roundedRect(g,cell.x(),cell.y(),cell.width(),cell.height(),4,0x60000000|colors.get(id));
                else if(hover>.02f)VelaDraw.roundedRect(g,cell.x(),cell.y(),cell.width(),cell.height(),4,(Math.round(0x30*hover)<<24)|0xFFFFFF);
                if(hover>.02f)new GameUiCanvas(g).border(cell.x(),cell.y(),cell.width(),cell.height(),4,UiKit.hoverBorder(hover));
                g.item(stack,cell.x()+2,cell.y()+2);
                if(over)actions.add(Map.entry(cell,()->{if(!colors.containsKey(id))module.custom().put(id,VelaTheme.PRIMARY);editing=id;saveConfig();}));
            }
        }finally{g.disableScissor();}
        if(shown.isEmpty())UiKit.empty(g,grid,"search","Ничего не найдено","Попробуйте другое название или id");

        // Right: the added items with their colours.
        list=new Rect(content.x()+split+4,content.y(),content.width()-split-4,content.height());
        UiKit.caption(g,Lang.pick("Added · ","Добавлено · ")+colors.size(),list.x(),list.y()+2);
        var area=new Rect(list.x(),list.y()+16,list.width(),list.height()-16);
        if(colors.isEmpty()){UiKit.empty(g,area,"star","Пока ничего","Выберите предметы слева");return;}
        int editorHeight=editing!=null&&colors.containsKey(editing)?54:0;
        float maxList=Math.max(0,colors.size()*ROW+editorHeight-area.height());
        listTarget=Math.clamp(listTarget,0,maxList);listScroll=Motion.approach(listScroll,listTarget,18,dt);
        UiClip.enable(g,area.x(),area.y(),area.x()+area.width(),area.y()+area.height());
        try{
            int y=area.y()-Math.round(listScroll);
            for(var e:colors.entrySet()){
                String id=e.getKey();int rgb=e.getValue();
                var row=new Rect(area.x(),y,area.width(),ROW-4);
                boolean over=row.contains(mx,my)&&area.contains(mx,my);
                UiKit.card(g,row,controls.animate("row"+id,over?1:0),controls.animate("sel"+id,id.equals(editing)?1:0));
                var item=BuiltInRegistries.ITEM.getValue(Identifier.tryParse(id));
                if(item!=null)g.item(new ItemStack(item),row.x()+5,row.y()+5);
                String name=item==null?id:new ItemStack(item).getHoverName().getString();
                VelaDraw.text(g,VelaDraw.ellipsize(name,row.width()-74,VelaDraw.Weight.MEDIUM,8),row.x()+26,row.y()+9,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
                var swatch=new Rect(row.x()+row.width()-46,row.y()+6,16,14);
                VelaDraw.roundedOutline(g,swatch.x(),swatch.y(),swatch.width(),swatch.height(),4,VelaTheme.BORDER_HOVER,0xFF000000|rgb);
                var remove=new Rect(row.x()+row.width()-24,row.y()+4,18,18);
                UiKit.iconAction(g,remove,"trash",10,controls.animate("rm"+id,remove.contains(mx,my)?1:0),VelaTheme.DANGER);
                if(over){
                    actions.add(Map.entry(remove,()->{module.custom().remove(id);if(id.equals(editing))editing=null;saveConfig();}));
                    actions.add(Map.entry(row,()->{editing=id.equals(editing)?null:id;hex.set(String.format("%06X",rgb));hexFocused=false;}));
                }
                y+=ROW;
                if(id.equals(editing)){
                    // Palette and hex field under the selected item.
                    int px=row.x()+4;
                    for(int c:PALETTE){
                        var chip=new Rect(px,y,16,16);
                        float ph=controls.animate("pal"+c,chip.contains(mx,my)?1:0);
                        VelaDraw.roundedOutline(g,chip.x(),chip.y(),16,16,8,c==rgb?VelaTheme.TEXT:VelaTheme.mix(VelaTheme.BORDER,VelaTheme.BORDER_HOVER,ph),0xFF000000|c);
                        final int color=c;
                        actions.add(Map.entry(chip,()->{module.custom().put(id,color);hex.set(String.format("%06X",color));saveConfig();}));
                        px+=20;
                    }
                    var field=new Rect(row.x()+4,y+22,row.width()-8,20);
                    VelaDraw.roundedOutline(g,field.x(),field.y(),field.width(),field.height(),5,hexFocused?VelaTheme.PRIMARY:VelaTheme.BORDER,VelaTheme.SURFACE_DEEP);
                    VelaDraw.text(g,"#",field.x()+7,field.y()+6,VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,8);
                    TextFieldDraw.draw(g,hex,new Rect(field.x()+16,field.y()+3,field.width()-22,14),hexFocused,"RRGGBB");
                    actions.add(Map.entry(field,()->hexFocused=true));
                    y+=54;
                }
            }
        }finally{g.disableScissor();}
    }
    private void applyHex(){
        if(editing==null)return;
        String text=hex.text().strip().replace("#","");
        if(text.matches("[0-9A-Fa-f]{6}")){module.custom().put(editing,Integer.parseInt(text,16));saveConfig();}
    }
    private void saveConfig(){var services=dev.sqwat.vela.VelaClient.services();if(services!=null)services.configs().requestSave();}

    @Override protected boolean clickVela(MouseButtonEvent e,boolean doubled){
        boolean wasHex=hexFocused;hexFocused=false;
        for(var a:actions)if(a.getKey().contains(e.x(),e.y())){a.getValue().run();if(wasHex&&!hexFocused)applyHex();return true;}
        if(wasHex)applyHex();
        return true;
    }
    @Override protected boolean scrollVela(double x,double y,double h,double v){
        if(grid.contains(x,y))gridTarget-=(float)v*CELL*2;else if(list.contains(x,y))listTarget-=(float)v*ROW;
        return true;
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(event.key()==GLFW.GLFW_KEY_ESCAPE){if(hexFocused){hexFocused=false;return true;}onClose();return true;}
        if(hexFocused){
            if(event.key()==GLFW.GLFW_KEY_ENTER||event.key()==GLFW.GLFW_KEY_KP_ENTER){applyHex();hexFocused=false;return true;}
            GameTextInput.key(hex,event,7);return true;
        }
        GameTextInput.key(search,event,48);return true;
    }
    @Override public boolean charTyped(CharacterEvent event){
        if(!event.isAllowedChatCharacter())return true;
        if(hexFocused)hex.insert(event.codepointAsString(),7);else search.insert(event.codepointAsString(),48);
        return true;
    }
}
