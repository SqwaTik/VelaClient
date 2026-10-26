package dev.sqwat.vela.ui;
public final class SessionPresentation {
    private static boolean visitedWorld;
    private SessionPresentation(){}
    public static void visit(){visitedWorld=true;}
    public static boolean consumeDeparture(){boolean result=visitedWorld;visitedWorld=false;return result;}
}
