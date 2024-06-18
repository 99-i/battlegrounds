package trident.grimm.battlegrounds.game;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.ability.AbilityManager;
import trident.grimm.battlegrounds.game.classes.BClass;
import trident.grimm.battlegrounds.game.classes.BClasses;
import trident.grimm.battlegrounds.game.classes.ClassChangeManager;
import trident.grimm.battlegrounds.game.effects.BEffects;
import trident.grimm.battlegrounds.game.phases.PhaseManager;
import trident.grimm.battlegrounds.game.phases.PhaseUnlock;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.villagers.VillagerManager;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.sidebar.SidebarManager;
import trident.grimm.battlegrounds.util.WorldManager;

public class GameManager implements Listener {

	private static @Getter GameManager instance = new GameManager();

	private App app;

	private @Getter boolean started = false;
	private boolean ended = false;

	public GameManager() {
		app = App.getInstance();
		Bukkit.getPluginManager().registerEvents(CombatManager.getInstance(), app);
		Bukkit.getPluginManager().registerEvents(OresManager.getInstance(), app);
		Bukkit.getPluginManager().registerEvents(VillagerManager.getInstance(), app);
	}

	public void startGame() {
		WorldManager.getInstance().loadMap();

		for (BTeam team : BTeam.values()) {
			team.updateWithWorldConfig();
		}

		for (Player player : Bukkit.getOnlinePlayers()) {
			BPlayer bPlayer = BPlayer.getBPlayer(player);
			if (bPlayer.isInTeam()) {
				player.teleport(bPlayer.getTeam().getRandomSpawnPoint());
				bPlayer.getBClass().gameStartForClassPlayer(bPlayer);
				bPlayer.spawnIn();
			}
		}
		AbilityManager.getInstance().start();

		// register all the class providers.
		for (BClass bClass : BClasses.values) {
			Bukkit.getPluginManager().registerEvents(bClass, app);
		}

		// register all the effect providers.
		for (int i = 0; i < BEffects.values.length; i++) {
			Bukkit.getPluginManager().registerEvents(BEffects.values[i], app);
		}

		BlockDisplayManager.getInstance().start();

		VillagerManager.getInstance().spawnVillagers();
		StatShower.getInstance().start();
		HealthManager.getInstance().start();
		Bukkit.getPluginManager().registerEvents(this, app);

		Bukkit.getPluginManager().registerEvents(ClassChangeManager.getInstance(), app);
		Bukkit.getPluginManager().registerEvents(BlockDisplayManager.getInstance(), app);

		started = true;
	}

	// for backpack
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player))
			return;

		Player player = (Player) event.getWhoClicked();
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		ItemStack currentItem = event.getCurrentItem();
		ItemStack cursor = event.getCursor();
		if (currentItem == null && cursor == null) {
			return;
		}

		if (currentItem != null) {
			if (ItemUtil.itemIsBackpack(currentItem)) {
				event.setCancelled(true);
				player.updateInventory();
				new BukkitRunnable() {
					@Override
					public void run() {
						bPlayer.openBackpackInventory();
					}
				}.runTask(app);
				return;
			}
		}

	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		BPlayer bPlayer = BPlayer.getBPlayer(event.getEntity());

		boolean wasSlashKill = bPlayer.isSlashKill();

		if (bPlayer.getHealth() > 0)
			return;

		if (bPlayer.isAlreadyDied()) {
			event.deathMessage(Component.text(""));
			return;
		}
		bPlayer.setAlreadyDied(true);

		if (wasSlashKill) {
			bPlayer.setSlashKill(false);
		}

		if (bPlayer.isInTeam()) {
			event.getEntity().setBedSpawnLocation(bPlayer.getTeam().getRandomSpawnPoint(), true);
		}

		BPlayer killer = bPlayer.getLastDamager();
		if (killer != null) {
			killer.killedPlayer(bPlayer);
		}

		if (killer != null) {
			Player killerPlayer = killer.getPlayer();
			if (killerPlayer != null) {
				event.deathMessage(Component.text(""));
			} else {
				event.deathMessage(Component
						.text(killer.getPlayer().displayName() + " killed " + bPlayer.getPlayer().displayName()));
			}
		} else {
			event.setDroppedExp(0);
			event.deathMessage(Component.text(""));
		}
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		BPlayer bPlayer = BPlayer.getBPlayer(event.getPlayer());
		bPlayer.setHealth(bPlayer.getMaxHealth());
		bPlayer.clearEffects();
		if (bPlayer.isInTeam()) {
			if (!bPlayer.getTeam().isDead()) {
				bPlayer.spawnIn();
			}
		}
		bPlayer.setAlreadyDied(false);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Player player = event.getPlayer();
		BPlayer bPlayer = BPlayer.getBPlayer(player);

		for (BTeam team : BTeam.values()) {
			if (team.getNexusLocation().equals(block.getLocation())) {
				if (bPlayer.getTeam() != team) {
					if (PhaseUnlock.MINE_NEXUS.isUnlocked()) {
						playerMinedNexus(bPlayer, team);
					} else {
						player.sendMessage(Component.text().color(TextColor.color(36, 157, 159))
								.append(Component.text("Cannot mine a nexus in Phase 1!")));
					}
				}
				event.setCancelled(true);
				break;
			}
		}
	}

	private void playerMinedNexus(BPlayer player, BTeam team) {

		int damage = PhaseManager.getInstance().getCurrentPhase() == 5 ? 2 : 1;
		team.setNexusHealth(team.getNexusHealth() - damage);

		SidebarManager.getInstance().update();

		checkForGameEnd();
	}

	private void checkForGameEnd() {
		int numDeadTeams = 0;

		for (BTeam team : BTeam.values()) {
			if (team.isDead())
				numDeadTeams++;
		}
		if (numDeadTeams == 3) {
			if (!ended) {
				endGame();
			}
		}
	}

	public void endGame() {
		if (ended)
			return;
		BTeam aliveTeam = null;
		ended = true;
		for (BTeam team : BTeam.values()) {
			if (!team.isDead()) {
				aliveTeam = team;
				break;
			}
		}

		if (aliveTeam == null) {
			return;
		}

		new BukkitRunnable() {
			int timer = 5;

			@Override
			public void run() {
				if (timer == 0) {
					app.resetServer();
				}
				timer--;
			}

		}.runTaskTimer(app, 0, 20);
	}
}
