package dev.sqwat.vela.ui.render;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class SearchText {
    private SearchText(){}
    public static void draw(GuiGraphicsExtractor g,String text,String query,int x,int y,int color,VelaDraw.Weight weight,float size){
        VelaDraw.text(g,text,x,y,color,weight,size);
        highlight(g,text,query,x,y,weight,size);
    }
    public static void highlight(GuiGraphicsExtractor g,String text,String query,int x,int y,VelaDraw.Weight weight,float size){
        if(query==null||query.isBlank())return;
        for(int i=0;i+query.length()<=text.length();i++){
            if(!text.regionMatches(true,i,query,0,query.length()))continue;
            float left=VelaDraw.textWidth(text.substring(0,i),weight,size);
            String match=text.substring(i,i+query.length());
            VelaDraw.text(g,match,x+Math.round(left),y,VelaTheme.PRIMARY,weight,size);
            i+=query.length()-1;
        }
    }
}
