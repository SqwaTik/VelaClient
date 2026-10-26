package dev.sqwat.vela;
import dev.sqwat.vela.ui.render.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MarkerGeometryTest {
    @Test void creationContainsEditorAndAction(){for(int width:new int[]{170,190,320,600})for(float p:new float[]{0,.25f,.5f,.75f,1}){var c=new MarkerGeometry.Creation(10,20,width,p);assertTrue(c.action().y()+c.action().height()<c.frame().y()+c.frame().height());assertTrue(c.action().y()>=c.editor().y()+c.editorHeight());}}
    @Test void iconHitsNeverOverlapAndStayInsideStrip(){for(int w:new int[]{170,190,320,600}){var e=new MarkerGeometry.Editor(20,30,w);for(int i=0;i<8;i++){Rect r=e.icon(i);assertTrue(r.width()>=16);assertTrue(r.x()+r.width()<=e.icons().x()+e.icons().width());if(i>0)assertTrue(e.icon(i-1).x()+e.icon(i-1).width()<=r.x());}}}
    @Test void settingsGroupsDoNotOverlapDuringExpansion(){for(float p:new float[]{0,.1f,.5f,1}){var g=MarkerSettingsGeometry.place(new Rect(0,0,172,235),p,p,p);assertTrue(g.death().y()+g.death().height()<g.limit().y());assertTrue(g.limit().y()+g.limit().height()<g.quick().y());assertTrue(g.bottom()<=235);}}
}
