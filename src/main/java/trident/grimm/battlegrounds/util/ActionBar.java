package trident.grimm.battlegrounds.util;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.TextComponent;

public class ActionBar {
	public static void sendPlayerActionBar(Player player, TextComponent text) {
		player.sendActionBar(text);
	}
}
