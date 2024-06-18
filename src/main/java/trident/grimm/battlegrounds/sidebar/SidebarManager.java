package trident.grimm.battlegrounds.sidebar;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.game.CooldownManager;
import trident.grimm.battlegrounds.game.GameManager;
import trident.grimm.battlegrounds.game.StatusManager;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.sidebar.renderer.BukkitSidebarRenderer;

public class SidebarManager {
	private static @Getter SidebarManager instance = new SidebarManager();

	private Sidebar sidebar;

	private SidebarManager() {
		sidebar = new Sidebar(new BukkitSidebarRenderer());
		sidebar.render();
		this.setupSidebar();
	}

	public void start() {
		final int RATE = 2;
		new BukkitRunnable() {
			@Override
			public void run() {
				SidebarManager.this.update();
			}
		}.runTaskTimer(App.getInstance(), 0, RATE);
	}

	private void setupSidebar() {
		sidebar.title(player -> {
			return Component.text("--BATTLEGROUNDS--", TextColor.color(255, 120, 120));
		});

		sidebar.lines(player -> {
			List<Component> components;
			if (GameManager.getInstance().isStarted()) {
				components = getGameLines(player);
			} else {
				components = getPregameLines(player);
			}

			return components;
		});
	}

	private List<Component> getPregameLines(Player player) {
		List<Component> components = new ArrayList<>();
		BTeam[] teams = BTeam.values();

		for (int i = 0; i < teams.length; i++) {
			BTeam team = teams[i];
			components.add(team.getDisplayName()
					.appendSpace()
					.append(Component.text(team.getNumPlayers(), TextColor.color(0, 188, 188))));
		}

		return components;
	}

	private List<Component> getGameLines(Player player) {
		List<Component> components = new ArrayList<>();
		List<BTeam> teamsSortedByNexusHealth = BTeam.getSortedByNexusHealth();
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		components.add(Component.text(" "));
		for (BTeam team : teamsSortedByNexusHealth) {
			TextComponent health = (team.getNexusHealth() <= 0) ? Component.text("DEAD")
					: Component.text(team.getNexusHealth());

			components.add(team.getDisplayName()
					.appendSpace()
					.color(TextColor.color(0, 188, 188))
					.append(health));
		}
		if (bPlayer.isInTeam()) {
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

			// components.add(Component.text(" "));
			// components.add(armorText);
			// components.add(mrText);

			List<Component> statuses = StatusManager.getInstance().getPlayerStatusList(bPlayer);
			List<Component> cooldowns = CooldownManager.getInstance().getPlayerCooldownsList(bPlayer);
			if (statuses.size() + cooldowns.size() > 0) {
				components.add(Component.text("   "));
				components.addAll(statuses);
				components.addAll(cooldowns);
			}

		}

		return components;
	}

	public void update() {
		// todo: just give them one when they join.
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!this.sidebar.getViewers().contains(player)) {
				BPlayer.getBPlayer(player).createPlayerScoreboard();
			}
		}
		sidebar.render();
	}

	public void updatePlayer(Player player) {
		if (player == null)
			return;
		sidebar.renderForPlayer(player);
	}

	public void addPlayer(Player player) {
		this.sidebar.add(player);
	}

}
