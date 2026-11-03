package dev.sqwat.vela.media;

/** White control glyphs of the Music panel, painted in code and 4×4 supersampled; tinted when drawn. */
public enum MusicGlyph {
    PLAY,PAUSE,NEXT,PREV,MUTED;

    /** {@code size}×{@code size} ARGB pixels, white with coverage as alpha. */
    public int[] paint(int size){
        int[] pixels=new int[size*size];
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            int hits=0;
            for(int sy=0;sy<4;sy++)for(int sx=0;sx<4;sx++)if(inside((x+(sx+.5f)/4)/size,(y+(sy+.5f)/4)/size))hits++;
            pixels[y*size+x]=Math.round(hits/16f*255)<<24|0xFFFFFF;
        }
        return pixels;
    }
    boolean inside(float x,float y){
        return switch(this){
            case PLAY->triangle(x,y,.26f,.16f,.26f,.84f,.86f,.5f);
            case PAUSE->rect(x,y,.22f,.16f,.41f,.84f)||rect(x,y,.59f,.16f,.78f,.84f);
            case NEXT->triangle(x,y,.14f,.18f,.14f,.82f,.66f,.5f)||rect(x,y,.70f,.18f,.84f,.82f);
            case PREV->triangle(1-x,y,.14f,.18f,.14f,.82f,.66f,.5f)||rect(1-x,y,.70f,.18f,.84f,.82f);
            case MUTED->rect(x,y,.08f,.36f,.28f,.64f)||triangle(x,y,.16f,.5f,.50f,.12f,.50f,.88f)
                ||line(x,y,.62f,.34f,.92f,.66f,.07f)||line(x,y,.62f,.66f,.92f,.34f,.07f);
        };
    }
    private static boolean rect(float x,float y,float x0,float y0,float x1,float y1){return x>=x0&&x<=x1&&y>=y0&&y<=y1;}
    private static boolean triangle(float x,float y,float ax,float ay,float bx,float by,float cx,float cy){
        float d1=(x-bx)*(ay-by)-(ax-bx)*(y-by),d2=(x-cx)*(by-cy)-(bx-cx)*(y-cy),d3=(x-ax)*(cy-ay)-(cx-ax)*(y-ay);
        return !((d1<0||d2<0||d3<0)&&(d1>0||d2>0||d3>0));
    }
    /** A stroke of half-width {@code r} from (ax,ay) to (bx,by), with round ends. */
    private static boolean line(float x,float y,float ax,float ay,float bx,float by,float r){
        float dx=bx-ax,dy=by-ay,t=Math.clamp(((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy),0,1);
        float px=ax+dx*t-x,py=ay+dy*t-y;
        return px*px+py*py<=r*r;
    }
}
