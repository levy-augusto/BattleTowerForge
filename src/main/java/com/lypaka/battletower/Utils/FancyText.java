package com.lypaka.battletower.Utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;


public class FancyText {


    public static TextComponentString getFancyComponent(String value) {
        return new TextComponentString(value.replace("&", "§"));
    }


    public static Text getFancyText(String value) {               
        return TextSerializers.FORMATTING_CODE.deserialize(value);
    }

  public static String getFancyString(String value) {
        return value.replace("&", "§");
    }


    public static void sendMessage(Object player, String message) {
        if (player instanceof Player) {                           
            ((Player) player).sendMessage(getFancyText(message)); 
        } else if (player instanceof EntityPlayer) {              
            ((EntityPlayer) player).sendMessage(getFancyComponent(message));
        }
    }

    
}
