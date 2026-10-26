package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
/** Uses Minecraft's normal name-tag extraction and renderer. */
public final class SelfTag extends ClientModule {
    public SelfTag(){super("self_tag","Self Tag","Свой ник над персонажем от третьего лица",ModuleCategory.VISUALS);}
    public boolean visible(boolean own,boolean firstPerson,boolean hidden,boolean alive){return enabled()&&own&&!firstPerson&&!hidden&&alive;}
}
