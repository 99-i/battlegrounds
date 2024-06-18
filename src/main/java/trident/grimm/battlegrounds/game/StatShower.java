package trident.grimm.battlegrounds.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.BChat;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.util.ActionBar;
import trident.grimm.battlegrounds.util.FastUtil;
import trident.grimm.battlegrounds.util.WorldManager;

public class StatShower {

	private static @Getter StatShower instance = new StatShower();

	private App app;

	public StatShower() {
		app = App.getInstance();
	}

	public void start() {
		new BukkitRunnable() {

			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					updatePlayerStatline(player);
				}
			}
		}.runTaskTimer(app, 0, 40);
	}

	public void updatePlayerStatline(Player player) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		if (!bPlayer.isInTeam())
			return;

		if (player.getWorld() != WorldManager.getInstance().getMapWorld())
			return;

		double health = bPlayer.getHealth();
		if (health <= 0)
			health = 0;

		// double armor = bPlayer.getArmor();
		// TextComponent armorText = Component.text()
		// .append(Component.text("ARMOR: ", TextColor.color(120, 116, 3)))
		// .resetStyle()
		// .append(Component.text(armor))
		// .append(Component.text(" (" + ((int) (bPlayer.getDefenseReduction(armor) *
		// 100)) + "%)",
		// TextColor.color(95, 40, 40)))
		// .build();
		// double mr = bPlayer.getMagicResistance();
		// TextComponent mrText = Component.text()
		// .append(Component.text("MR: ", TextColor.color(120, 116, 3)))
		// .resetStyle()
		// .append(Component.text(mr))
		// .append(Component.text(" (" + ((int) (bPlayer.getDefenseReduction(mr) * 100))
		// + "%)",
		// TextColor.color(95, 40, 40)))
		// .build();

		double armor = bPlayer.getArmor();
		double mr = bPlayer.getMagicResistance();

		TextComponent component = (TextComponent) BChat.getMiniMessage().deserialize(
				"<#ff6482>Health: " + FastUtil.doubleToString(health) + "/" + bPlayer.getMaxHealth() + "</#ff6482>"
						+ "    <#787403>ARMOR: </#787403>" + armor + "<#5f2828> ("
						+ ((int) (bPlayer.getDefenseReduction(armor) * 100))
						+ "%)</#5f2828>"
						+ "    <#787403>MR: </#787403>" + mr + "<#5f5050> ("
						+ ((int) (bPlayer.getDefenseReduction(mr) * 100))
						+ "%)</#5f5050>");

		// TextComponent component = Component.text().append(Component
		// .text("Health: " + FastUtil.doubleToString(health) + "/" +
		// bPlayer.getMaxHealth())
		// .color(TextColor.color(255, 100, 130))).append(
		//
		// Component.text(" ARMOR: " + FastUtil.doubleToString(bPlayer.getArmor()))
		// .color(TextColor.color(100, 100, 130)))
		// .append(Component.text(" MR: " +
		// FastUtil.doubleToString(bPlayer.getMagicResistance()))
		// .color(TextColor.color(200, 100, 130)))
		// .build();

		ActionBar.sendPlayerActionBar(player, component);
	}
}
