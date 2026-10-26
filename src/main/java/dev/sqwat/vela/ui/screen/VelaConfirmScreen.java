package dev.sqwat.vela.ui.screen;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

public final class VelaConfirmScreen extends VelaScreen {
    private final VelaScreen parent;private final String name;private final Supplier<String> action;
    private final UiControls controls=new UiControls();private Rect confirm=new Rect(0,0,0,0),cancel=confirm;private String error="";
    public VelaConfirmScreen(VelaScreen parent,String name,Supplier<String> action){super(Component.literal(dev.sqwat.vela.ui.Lang.tr("Удаление конфигурации")));this.parent=parent;this.name=name;this.action=action;}
    @Override protected float interfaceScale(){return parent.interfaceScale();}
    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        parent.renderAsBackdrop(g,width,height,delta);g.nextStratum();g.fill(0,0,width,height,0xB805070A);
        int x=(width-240)/2,y=(height-112)/2;
        VelaDraw.roundedOutline(g,x,y,240,112,8,VelaTheme.BORDER,VelaTheme.SURFACE);
        VelaIcons.draw(g,"trash",x+12,y+13,14,VelaTheme.DANGER);
        VelaDraw.text(g,"Удалить конфигурацию?",x+34,y+15,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,10);
        VelaDraw.text(g,VelaDraw.ellipsize(name,214,VelaDraw.Weight.REGULAR,8),x+13,y+39,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
        if(!error.isBlank())VelaDraw.text(g,VelaDraw.ellipsize(error,214,VelaDraw.Weight.REGULAR,7),x+13,y+56,VelaTheme.DANGER,VelaDraw.Weight.REGULAR,7);
        cancel=new Rect(x+12,y+77,103,24);confirm=new Rect(x+123,y+77,105,24);
        controls.button(g,"cancel",cancel,"Отмена",cancel.contains(mx,my));controls.button(g,"confirm",confirm,"Удалить",confirm.contains(mx,my));
    }
    @Override protected boolean clickVela(MouseButtonEvent e,boolean twice){if(e.button()!=0)return true;if(cancel.contains(e.x(),e.y()))onClose();else if(confirm.contains(e.x(),e.y())){error=action.get();if(error==null||error.isBlank())onClose();}return true;}
    @Override public boolean keyPressed(KeyEvent e){if(e.key()==256)onClose();return true;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
