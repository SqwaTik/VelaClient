package dev.sqwat.vela.ui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Shared design coordinates for drawing, clipping and all pointer input. */
public abstract class VelaScreen extends Screen {
    private float scale=1;
    protected VelaScreen(Component title){super(title);}
    @Override public final void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        int gameScale=Math.max(1,minecraft.getWindow().getGuiScale());
        var viewport=dev.sqwat.vela.ui.render.UiViewport.fit(g.guiWidth(),g.guiHeight(),gameScale,interfaceScale());
        scale=viewport.scale();width=viewport.width();height=viewport.height();
        boolean previousSubpixel=dev.sqwat.vela.ui.render.VelaDraw.subpixelText(true);
        g.pose().pushMatrix();g.pose().scale(scale);
        try{renderVela(g,Math.round(mx/scale),Math.round(my/scale),delta);dev.sqwat.vela.ui.LangTransition.draw(g,width,height);}finally{g.pose().popMatrix();dev.sqwat.vela.ui.render.VelaDraw.subpixelText(previousSubpixel);}
    }
    public final void renderAsBackdrop(GuiGraphicsExtractor g,int logicalWidth,int logicalHeight,float delta){
        width=logicalWidth;height=logicalHeight;renderVela(g,-10000,-10000,delta);
    }
    protected abstract void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta);
    @Override public void removed(){dev.sqwat.vela.ui.render.UiCursor.hand(false);super.removed();}
    protected float interfaceScale(){return 1;}
    protected final float renderScale(){return scale;}
    private MouseButtonEvent local(MouseButtonEvent e){return new MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo());}
    @Override public final boolean mouseClicked(MouseButtonEvent e,boolean doubled){return clickVela(local(e),doubled);}
    protected boolean clickVela(MouseButtonEvent e,boolean doubled){return super.mouseClicked(e,doubled);}
    @Override public final boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return dragVela(local(e),dx/scale,dy/scale);}
    protected boolean dragVela(MouseButtonEvent e,double dx,double dy){return super.mouseDragged(e,dx,dy);}
    @Override public final boolean mouseReleased(MouseButtonEvent e){return releaseVela(local(e));}
    protected boolean releaseVela(MouseButtonEvent e){return super.mouseReleased(e);}
    @Override public final boolean mouseScrolled(double x,double y,double h,double v){return scrollVela(x/scale,y/scale,h,v);}
    protected boolean scrollVela(double x,double y,double h,double v){return super.mouseScrolled(x,y,h,v);}
}
