package trident.grimm.battlegrounds;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import trident.grimm.battlegrounds.game.GameManager;
import trident.grimm.battlegrounds.game.StatusManager;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.sidebar.SidebarManager;
import trident.grimm.battlegrounds.util.WorldManager;

public class EventListener implements Listener {

	private App app;

	public EventListener() {
		app = App.getInstance();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (!BPlayer.getBPlayer(event.getPlayer()).isInTeam() || !GameManager.getInstance().isStarted()) {
			event.getPlayer().teleport(WorldManager.getLobbyLocation(this.app.getBConfig().getLobbySpawnPoint()));
			event.getPlayer().setFoodLevel(20);
		}
		// event.getPlayer().setCollidable(false);
		SidebarManager.getInstance().updatePlayer(event.getPlayer());

	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		Player player = (Player) event.getEntity();
		if (!BPlayer.getBPlayer(player).isInTeam() || !GameManager.getInstance().isStarted()) {
			event.setCancelled(true);
			player.setFoodLevel(20);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		BPlayer.getBPlayer(event.getPlayer()).invalidatePlayer();
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getWorld() == Bukkit.getWorld("world")) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock().getWorld() == Bukkit.getWorld("world")) {
			event.setCancelled(true);
		}
		if (ItemUtil.itemIsUndroppable(event.getItemInHand())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
		if (event.getPlayer().getWorld() == Bukkit.getWorld("world")) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (ItemUtil.itemIsSoulbound(event.getItemDrop().getItemStack())) {
			event.getItemDrop().remove();
		}
		if (ItemUtil.itemIsUndroppable(event.getItemDrop().getItemStack())) {
			if (ItemUtil.itemIsBackpack(event.getItemDrop().getItemStack())) {
				event.getItemDrop().remove();
				BPlayer.getBPlayer(event.getPlayer()).setPlayerBackpackChest();
			} else {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() != SpawnReason.CUSTOM) {
			event.getEntity().remove();
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		int i = 0;
		while (i < event.getDrops().size()) {
			if (ItemUtil.itemIsSoulbound(event.getDrops().get(i))) {
				event.getDrops().remove(i);
			} else {
				i++;
			}
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			if (event.getEntity().getWorld() == Bukkit.getWorld("world")) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPlayerSwapHandItemsSwap(PlayerSwapHandItemsEvent event) {
		if (event.getPlayer().getWorld() == Bukkit.getWorld("world")) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPrepareItemCraft(PrepareItemCraftEvent event) {
		ItemStack result = event.getInventory().getResult();
		if (result != null) {
			ItemUtil.disableAttributes(result);
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		// if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
		// event.setCancelled(true);
		// return;
		// }

		// if (event.getSlotType() == SlotType.QUICKBAR) {
		// return;
		// }
		// if (event.getClickedInventory() != null) {
		// if (event.getClickedInventory().getSize() == 41 && event.getSlotType() ==
		// SlotType.CONTAINER) {
		// if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
		// event.setCancelled(true);
		// }
		// return;
		// }
		// }

		// if (event.getAction() == InventoryAction.PLACE_ALL || event.getAction() ==
		// InventoryAction.PLACE_ONE
		// || event.getAction() == InventoryAction.PLACE_SOME) {
		// ItemStack cursor = event.getCursor();
		// if (cursor != null) {
		// if (ItemUtil.itemIsSoulbound(cursor)) {
		// event.setCancelled(true);
		// }
		// }
		// }

	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		int inventorySize = event.getInventory().getSize();

		List<Integer> slots = event.getRawSlots().stream().filter((i) -> i < inventorySize)
				.collect(Collectors.toList());

		List<Integer> slotsWithSoulboundItems = slots.stream()
				.filter((i) -> ItemUtil.itemIsSoulbound(event.getNewItems().get(i))).collect(Collectors.toList());

		if (!slotsWithSoulboundItems.isEmpty()) {
			event.setCancelled(true);
		}
	}
}
