import dev.sqwat.vela.ui.render.*;
import java.nio.file.*;
public class RenderSegmentPreview {
    public static void main(String[] args)throws Exception{
        var canvas=new RenderPngPreview(Path.of(args[0]),660,330,2);
        for(int row=0;row<3;row++){
            Rect bounds=new Rect(18,22+row*46,290,21);
            canvas.round(bounds.x(),bounds.y(),bounds.width(),bounds.height(),5,VelaTheme.SURFACE_DEEP);
            SegmentedControl.draw(canvas,SegmentedControl.segment(bounds,0,2),"Attack",row<2?1:0,0);
            SegmentedControl.draw(canvas,SegmentedControl.segment(bounds,1,2),"Use",row>0?1:0,0);
        }
        canvas.save(Path.of(args[1]));
    }
}
