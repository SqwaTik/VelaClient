package dev.sqwat.vela.ui.render;

/** Shared geometry for live rendering, input bounds and offline visual regression tests. */
public final class MarkerGeometry {
    public static final int EDITOR_HEIGHT=182;
    private MarkerGeometry(){}
    public record Editor(int x,int y,int width){
        public Rect name(){return new Rect(x+10,y+20,width-20,25);}
        public Rect color(){return new Rect(x+width-92,y+49,82,22);}
        public Rect coordinate(int axis){int cw=(width-32)/3;return new Rect(x+10+axis*(cw+6),y+93,cw,25);}
        public Rect icons(){return new Rect(x+10,y+142,width-20,28);}
        public Rect icon(int index){return icon(index,8);}
        public Rect icon(int index,int count){Rect strip=icons();int cw=strip.width()/count;return new Rect(strip.x()+index*cw,strip.y()+2,cw,24);}
    }
    public record Creation(int x,int y,int width,float expansion){
        public int editorHeight(){return Math.round(EDITOR_HEIGHT*Math.clamp(expansion,0,1));}
        public int height(){return 82+editorHeight();}
        public Rect frame(){return new Rect(x,y,width,height());}
        public Editor editor(){return new Editor(x,y+44,width);}
        public Rect action(){return new Rect(x+10,y+height()-33,70,23);}
    }
}
