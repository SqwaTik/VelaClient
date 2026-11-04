package dev.sqwat.vela.compat.input;

/** 1.21.9's click record; 1.21.5's screen methods take (x, y, button), turned into one of these (see manual.txt). */
public record MouseButtonEvent(double x,double y,MouseButtonInfo buttonInfo) {
    public MouseButtonEvent(double x,double y,int button){this(x,y,new MouseButtonInfo(button,Modifiers.current()));}
    public int button(){return buttonInfo.button();}
    public int modifiers(){return buttonInfo.modifiers();}
    public boolean hasShiftDown(){return buttonInfo.hasShiftDown();}
    public boolean hasControlDown(){return buttonInfo.hasControlDown();}
    public boolean hasAltDown(){return buttonInfo.hasAltDown();}
}
