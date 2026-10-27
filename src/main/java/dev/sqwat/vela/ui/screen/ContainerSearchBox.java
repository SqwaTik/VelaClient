package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.mixin.screen.ContainerScreenAccessor;
import dev.sqwat.vela.module.utility.ContainerTools;
import dev.sqwat.vela.ui.input.GameTextInput;
import dev.sqwat.vela.ui.input.TextEdit;
import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.TextFieldDraw;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaIcons;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Container Search box, drawn in the client style at the window's top-right corner. Input goes through Fabric's screen
 * events, which run before the screen itself, so it works the same in chests, the survival inventory (whose recipe
 * book takes keys first) and creative. While focused it keeps every key except Escape. Render thread only.
 */
public final class ContainerSearchBox {
    private static final int W=104,H=18;
    private static final TextEdit EDIT=new TextEdit();
    private static Screen owner;
    private static boolean focused;
    private static Rect bounds=new Rect(0,0,0,0);
    private ContainerSearchBox(){}

    public static void initialize(){
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof AbstractContainerScreen<?>))return;
            if(screen!=owner){owner=screen;focused=false;EDIT.set("");EDIT.set(initial());}
            ScreenEvents.afterExtract(screen).register((s,g,mouseX,mouseY,partial)->draw(s,g,mouseX,mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s,event)->click(event.x(),event.y(),event.button(),event.modifiers()));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s,event)->key(event));
            ScreenKeyboardEvents.allowCharType(screen).register((s,event)->{
                if(!active()||!focused)return true;
                if(event.isAllowedChatCharacter()){EDIT.insert(event.codepointAsString(),40);changed();}
                return false;
            });
            ScreenEvents.remove(screen).register(s->{if(s==owner){owner=null;focused=false;}});
        });
    }
    private static String initial(){var module=ContainerTools.search();return module==null?"":module.initialQuery();}
    private static boolean active(){return owner!=null&&ContainerTools.search()!=null;}
    private static void changed(){var module=ContainerTools.search();if(module!=null)module.query(EDIT.text());}
    /** The query to highlight for this screen, or blank. */
    public static String query(Screen screen){return screen==owner&&active()?EDIT.text():"";}

    private static Rect layout(Screen screen){
        var window=(ContainerScreenAccessor)screen;
        int left=window.vela$left(),top=window.vela$top(),right=left+window.vela$width();
        // Creative tabs sit above and below the window, so there the box goes beside it.
        if(screen instanceof CreativeModeInventoryScreen||top-H-4<2)return new Rect(right+4,top+2,W,H);
        return new Rect(right-W,top-H-4,W,H);
    }
    private static void draw(Screen screen,GuiGraphicsExtractor g,int mouseX,int mouseY){
        if(screen!=owner||!active())return;
        Rect r=bounds=layout(screen);
        boolean hover=r.contains(mouseX,mouseY);
        int border=focused?VelaTheme.PRIMARY:hover?VelaTheme.BORDER_HOVER:VelaTheme.BORDER;
        VelaDraw.roundedOutline(g,r.x(),r.y(),r.width(),r.height(),6,border,VelaTheme.SURFACE_DEEP);
        VelaIcons.draw(g,"search",r.x()+6,r.y()+5,8,focused?VelaTheme.PRIMARY:VelaTheme.TEXT_MUTED);
        int textRight=r.x()+r.width()-6;
        if(!EDIT.text().isBlank()){
            String count=String.valueOf(ContainerTools.count(((AbstractContainerScreen<?>)screen).getMenu(),EDIT.text()));
            int width=(int)Math.ceil(VelaDraw.textWidth(count,VelaDraw.Weight.MEDIUM,8f));
            VelaDraw.text(g,count,r.x()+r.width()-6f-width,r.y()+5f,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,8f);
            textRight-=width+4;
        }
        TextFieldDraw.draw(g,EDIT,new Rect(r.x()+18,r.y()+3,textRight-r.x()-18,14),focused,"Поиск");
    }
    private static boolean click(double x,double y,int button,int modifiers){
        if(!active())return true;
        if(!bounds.contains(x,y)){focused=false;return true;}
        focused=true;
        if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT){EDIT.set("");changed();}
        else TextFieldDraw.click(EDIT,new Rect(bounds.x()+18,bounds.y()+3,bounds.width()-24,14),x,(modifiers&GLFW.GLFW_MOD_SHIFT)!=0);
        return false;
    }
    private static boolean key(net.minecraft.client.input.KeyEvent event){
        if(!active())return true;
        boolean ctrl=(event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0;
        if(!focused){
            if(dev.sqwat.vela.ui.input.BindInput.matches(ContainerTools.search().searchKey(),event.key(),event.modifiers())){focused=true;EDIT.selectAll();return false;}
            return true;
        }
        switch(event.key()){
            case GLFW.GLFW_KEY_ESCAPE->{return true;}
            case GLFW.GLFW_KEY_ENTER,GLFW.GLFW_KEY_KP_ENTER,GLFW.GLFW_KEY_TAB->focused=false;
            default->{if(GameTextInput.key(EDIT,event,40))changed();}
        }
        return false;
    }
}
