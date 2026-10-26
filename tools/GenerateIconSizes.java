import java.nio.file.*;
import javax.imageio.ImageIO;

/** Prefilter every small physical icon size from the existing master assets. */
public class GenerateIconSizes {
    public static void main(String[] args)throws Exception{
        Path gui=Path.of(args[0]);
        for(String name:new String[]{"account","visuals","hud","utilities","markers","friends","configs","settings","search","arrow","back","x","plus","check","trash","copy","folder","key","star","star_filled","download","save","move","microsoft","chevron"}){
            var master=ImageIO.read(gui.resolve("icon_"+name+".png").toFile());
            for(int size=8;size<=64;size++){
                GeneratePngAssets.write(GeneratePngAssets.resize(master,size,size,true),gui.resolve("icon_"+name+"_"+size+".png"));
            }
        }
    }
}
