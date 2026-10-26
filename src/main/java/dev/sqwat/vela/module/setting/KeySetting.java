package dev.sqwat.vela.module.setting;
public final class KeySetting extends Setting<Integer> {
    public KeySetting(String id,String name){super(id,name,"Нажмите для назначения клавиши",-1);}
    /** {@code key}: GLFW key code (or mouse code from BindInput) used until the user rebinds it. */
    public KeySetting(String id,String name,int key){super(id,name,"Нажмите для назначения клавиши",key);}
    @Override protected Integer validate(Integer value){if(value!=-1&&(value<32||value>348)&&!dev.sqwat.vela.ui.input.BindInput.isMouse(value))throw new IllegalArgumentException("Invalid key");return value;}
}
