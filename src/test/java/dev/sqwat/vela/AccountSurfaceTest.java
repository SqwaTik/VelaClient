package dev.sqwat.vela;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AccountSurfaceTest {
    @Test void modalAndProfileHaveIndependentVectorBorders(){
        var borders=new java.util.ArrayList<dev.sqwat.vela.ui.render.Rect>();
        var canvas=(dev.sqwat.vela.ui.render.UiCanvas)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{dev.sqwat.vela.ui.render.UiCanvas.class},(proxy,method,args)->{
            if(method.getName().equals("border"))borders.add(new dev.sqwat.vela.ui.render.Rect(Math.round((float)args[0]),Math.round((float)args[1]),Math.round((float)args[2]),Math.round((float)args[3])));
            if(method.getName().equals("density"))return 1.5f;
            if(method.getName().equals("textWidth"))return ((String)args[0]).length()*4f;
            return null;
        });
        var layout=dev.sqwat.vela.ui.render.AccountChrome.layout(854,480);
        var dialog=dev.sqwat.vela.ui.render.AccountChrome.authDialog(layout);
        dev.sqwat.vela.ui.render.AccountChrome.drawAuth(canvas,layout,dialog,true);
        assertTrue(borders.contains(dialog.panel()),"Modal border must not depend on downscaled PNG edge pixels");
        borders.clear();
        dev.sqwat.vela.ui.render.AccountChrome.drawProfile(canvas,layout,"Demo","uuid",false,false,0);
        var r=layout.details();
        assertTrue(borders.contains(new dev.sqwat.vela.ui.render.Rect(r.x(),r.y(),r.width(),84)));
    }
    @Test void rasterBordersHaveBothOppositeEdges()throws Exception{
        for(String name:new String[]{"account_hero","account_modal"}){
            try(var stream=getClass().getResourceAsStream("/assets/velaclient/textures/gui/"+name+".png")){
                var image=javax.imageio.ImageIO.read(stream);
                int w=image.getWidth(),h=image.getHeight();
                assertTrue((image.getRGB(w-1,h/2)>>>24)>20,name+" right border");
                assertTrue((image.getRGB(w/2,h-1)>>>24)>20,name+" bottom border");
                assertEquals(image.getRGB(0,h/2)>>>24,image.getRGB(w-1,h/2)>>>24,name+" symmetric edge alpha");
            }
        }
    }
}
