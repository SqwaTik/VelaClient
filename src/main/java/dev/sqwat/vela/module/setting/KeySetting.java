package dev.sqwat.vela.module.setting;
public final class KeySetting extends Setting<Integer> {
    public KeySetting(String id,String name){super(id,name,"Нажмите для назначения клавиши",-1);}
    /** {@code key}: GLFW key code (or mouse code from BindInput) used until the user rebinds it. */
    public KeySetting(String id,String name,int key){super(id,name,"Нажмите для назначения клавиши",key);}
    /** Accepts a key, a mouse button or a combination of either with Ctrl/Shift/Alt (BindInput). */
    @Override protected Integer validate(Integer value){
        if(value==-1)return value;
        int key=dev.sqwat.vela.ui.input.BindInput.base(value);
        if((key<32||key>348)&&!dev.sqwat.vela.ui.input.BindInput.isMouse(key)||value>>>19!=0)throw new IllegalArgumentException("Invalid key");
        return value;
    }
}
