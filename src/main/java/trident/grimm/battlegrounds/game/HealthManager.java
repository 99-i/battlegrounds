package trident.grimm.battlegrounds.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.util.WorldManager;

public class HealthManager implements Listener {

	private static @Getter HealthManager instance = new HealthManager();

	private App app;

	public HealthManager() {
		app = App.getInstance();
	}

	public void start() {
		final int RATE = 20;
		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					BPlayer bPlayer = BPlayer.getBPlayer(player);
					if (bPlayer.getHealth() < bPlayer.getMaxHealth()) {
						bPlayer.setHealth(bPlayer.getHealth() + (bPlayer.getHealthRegen() / (20 / (double) RATE)));
					}
					updatePlayerHealth(player);
				}
			}
		}.runTaskTimer(app, 0, RATE);

		Bukkit.getPluginManager().registerEvents(this, app);
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		BPlayer bPlayer = BPlayer.getBPlayer(event.getPlayer());

		bPlayer.setHealth(bPlayer.getMaxHealth());

	}

	public void updatePlayerHealth(Player player) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		if (!bPlayer.isInTeam())
			return;

		if (player.getWorld() != WorldManager.getInstance().getMapWorld())
			return;

		if (bPlayer.getHealth() <= 0) {
			player.setHealth(0);
		} else {
			double healthToSet = (bPlayer.getHealth() / bPlayer.getMaxHealth()) * 20;
			if (healthToSet < 1) {
				healthToSet = 2;
			}
			player.setHealth(healthToSet > 20 ? 20 : healthToSet);
		}
		StatShower.getInstance().updatePlayerStatline(player);
	}
}
