package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class FarewellScreen extends VelaScreen {
    private final long started=System.nanoTime();
    private boolean stopped;
    private static final String[] PHRASES={"До встречи!","Заходите ещё","Удачной игры!","Увидимся в новом мире","Спасибо за игру"};
    private final String phrase=PHRASES[java.util.concurrent.ThreadLocalRandom.current().nextInt(PHRASES.length)];
    public FarewellScreen(){super(Component.literal(dev.sqwat.vela.ui.Lang.tr("До встречи")));}
    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.fillGradient(0,0,width,height,0xFF111725,0xFF070A10);
        int center=width/2,top=height/2;
        VelaDraw.logo(g,center-22,top-65,44);
        VelaDraw.centeredText(g,"Vela "+net.minecraft.SharedConstants.getCurrentVersion().name(),center,top,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,20);
        VelaDraw.centeredText(g,phrase,center,top+32,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,9);
    }
    @Override public void tick(){if(!stopped&&System.nanoTime()-started>=650_000_000L){stopped=true;minecraft.stop();}}
    @Override public boolean shouldCloseOnEsc(){return false;}
}
