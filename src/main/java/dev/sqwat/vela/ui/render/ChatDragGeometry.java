package dev.sqwat.vela.ui.render;
public final class ChatDragGeometry {
    public static final int SIZE=22;
    private ChatDragGeometry(){}
    public static Rect place(UiViewport viewport,int inputX,int inputY,int inputWidth){
        int right=(int)Math.floor(viewport.local(inputX+inputWidth));
        int top=(int)Math.floor(viewport.local(inputY));
        return new Rect(Math.clamp(right-SIZE-3,2,Math.max(2,viewport.width()-SIZE-2)),Math.clamp(top-SIZE-5,2,Math.max(2,viewport.height()-SIZE-2)),SIZE,SIZE);
    }
}
