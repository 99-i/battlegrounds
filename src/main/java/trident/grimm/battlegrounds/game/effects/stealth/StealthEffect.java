package trident.grimm.battlegrounds.game.effects.stealth;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.stealth.StealthEffect.StealthEffectSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;

public class StealthEffect extends BEffect<StealthEffectSettings> {
	public static record StealthEffectSettings() {
	}

	public StealthEffect(TextComponent displayName) {
		super(displayName);
	}

	public void playerStart(Player player, StealthEffectSettings settings) {
		for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
			otherPlayer.hidePlayer(App.getInstance(), player);
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		this.players.forEach((assassinPlayer) -> {
			BPlayer assassin = BPlayer.getBPlayer(assassinPlayer);
			if (assassinPlayer == null || assassin == BPlayer.getBPlayer(event.getPlayer()))
				return;

			event.getPlayer().hidePlayer(App.getInstance(), assassinPlayer);
		});
	}

	public void onStop(Player player, StealthEffectSettings settings) {
		playerEnd(player, settings);
	}

	public void playerEnd(Player player, StealthEffectSettings settings) {
		for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
			otherPlayer.showPlayer(App.getInstance(), player);
		}
	}

}
