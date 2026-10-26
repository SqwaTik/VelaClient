package dev.sqwat.vela.module.setting;
public final class ActionSetting extends Setting<String> {
    private final Runnable action;
    public ActionSetting(String id,String name){this(id,name,null);}
    /** {@code action} runs when the button is clicked; settings without one are handled by their owning screen. */
    public ActionSetting(String id,String name,Runnable action){super(id,name,"","");this.action=action;}
    public boolean run(){if(action==null)return false;action.run();return true;}
}
