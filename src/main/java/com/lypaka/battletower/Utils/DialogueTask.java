package com.lypaka.battletower.Utils;

import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Guis.BattleModeMenus;
import com.lypaka.battletower.Guis.TeamSelectionMenu;
import com.lypaka.battletower.Guis.YesNoMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DialogueTask extends TimerTask {

    private final EntityPlayer player;
    private int index;
    private final List<String> messages;
    private final String which;
    public static Timer timer;
    public static boolean forceStop = false;

    public DialogueTask(EntityPlayer player, int index, List<String> messages, String which) {
        this.player = player;
        this.index = index;
        this.messages = messages;
        this.which = which;
    }

    @Override
    public void run() {
        if (BattleTower.server.getPlayerList().getPlayerByUsername(this.player.getName()) == null) {
            forceStop = true;
        }

        if (!forceStop) {
            if (this.index < this.messages.size()) {
                this.player.sendMessage(new TextComponentString(FancyText.getFancyString(this.messages.get(this.index))));
                ++this.index;
            } else {
                this.cancel();
                timer = new Timer();

                try {
                    switch (this.which.toLowerCase()) {
                        case "introduction":
                            YesNoMenu.openYesNoContainer(this.player);
                            break;
                        case "picked-yes":
                        	BattleModeMenus.openBattleModeSelectionMenu(this.player);
                            break;
                        case "team-selection":
                            TeamSelectionMenu.open(this.player); 
                            break;
                    }
                } catch (ObjectMappingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
