package dev.sqwat.vela.core;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.marker.MarkerRuntime;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** Recognized dot commands never leave the client. */
public final class LocalCommands {
    private LocalCommands(){}
    public static boolean send(String message,VelaServices services){
        String[] args=message.strip().split("\\s+");
        if(!args[0].equalsIgnoreCase(".gps")&&!args[0].equalsIgnoreCase(".cfg")&&!args[0].equals("."))return true;
        var client=Minecraft.getInstance();
        String result;
        try{
            if(args[0].equals(".")){
                result=".cfg load/save/remove <имя> · .cfg dir · .gps <x> [y] <z>";
            }else if(args[0].equalsIgnoreCase(".gps")){
                if(args.length!=3&&args.length!=4)throw new IllegalArgumentException(".gps <x> <z> или .gps <x> <y> <z>");
                if(client.player==null||client.level==null)throw new IllegalArgumentException("Сначала войдите в мир");
                double x=coordinate(args[1]),y=args.length==4?coordinate(args[2]):client.player.getY(),z=coordinate(args[args.length-1]);
                var markers=services.markers();String world=MarkerRuntime.world(client),dimension=MarkerRuntime.dimension(client);
                markers.add(markers.nextName(world,dimension),world,dimension,x,y,z,VelaTheme.PRIMARY,"markers",false);
                result="Метка создана";
            }else{
                String usage=".cfg load/save/remove <имя> · .cfg dir";
                if(args.length==2&&args[1].equalsIgnoreCase("dir")){
                    java.nio.file.Files.createDirectories(services.configs().profilesDirectory());
                    Util.getPlatform().openUri(services.configs().profilesDirectory().toUri());result="Папка конфигураций открыта";
                }else{
                    if(args.length!=3||!args[2].matches("[A-Za-z0-9_-]{1,32}"))throw new IllegalArgumentException(usage+". Имя: латиница, цифры, _ и -");
                    String name=args[2].toLowerCase(java.util.Locale.ROOT);
                    switch(args[1].toLowerCase(java.util.Locale.ROOT)){
                        case "load" -> {requireProfile(services,name);services.configs().selectProfile(name);result="Загружен: "+name;}
                        case "save" -> {services.configs().saveToProfile(name);result="Сохранён: "+name;}
                        case "remove" -> {requireProfile(services,name);services.configs().archiveProfile(name);result="Перемещён в архив: "+name;}
                        default -> throw new IllegalArgumentException(usage);
                    }
                }
            }
        }catch(java.io.IOException|IllegalArgumentException error){result=error.getMessage();}
        client.gui.hud.getChat().addClientSystemMessage(Component.literal("Vela Visual").withStyle(s->s.withColor(VelaTheme.PRIMARY&0xFFFFFF))
            .append(Component.literal(" -> ").withStyle(s->s.withColor(0x929AAA)))
            .append(Component.literal(result).withStyle(s->s.withColor(0xDCE2EE))));
        return false;
    }
    private static void requireProfile(VelaServices services,String name){if(!services.configs().profiles().contains(name))throw new IllegalArgumentException("Конфигурация не найдена: "+name);}
    private static double coordinate(String value){
        try{double number=Double.parseDouble(value);if(!Double.isFinite(number)||Math.abs(number)>30_000_000)throw new NumberFormatException();return number;}
        catch(NumberFormatException error){throw new IllegalArgumentException("Координаты должны быть числами в пределах мира: .gps <x> [y] <z>");}
    }
}
