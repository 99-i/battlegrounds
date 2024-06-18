package trident.grimm.battlegrounds;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import trident.grimm.Elib;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import trident.grimm.battlegrounds.commands.KillCommand;
import trident.grimm.battlegrounds.config.BConfig;
import trident.grimm.battlegrounds.game.CooldownManager;
import trident.grimm.battlegrounds.game.effects.PotionsManager;
import trident.grimm.battlegrounds.game.phases.PhaseManager;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.items.lobby.LobbyListener;
import trident.grimm.battlegrounds.redis.RedisManager;
import trident.grimm.battlegrounds.sidebar.SidebarManager;
import trident.grimm.battlegrounds.util.WorldManager;

public class App extends JavaPlugin {

	private @Getter BConfig bConfig;
	private @Getter ProtocolManager protocolManager;

	private @Getter static App instance;

	@Override
	public void onEnable() {
		instance = this;
		Elib.init();
		RedisManager redisManager = RedisManager.getInstance();
		redisManager.connect();

		if (!redisManager.isConnected()) {
			getLogger().severe("Could not connect to Redis.");
			redisManager.unregister();
			return;
		}
		this.saveDefaultConfig();
		this.bConfig = BConfig.getBConfig();

		this.protocolManager = ProtocolLibrary.getProtocolManager();

		Bukkit.getPluginManager().registerEvents(PhaseManager.getInstance(), this);

		PhaseManager.getInstance().start();
		SidebarManager.getInstance().start();

		Bukkit.getPluginManager().registerEvents(TickCounter.getInstance(), this);
		Bukkit.getPluginManager().registerEvents(new EventListener(), this);
		Bukkit.getPluginManager().registerEvents(new LobbyListener(), this);
		Bukkit.getPluginManager().registerEvents(PotionsManager.getInstance(), this);

		this.getCommand("kill").setExecutor(new KillCommand());

		redisManager.uploadStatus(true);

		SidebarManager.getInstance().update();

		ItemUtil.setRecipes();

	}

	@Override
	public void onDisable() {
		WorldManager.getInstance().deleteMap();
		PhaseManager.getInstance().disable();
		CooldownManager.getInstance().shutdownCooldowns();
		if (RedisManager.getInstance().isConnected()) {
			RedisManager.getInstance().uploadStatus(false);
		}
	}

	public void resetServer() {
		// todo!.
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.kick(Component.text("Server resetting."));
		}
		Bukkit.reload();
	}

}
