package trident.grimm.battlegrounds.items.lobby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import trident.grimm.battlegrounds.game.GameManager;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;

public class LobbyListener implements Listener {

	ClassLobbyItem classLobbyItem;
	TeamLobbyItem teamLobbyItem;

	public LobbyListener() {
		classLobbyItem = new ClassLobbyItem();
		teamLobbyItem = new TeamLobbyItem();

		for (Player player : Bukkit.getOnlinePlayers()) {
			player.setFoodLevel(20);
			setPlayerLobbyInventory(player);
		}
	}

	private void setPlayerLobbyInventory(Player player) {
		player.getInventory().clear();
		for (int i = 0; i < LobbyItem.lobbyItems.size(); i++) {
			LobbyItem.LobbyItemRegistry registry = LobbyItem.lobbyItems.get(i);
			player.getInventory().addItem(registry.lobbyItem.getDefaultItem(player));
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		GameManager gameManager = GameManager.getInstance();
		Player player = event.getPlayer();
		BPlayer bPlayer = BPlayer.getBPlayer(player);

		if (gameManager.isStarted() && bPlayer.isInTeam())
			return;

		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getItem() == null)
				return;

			ItemStack itemInHand = event.getItem().clone();
			for (int i = 0; i < LobbyItem.lobbyItems.size(); i++) {
				LobbyItem.LobbyItemRegistry registry = LobbyItem.lobbyItems.get(i);
				if (ItemUtil.itemHasFlag(itemInHand, registry.identifier)) {
					registry.lobbyItem.playerOpenedThisInventory(player);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		GameManager gameManager = GameManager.getInstance();
		Player player = event.getPlayer();
		BPlayer bPlayer = BPlayer.getBPlayer(player);

		if (gameManager.isStarted() && bPlayer.isInTeam()) {
			return;
		}
		setPlayerLobbyInventory(event.getPlayer());
	}

}
