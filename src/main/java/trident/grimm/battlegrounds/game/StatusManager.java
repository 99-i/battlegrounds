package trident.grimm.battlegrounds.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.sidebar.SidebarManager;

// statuses are like cooldowns but have no expiry time.
public class StatusManager {
	private static @Getter StatusManager instance = new StatusManager();

	private HashMap<BPlayer, Set<TextComponent>> statuses = new HashMap<>();

	public List<Component> getPlayerStatusList(BPlayer bPlayer) {
		Set<TextComponent> set = getPlayerStatuses(bPlayer);
		return set.stream().collect(Collectors.toList());
	}

	// add the status.
	// if the status already exists, do nothing.
	public void addPlayerStatus(BPlayer bPlayer, TextComponent status) {
		Set<TextComponent> components = this.getPlayerStatuses(bPlayer);

		if (components.contains(status)) {
			return;
		}

		components.add(status);

		SidebarManager.getInstance().updatePlayer(bPlayer.getPlayer());
	}

	// remove the status
	// if the status does not exist, do nothing.
	public void removePlayerStatus(BPlayer bPlayer, TextComponent status) {
		Set<TextComponent> components = this.getPlayerStatuses(bPlayer);

		components.remove(status);
		SidebarManager.getInstance().updatePlayer(bPlayer.getPlayer());
	}

	public Set<TextComponent> getPlayerStatuses(BPlayer bPlayer) {
		if (!this.statuses.containsKey(bPlayer)) {
			this.statuses.put(bPlayer, new HashSet<>());
		}

		return this.statuses.get(bPlayer);
	}

}
