package dev.sqwat.vela.ui.render;
import dev.sqwat.vela.ui.TextEdit;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class TextFieldDraw {
 private TextFieldDraw(){}
 public static void draw(GuiGraphicsExtractor g,TextEdit edit,Rect r,boolean focused,String placeholder){
  int begin=focused?Math.max(0,edit.caret()-Math.max(32,r.width())):0;
  if(begin>0&&Character.isLowSurrogate(edit.text().charAt(begin)))begin--;
  int finish=Math.min(edit.text().length(),(focused?Math.max(edit.caret(),begin):0)+Math.max(32,r.width()));
  if(finish<edit.text().length()&&Character.isLowSurrogate(edit.text().charAt(finish)))finish--;
  float before=focused?VelaDraw.textWidth(edit.text().substring(begin,Math.max(begin,edit.caret())),VelaDraw.Weight.REGULAR,8):0;
  float offset=focused?Math.max(0,before-r.width()+5):0;
  int x=r.x()-Math.round(offset),y=r.y()+2;
  dev.sqwat.vela.ui.render.UiClip.enable(g,r.x(),r.y(),r.x()+r.width(),r.y()+r.height());
  if(focused&&edit.start()!=edit.end()){
   int a=Math.round(VelaDraw.textWidth(edit.text().substring(begin,Math.max(begin,Math.min(finish,edit.start()))),VelaDraw.Weight.REGULAR,8));
   int b=Math.round(VelaDraw.textWidth(edit.text().substring(begin,Math.max(begin,Math.min(finish,edit.end()))),VelaDraw.Weight.REGULAR,8));
   g.fill(x+a,y-1,x+b,y+11,0x665F8FD9);
  }
  VelaDraw.text(g,edit.text().isEmpty()&&!focused?placeholder:edit.text().substring(begin,finish),x,y,edit.text().isEmpty()?VelaTheme.TEXT_MUTED:VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
  if(focused&&edit.caretVisible())VelaDraw.blit(g,net.minecraft.resources.Identifier.fromNamespaceAndPath("velaclient","textures/gui/chrome.png"),x+Math.round(before),y,1/VelaDraw.pixelDensity(g),9,6,6,1,1,1024,1024,VelaTheme.TEXT_SECONDARY);
  g.disableScissor();
 }
 public static void click(TextEdit edit,Rect r,double mx,boolean extend){int begin=Math.max(0,edit.caret()-Math.max(32,r.width()));if(begin>0&&Character.isLowSurrogate(edit.text().charAt(begin)))begin--;float before=VelaDraw.textWidth(edit.text().substring(begin,edit.caret()),VelaDraw.Weight.REGULAR,8);float target=(float)mx-r.x()+Math.max(0,before-r.width()+5);int best=begin;float distance=Float.MAX_VALUE;for(int i=begin;i<=edit.text().length();){float d=Math.abs(VelaDraw.textWidth(edit.text().substring(begin,i),VelaDraw.Weight.REGULAR,8)-target);if(d>distance)break;distance=d;best=i;if(i==edit.text().length())break;i=edit.text().offsetByCodePoints(i,1);}edit.move(best,extend);}
}
