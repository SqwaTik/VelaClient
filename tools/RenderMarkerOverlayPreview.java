import dev.sqwat.vela.ui.render.*;
import java.nio.file.*;

public class RenderMarkerOverlayPreview {
    public static void main(String[] args)throws Exception{
        Path assets=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);
        for(float density:new float[]{1,1.5f,2}){
            var c=new RenderPngPreview(assets,Math.round(500*density),Math.round(200*density),density);
            for(int i=0;i<3;i++){
                var transform=c.graphics.getTransform();c.graphics.translate((60+i*110)*density,80*density);
                MarkerOverlayChrome.label(c,"Метка 4",new String[]{"1m","84m","19037m"}[i],"markers",0xFFFF8F33);c.graphics.setTransform(transform);
            }
            Rect button=MarkerOverlayChrome.notice(c,328,118,160,"Метка 4","markers",0xFFFF8F33);
            c.round(button.x(),button.y(),button.width(),button.height(),6,VelaTheme.SURFACE_DEEP);
            c.centered("Удалить",button.x()+button.width()/2f,button.y()+7,8,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
            c.save(out.resolve("marker-overlay-"+density+".png"));
        }
    }
}
