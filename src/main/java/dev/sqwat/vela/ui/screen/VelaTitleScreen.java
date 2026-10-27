package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.ui.animation.FrameClock;
import dev.sqwat.vela.ui.animation.Motion;
import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class VelaTitleScreen extends VelaScreen {
    private final VelaServices services;
    private final FrameClock clock = new FrameClock();
    private final List<MenuHit> hits = new ArrayList<>();
    private float entrance;
    private final float[] buttonHover=new float[5];
    private long clockSecond=-1;
    private String timeLabel="",dateLabel="";
    private static final java.time.format.DateTimeFormatter TIME=java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final java.time.format.DateTimeFormatter DATE_EN=java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM",java.util.Locale.ENGLISH);
    private static final java.time.format.DateTimeFormatter DATE=java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM",java.util.Locale.forLanguageTag("ru"));

    public VelaTitleScreen(VelaServices services) {
        super(Component.literal("Vela"));
        this.services = services;
    }

    @Override
    public void renderVela(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        VelaTheme.setPrimary(VelaTheme.parseHex(services.configs().current().accentColor));
        float dt = clock.step();
        entrance = Motion.approach(entrance, 1, services.configs().current().reducedMotion ? 60 : 13, dt);
        drawBackground(g);

        int center=width/2;
        int y=height/2-75+Math.round((1-Motion.easeOutCubic(entrance))*5);
        VelaDraw.logo(g,center-14,y-43,28);
        long second=System.currentTimeMillis()/1000;
        if(second!=clockSecond){clockSecond=second;var now=java.time.LocalDateTime.now();timeLabel=now.format(TIME);dateLabel=now.format(dev.sqwat.vela.ui.Lang.english()?DATE_EN:DATE);}
        VelaDraw.centeredText(g,timeLabel,center,y,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,36);
        VelaDraw.centeredText(g,dateLabel,center,y+45,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,9);
        hits.clear();
        String[] labels={"Singleplayer","Multiplayer","Accounts","Settings","Quit"};
        PILL_W=Math.max(96,Math.round(maxLabel(labels)+50));ROW=PILL_W*4+PILL_GAP*3;
        String[] icons={"singleplayer","multiplayer","account","settings","power"};
        Action[] actions={Action.SINGLEPLAYER,Action.MULTIPLAYER,Action.ACCOUNTS,Action.SETTINGS,Action.QUIT};
        for(int i=0;i<5;i++){
            int slot=i<2?i:i-1;
            Rect rect=i==2?new Rect(width-140,12,128,30):new Rect(center-ROW/2+slot*(PILL_W+PILL_GAP),y+86,PILL_W,PILL_H);
            boolean hovered=rect.contains(mouseX,mouseY)||i==2&&minecraft.gui.screen() instanceof VelaAccountScreen;
            buttonHover[i]=Motion.approach(buttonHover[i],hovered?1:0,services.configs().current().reducedMotion?60:13,dt);
            float h=buttonHover[i];
            if(i==2){
                VelaDraw.roundedRect(g,rect.x(),rect.y(),rect.width(),rect.height(),8,dev.sqwat.vela.ui.render.AccountChrome.selection(false,h));
                dev.sqwat.vela.ui.hud.PlayerAvatars.draw(g,minecraft.getUser().getName(),rect.x()+6,rect.y()+4,22);
                VelaDraw.text(g,VelaDraw.ellipsize(minecraft.getUser().getName(),73,VelaDraw.Weight.MEDIUM,9),rect.x()+33,rect.y()+10,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);
                float accountReveal=minecraft.gui.screen() instanceof VelaAccountScreen account?account.revealProgress():0;
                g.pose().pushMatrix();g.pose().translate(rect.x()+116,rect.y()+15);
                g.pose().rotate(dev.sqwat.vela.ui.render.ControlChrome.disclosureAngle(accountReveal));
                dev.sqwat.vela.ui.render.VelaIcons.draw(g,"chevron",-5,-5,10,VelaTheme.PRIMARY);
                g.pose().popMatrix();
                hits.add(new MenuHit(rect,actions[i]));continue;
            }
            // Labelled pill: the hover is a clear accent border, a tinted fill and a small lift; Quit turns red.
            int accent=i==4?VelaTheme.DANGER:VelaTheme.PRIMARY;
            float lift=h*2;
            Rect pill=new Rect(rect.x(),Math.round(rect.y()-lift),rect.width(),rect.height());
            if(h>.01f)VelaDraw.roundedRect(g,pill.x()-3,pill.y()-3,pill.width()+6,pill.height()+6,13,(Math.round(h*40)<<24)|(accent&0xFFFFFF));
            VelaDraw.roundedOutline(g,pill.x(),pill.y(),pill.width(),pill.height(),11,VelaTheme.mix(0xFF262E3F,accent,h),VelaTheme.mix(0xE0121722,VelaTheme.mix(0xFF121722,accent,.16f),h));
            dev.sqwat.vela.ui.render.VelaIcons.draw(g,icons[i],pill.x()+14,pill.y()+(pill.height()-14)/2,14,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,accent,h));
            VelaDraw.text(g,labels[i],pill.x()+35,pill.y()+(pill.height()-9)/2f,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,h),VelaDraw.Weight.MEDIUM,9);
            hits.add(new MenuHit(rect,actions[i]));
        }
        dev.sqwat.vela.ui.render.UiCursor.hand(hits.stream().anyMatch(hit->hit.rect.contains(mouseX,mouseY))||changelogHit.contains(mouseX,mouseY));
        drawChangelogPill(g,mouseX,mouseY,dt);
    }
    private Rect changelogHit=new Rect(0,0,0,0);
    private float changelogHover;
    /** "Vela 0.2.0 · What's new" at the bottom; a pulsing dot until the newest release notes were opened. */
    private void drawChangelogPill(GuiGraphicsExtractor g,int mouseX,int mouseY,float dt){
        String label="Vela "+dev.sqwat.vela.ui.Changelog.current()+"  ·  "+dev.sqwat.vela.ui.Lang.pick("What's new","Что нового");
        boolean unseen=ChangelogScreen.unseen();
        int textWidth=(int)Math.ceil(VelaDraw.textWidth(label,VelaDraw.Weight.MEDIUM,8f));
        int w=textWidth+20+(unseen?10:0),h=18,x=(width-w)/2,y=height-26;
        changelogHit=new Rect(x,y,w,h);
        changelogHover=Motion.approach(changelogHover,changelogHit.contains(mouseX,mouseY)?1:0,services.configs().current().reducedMotion?60:13,dt);
        VelaDraw.roundedOutline(g,x,y,w,h,9,VelaTheme.mix(VelaTheme.BORDER,VelaTheme.PRIMARY,changelogHover*.7f),VelaTheme.mix(VelaTheme.SURFACE_DEEP,VelaTheme.SURFACE_HOVER,changelogHover));
        int tx=x+10;
        if(unseen){
            float pulse=.55f+.45f*(float)Math.sin(System.currentTimeMillis()/1000.0*Math.PI*1.4);
            VelaDraw.roundedRect(g,tx-1,y+6,6,6,3,(Math.round(80*pulse)<<24)|(VelaTheme.PRIMARY&0xFFFFFF));
            VelaDraw.roundedRect(g,tx,y+7,4,4,2,VelaTheme.PRIMARY);tx+=10;
        }
        VelaDraw.text(g,label,tx,y+5,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,changelogHover),VelaDraw.Weight.MEDIUM,8f);
    }

    private static final int PILL_H=34,PILL_GAP=10;
    private static final net.minecraft.resources.Identifier GLOW=net.minecraft.resources.Identifier.fromNamespaceAndPath("velaclient","textures/gui/glow.png");
    private int PILL_W=104,ROW=PILL_W*4+PILL_GAP*3;
    private static float maxLabel(String[] labels){float m=0;for(int i=0;i<labels.length;i++)if(i!=2)m=Math.max(m,VelaDraw.textWidth(labels[i],VelaDraw.Weight.MEDIUM,9));return m;}
    /**
     * Deep base with three soft light blobs in the accent colour drifting slowly (one radial-glow quad each), and a
     * vignette that keeps the edges dark.
     */
    private void drawBackground(GuiGraphicsExtractor g) {
        g.fillGradient(0,0,width,height,0xFF0B0F17,0xFF05070B);
        double t=System.currentTimeMillis()/1000.0;
        boolean still=services.configs().current().reducedMotion;
        int accent=VelaTheme.PRIMARY&0xFFFFFF;
        float[][] blobs={{.28f,.30f,.42f,0},{.74f,.62f,.38f,2.1f},{.52f,.92f,.50f,4.2f}};
        for(int b=0;b<blobs.length;b++){
            float phase=blobs[b][3];
            float cx=width*(blobs[b][0]+(still?0:(float)Math.sin(t*.07+phase)*.05f)),cy=height*(blobs[b][1]+(still?0:(float)Math.cos(t*.06+phase)*.05f));
            float radius=Math.max(width,height)*blobs[b][2];
            int tint=b==1?VelaTheme.mix(accent|0xFF000000,0xFF43C7F4,.45f)&0xFFFFFF:accent;
            // One soft radial texture, tinted and scaled: a real round glow in a single quad.
            VelaDraw.blit(g,GLOW,cx-radius,cy-radius,radius*2,radius*2,0,0,256,256,256,256,(b==1?0x30000000:0x3C000000)|tint);
        }
        // Vignette
        g.fillGradient(0,0,width,height/3,0xB005070B,0x0005070B);
        g.fillGradient(0,height*2/3,width,height,0x0005070B,0xC005070B);
    }

    @Override
    public boolean clickVela(MouseButtonEvent event, boolean doubled) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.clickVela(event, doubled);
        if(changelogHit.contains(event.x(),event.y())){minecraft.gui.setScreen(new ChangelogScreen(this));return true;}
        for (MenuHit hit : hits) {
            if (!hit.rect().contains(event.x(), event.y())) continue;
            switch (hit.action()) {
                case SINGLEPLAYER -> minecraft.gui.setScreen(new SelectWorldScreen(this));
                case MULTIPLAYER -> minecraft.gui.setScreen(new JoinMultiplayerScreen(this));
                case ACCOUNTS -> minecraft.gui.setScreen(new VelaAccountScreen(this, services));
                case SETTINGS -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options, false));
                case QUIT -> minecraft.gui.setScreen(new FarewellScreen());
            }
            return true;
        }
        return true;
    }

    @Override protected float interfaceScale(){return services.configs().current().uiScale;}
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    private enum Action { SINGLEPLAYER, MULTIPLAYER, ACCOUNTS, SETTINGS, QUIT }
    private record MenuHit(Rect rect, Action action) {}
}
