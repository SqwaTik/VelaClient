import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/** Lossless white-mask PNG compaction. Dry-run unless --apply is supplied. */
public class CompactFontPng {
    public static void main(String[] args) throws Exception {
        if(args.length<1 || args.length>2 || (args.length==2&&!args[1].equals("--apply")))
            throw new IllegalArgumentException("Usage: CompactFontPng <gui-directory> [--apply]");
        boolean apply=args.length==2;
        long before=0,after=0;int changed=0;
        byte[] white=new byte[256],alpha=new byte[256];Arrays.fill(white,(byte)255);
        for(int i=0;i<256;i++)alpha[i]=(byte)i;
        var palette=new IndexColorModel(8,256,white,white,white,alpha);
        try(var paths=Files.list(Path.of(args[0]))){
            for(Path path:paths.filter(p->p.getFileName().toString().matches("font_(regular|medium|semibold)_[0-9]+\\.png")).sorted().toList()){
                byte[] original=Files.readAllBytes(path);
                var source=ImageIO.read(new ByteArrayInputStream(original));
                int w=source.getWidth(),h=source.getHeight();
                var image=new BufferedImage(w,h,BufferedImage.TYPE_BYTE_INDEXED,palette);
                for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                    int pixel=source.getRGB(x,y);
                    if((pixel&0xffffff)!=0xffffff)throw new IOException("Non-white font mask: "+path);
                    image.getRaster().setSample(x,y,0,pixel>>>24);
                }
                var bytes=new ByteArrayOutputStream();var writer=ImageIO.getImageWritersByFormatName("png").next();
                try(var output=new MemoryCacheImageOutputStream(bytes)){
                    writer.setOutput(output);var params=writer.getDefaultWriteParam();
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);params.setCompressionQuality(0);
                    writer.write(null,new IIOImage(image,null,null),params);
                }finally{writer.dispose();}
                byte[] candidate=bytes.toByteArray();var decoded=ImageIO.read(new ByteArrayInputStream(candidate));
                if(!Arrays.equals(source.getRGB(0,0,w,h,null,0,w),decoded.getRGB(0,0,w,h,null,0,w)))
                    throw new IOException("Pixel round-trip mismatch: "+path);
                before+=original.length;
                if(candidate.length<original.length){
                    after+=candidate.length;changed++;
                    if(apply)Files.write(path,candidate);
                }else after+=original.length;
            }
        }
        System.out.printf(Locale.ROOT,"%s: %d fonts; %,d -> %,d bytes (%.1f%% saved); exact RGBA verified%n",
                apply?"Applied":"Dry run",changed,before,after,100.0*(before-after)/Math.max(1,before));
    }
}
