package com.lypaka.battletower.Utils;

import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Listeners.PlayerInteractListener;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.function.Consumer;

public class SelfCancellingTask implements Consumer<Task> {

    private final Player player;
    public static boolean endTask = false;

    public SelfCancellingTask(Player player) {
        this.player = player;
    }

    @Override
    public void accept(Task task) {
        if (endTask) {
            task.cancel();
            endTask = false;
            return;
        }

        try {
            if (!player.isOnline()) {
                cancelTask(task);
                return;
            }

            if (PlayerInteractListener.startingIndex < PlayerInteractListener.messages) {
                String line = PlayerInteractListener.dialogue.get(PlayerInteractListener.startingIndex)
                        .replace("%defaultprice%", String.valueOf(getDefaultPrice()));
                player.sendMessage(FancyText.getFancyText(line));
                PlayerInteractListener.startingIndex++;
            } else {
                cancelTask(task);
                Text yes = Text.builder("Yes")
                        .color(TextColors.YELLOW)
                        .onHover(TextActions.showText(Text.of("Click me!")))
                        .onClick(TextActions.runCommand("/tmfactory request yes"))
                        .build();

                Text no = Text.builder("No")
                        .color(TextColors.YELLOW)
                        .onHover(TextActions.showText(Text.of("Click me!")))
                        .onClick(TextActions.runCommand("/tmfactory request no"))
                        .build();

                player.sendMessage(Text.of(TextColors.YELLOW, yes, TextColors.WHITE, " or ", TextColors.YELLOW, no));
            }

        } catch (Exception e) {
            cancelTask(task);
        }
    }

    private void cancelTask(Task task) {
        task.cancel();
        PlayerInteractListener.startingIndex = 0;
    }

    public static int getDefaultPrice() {
        return ConfigManager.getConfigNode(4, "Default-Cost-Of-Move-Requesting").getInt();
    }
}
