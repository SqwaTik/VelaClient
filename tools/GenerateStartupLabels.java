import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Fixed startup labels usable before resource-pack initialization. */
public class GenerateStartupLabels {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]),out=root.resolve("textures/gui");
        Font regular=Font.createFont(Font.TRUETYPE_FONT,root.resolve("font/inter_regular.ttf").toFile());
        Font semibold=Font.createFont(Font.TRUETYPE_FONT,root.resolve("font/inter_semibold.ttf").toFile());
        String[] names={"title","resources","initializing","ready","goodbye","leaving"},labels={"Загрузка","Загрузка ресурсов","Подготовка интерфейса","Готово","До встречи","Завершаем сеанс…"};
        for(int i=0;i<names.length;i++){
            BufferedImage image=new BufferedImage(768,96,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont((i==0?semibold:regular).deriveFont(i==0?57f:30f));g.setColor(Color.WHITE);g.drawString(labels[i],(768-g.getFontMetrics().stringWidth(labels[i]))/2f,65);g.dispose();
            for(int y=0;y<96;y++)for(int x=0;x<768;x++)image.setRGB(x,y,image.getRGB(x,y)|0xFFFFFF);
            ImageIO.write(image,"png",out.resolve("startup_"+names[i]+".png").toFile());
        }
        var digits=new BufferedImage(792,96,BufferedImage.TYPE_INT_ARGB);var g=digits.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(regular.deriveFont(24f));g.setColor(Color.WHITE);
        for(int i=0;i<11;i++){String value=i==10?"%":Integer.toString(i);g.drawString(value,i*72+(72-g.getFontMetrics().stringWidth(value))/2,55);}
        g.dispose();for(int y=0;y<96;y++)for(int x=0;x<792;x++)digits.setRGB(x,y,digits.getRGB(x,y)|0xffffff);
        ImageIO.write(digits,"png",out.resolve("startup_digits.png").toFile());
    }
}
