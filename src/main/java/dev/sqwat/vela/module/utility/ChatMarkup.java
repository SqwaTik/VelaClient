package dev.sqwat.vela.module.utility;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure chat text analysis for the Chat module: coordinates, mentions of the player's name and private-message
 * formats. Rewriting keeps every original style; only matched ranges gain a click action or highlight.
 */
public final class ChatMarkup {
    private ChatMarkup(){}
    public record Span(int start,int end,String command){}
    public record Private(String partner,boolean incoming){}

    private static final String NUM="(-?\\d{1,8})";
    private static final Pattern LABELLED=Pattern.compile("(?i)(?<![\\p{L}\\d])x\\s*[:=]?\\s*"+NUM+"[,;\\s]+y\\s*[:=]?\\s*"+NUM+"[,;\\s]+z\\s*[:=]?\\s*"+NUM);
    private static final Pattern BARE=Pattern.compile("(?<![\\w.:-])"+NUM+"(?:\\s*[,;]\\s*|\\s+)"+NUM+"(?:\\s*[,;]\\s*|\\s+)"+NUM+"(?![\\w.:]|\\s*[:%])");
    private static final int WORLD_LIMIT=30_000_000;

    /** Coordinate triples with plausible values; each becomes a "/vela mark x y z" click. */
    public static List<Span> coordinates(String text){
        var spans=new ArrayList<Span>();
        collect(LABELLED.matcher(text),spans,false);
        collect(BARE.matcher(text),spans,true);
        spans.sort((a,b)->Integer.compare(a.start,b.start));
        // Labelled and bare forms can overlap; keep the first.
        var result=new ArrayList<Span>();int end=-1;
        for(var s:spans)if(s.start>=end){result.add(s);end=s.end;}
        return result;
    }
    /** Bare triples must not all be tiny, or "3 4 5 items" would become a link. */
    private static void collect(Matcher m,List<Span> into,boolean bare){
        while(m.find()){
            long x,y,z;
            try{x=Long.parseLong(m.group(1));y=Long.parseLong(m.group(2));z=Long.parseLong(m.group(3));}catch(NumberFormatException e){continue;}
            if(Math.abs(x)>WORLD_LIMIT||Math.abs(z)>WORLD_LIMIT||y<-64||y>320)continue;
            if(bare&&Math.max(Math.abs(x),Math.abs(z))<10)continue;
            into.add(new Span(m.start(),m.end(),"/vela mark "+x+" "+y+" "+z));
        }
    }

    private static final Pattern SEPARATOR=Pattern.compile("(:\\s|»|>\\s|→)");
    /**
     * Ranges where {@code name} is mentioned after the sender part ("Name: text", "[Rank] Name » text"), so the
     * player's own messages and join lines do not count. Empty when the text has no sender separator.
     */
    public static List<Span> mentions(String text,String name){
        if(name==null||name.length()<3)return List.of();
        Matcher sep=SEPARATOR.matcher(text);
        if(!sep.find())return List.of();
        int from=sep.end();
        String lower=text.toLowerCase(Locale.ROOT),needle=name.toLowerCase(Locale.ROOT);
        var result=new ArrayList<Span>();
        for(int i=lower.indexOf(needle,from);i>=0;i=lower.indexOf(needle,i+needle.length())){
            boolean left=i==0||!isNameChar(text.charAt(i-1)),right=i+needle.length()>=text.length()||!isNameChar(text.charAt(i+needle.length()));
            if(left&&right)result.add(new Span(i,i+needle.length(),null));
        }
        return result;
    }
    private static boolean isNameChar(char c){return Character.isLetterOrDigit(c)||c=='_';}

    private static final String RANK="(?:\\[[^\\]]{1,24}\\]\\s*)?";
    private static final String NAME="(\\w{3,16})";
    private static final String ARROW="\\s*(?:->|→|»|>>)\\s*";
    private static final String ME="(?:me|мне|я|вам|вы|you)";
    private static final List<Pattern> INCOMING=List.of(
        Pattern.compile("(?iu)^\\[?"+RANK+NAME+ARROW+ME+"\\]?\\s*:?\\s*\\S.*$"),
        Pattern.compile("(?iu)^\\[(?:лс|pm|л\\.с\\.)\\]\\s*"+RANK+NAME+ARROW+ME+"\\s*:?\\s*\\S.*$"),
        Pattern.compile("(?iu)^"+NAME+" whispers to you: .+$"),
        Pattern.compile("(?iu)^(?:сообщение от|лс от|pm from)\\s+"+RANK+NAME+"\\s*[:»>]\\s*\\S.*$"));
    private static final List<Pattern> OUTGOING=List.of(
        Pattern.compile("(?iu)^\\[?"+ME+ARROW+RANK+NAME+"\\]?\\s*:?\\s*\\S.*$"),
        Pattern.compile("(?iu)^\\[(?:лс|pm|л\\.с\\.)\\]\\s*"+ME+ARROW+RANK+NAME+"\\s*:?\\s*\\S.*$"),
        Pattern.compile("(?iu)^you whisper to "+NAME+": .+$"),
        Pattern.compile("(?iu)^(?:сообщение для|лс для|pm to)\\s+"+RANK+NAME+"\\s*[:»>]\\s*\\S.*$"));

    /** The other side of a private message, or null for ordinary chat. */
    public static Private privateMessage(String text){
        String line=text.strip();
        for(var p:OUTGOING){var m=p.matcher(line);if(m.matches())return new Private(m.group(1),false);}
        for(var p:INCOMING){var m=p.matcher(line);if(m.matches()&&!isMe(m.group(1)))return new Private(m.group(1),true);}
        return null;
    }
    private static boolean isMe(String word){return word.matches("(?iu)me|мне|я|вам|вы|you");}

    /** Flattens {@code message} into styled runs and applies the spans; returns the original when nothing matched. */
    public static Component apply(Component message,List<Span> links,List<Span> mentions,int mentionColor){
        if(links.isEmpty()&&mentions.isEmpty())return message;
        record Run(Style style,String text){}
        var runs=new ArrayList<Run>();
        message.visit((style,text)->{if(!text.isEmpty())runs.add(new Run(style,text));return java.util.Optional.empty();},Style.EMPTY);
        MutableComponent out=Component.empty();int offset=0;
        for(var run:runs){
            int start=offset,end=offset+run.text.length();int cursor=start;
            while(cursor<end){
                Span link=at(links,cursor),mention=at(mentions,cursor);
                Span active=link!=null?link:mention;
                int stop=end;
                if(active==null){int next=Math.min(next(links,cursor),next(mentions,cursor));stop=Math.min(end,next);}
                else stop=Math.min(end,active.end);
                Style style=run.style;
                if(link!=null)style=style.withUnderlined(true).withClickEvent(new ClickEvent.RunCommand(link.command))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Создать метку")));
                if(mention!=null)style=style.withColor(mentionColor).withBold(true);
                out.append(Component.literal(run.text.substring(cursor-start,stop-start)).withStyle(style));
                cursor=stop;
            }
            offset=end;
        }
        return out;
    }
    private static Span at(List<Span> spans,int i){for(var s:spans)if(s.start<=i&&i<s.end)return s;return null;}
    private static int next(List<Span> spans,int i){int best=Integer.MAX_VALUE;for(var s:spans)if(s.start>i)best=Math.min(best,s.start);return best;}
}
