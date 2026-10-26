package dev.sqwat.vela;

import dev.sqwat.vela.core.ConfigService;
import dev.sqwat.vela.module.*;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModuleSettingsInteractionTest {
    @TempDir Path root;
    private static BooleanSetting flag(){return new BooleanSetting("transparent","Прозрачный","",false);}
    private static Rect card(int x,int y,List<Setting<?>> settings){return new Rect(x,y,220,44+ModuleSettingsGeometry.extraHeight(settings));}
    private static Rect viewport(){return new Rect(0,0,640,500);}
    private static Rect hit(Rect card,int cursor,Setting<?> setting,Rect viewport){
        return ModuleSettingsGeometry.visibleHit(ModuleSettingsGeometry.row(card.x()+8,cursor,card.width()-16,setting),card,viewport);
    }
    @Test void reproducesOldOnePixelErrorAndKeepsWholeCheckboxClickable(){
        var setting=flag();int cursor=52;
        Rect row=ModuleSettingsGeometry.row(8,cursor,204,setting);
        int oldCardHeight=44+4+20;
        assertEquals(1,row.y()+row.height()-(oldCardHeight-2));
        Rect c=card(0,0,List.of(setting));Rect target=hit(c,cursor,setting,viewport());
        Rect checkbox=ModuleSettingsGeometry.checkbox(8,cursor,204);
        assertEquals(row,target);
        for(int x=checkbox.x();x<checkbox.x()+checkbox.width();x++)
            for(int y=checkbox.y();y<checkbox.y()+checkbox.height();y++)assertTrue(target.contains(x+.5,y+.5));
    }
    @Test void lastBooleanWorksAfterNumbersChoicesAndOtherBooleans(){
        List<Setting<?>> settings=List.of(new NumberSetting("size","Size","",1,0,2,.1),new ChoiceSetting("style","Style","","A",List.of("A","B")),new BooleanSetting("background","Background","",true),flag());
        for(int columnX:new int[]{12,240})for(int scroll:new int[]{0,31,75}){
            Rect c=card(columnX,90-scroll,settings);int cursor=c.y()+52;
            for(var setting:settings){
                Rect target=hit(c,cursor,setting,viewport());
                if(setting instanceof BooleanSetting){Rect checkbox=ModuleSettingsGeometry.checkbox(c.x()+8,cursor,c.width()-16);assertTrue(target.contains(checkbox.x()+5,checkbox.y()+5));}
                cursor+=ModuleSettingsGeometry.rowHeight(setting);
            }
        }
    }
    @Test void partiallyVisibleRowKeepsOnlyVisiblePointerArea(){
        var setting=flag();Rect c=new Rect(0,0,220,60);
        Rect clipped=hit(c,52,setting,viewport());
        assertEquals(new Rect(8,50,204,8),clipped);
        assertTrue(clipped.contains(200,55));assertFalse(clipped.contains(200,59));
        Rect above=hit(card(0,0,List.of(setting)),52,setting,new Rect(0,56,640,444));
        assertFalse(above.contains(200,55));assertTrue(above.contains(200,60));
    }
    @Test void hiddenRowCannotInterceptAClick(){
        Rect target=hit(new Rect(0,0,220,44),52,flag(),viewport());
        assertEquals(0,target.height());assertFalse(target.contains(200,52));
    }
    @Test void sliderAndChoiceTargetsFitAtFullExpansion(){
        var number=new NumberSetting("limit","Limit","",50,0,100,1);
        var choice=new ChoiceSetting("style","Style","","A",List.of("A","B"));
        Rect c=card(10,20,List.of(number,choice,flag()));int cursor=c.y()+52;
        Rect track=new Rect(c.x()+11,cursor+12,c.width()-22,13);
        assertEquals(track,ModuleSettingsGeometry.visibleHit(track,c,viewport()));
        cursor+=ModuleSettingsGeometry.rowHeight(number);
        Rect dropdown=new Rect(c.x()+8,cursor+14,c.width()-16,21);
        assertEquals(dropdown,ModuleSettingsGeometry.visibleHit(dropdown,c,viewport()));
        Rect clippedTrack=ModuleSettingsGeometry.visibleHit(track,c,new Rect(track.x()+20,0,620,500));
        assertEquals(track.x()+20,clippedTrack.x());
        // The original track remains the value-mapping geometry, even when its hit area is clipped.
        number.setFraction((track.x()+track.width()/2.0-track.x())/track.width());assertEquals(50,number.value());
    }
    private static class Sample extends ClientModule {
        final BooleanSetting flag=setting(ModuleSettingsInteractionTest.flag());
        Sample(){super("sample","Sample","",ModuleCategory.HUD);}
    }
    @Test void clickingCheckboxValueSurvivesConfigReload(){
        var registry=new ModuleRegistry();var module=registry.register(new Sample());
        try(var config=new ConfigService(root,registry)){
            Rect c=card(10,20,module.settings());int cursor=c.y()+52;
            Rect checkbox=ModuleSettingsGeometry.checkbox(c.x()+8,cursor,c.width()-16);
            Rect target=hit(c,cursor,module.flag,viewport());
            assertTrue(target.contains(checkbox.x()+5,checkbox.y()+5));
            module.flag.toggle();config.requestSave();assertTrue(module.flag.value());
        }
        var restoredRegistry=new ModuleRegistry();var restored=restoredRegistry.register(new Sample());
        try(var config=new ConfigService(root,restoredRegistry)){assertTrue(restored.flag.value());}
    }
}
