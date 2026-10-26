package dev.sqwat.vela.module.setting;
/** Stored as a 24-bit RGB integer in the existing settings map. */
public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String id,String name,int rgb){super(id,name,"Цвет RGB",rgb&0xFFFFFF);}
    @Override protected Integer validate(Integer next){if(next<0||next>0xFFFFFF)throw new IllegalArgumentException("RGB out of range");return next;}
}
