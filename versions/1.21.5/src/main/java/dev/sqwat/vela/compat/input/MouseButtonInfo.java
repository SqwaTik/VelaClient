package dev.sqwat.vela.compat.input;

/** 1.21.9's mouse button record, for the code written against it: 1.21.5 passes the button and reads modifiers itself. */
public record MouseButtonInfo(int button,int modifiers) {
    public boolean hasShiftDown(){return Modifiers.shift(modifiers);}
    public boolean hasControlDown(){return Modifiers.control(modifiers);}
    public boolean hasAltDown(){return Modifiers.alt(modifiers);}
}
