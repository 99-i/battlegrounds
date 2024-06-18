package trident.grimm.battlegrounds.game.ability;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.ability.Ability.AbilityType;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;

//event listener that listens for ability usage events.
public class AbilityManager implements Listener {

	public static @Getter AbilityManager instance = new AbilityManager();

	public AbilityManager() {
	}

	public void start() {
		Bukkit.getPluginManager().registerEvents(this, App.getInstance());
	}

	// f key event
	@EventHandler
	public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);
		if (event.getPlayer().getWorld() == Bukkit.getWorld("world")) {
			return;
		}

		BPlayer bPlayer = BPlayer.getBPlayer(event.getPlayer());

		for (Ability ability : bPlayer.getBClass().getAbilities()) {
			if (ability.getAbilityType() == AbilityType.F_KEY) {
				bPlayer.getBClass().abilityUsed(BPlayer.getBPlayer(event.getPlayer()), ability);
			}
		}
	}

	// double jump event
	@EventHandler
	public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
		event.setCancelled(true);
		if (event.getPlayer().getWorld() == Bukkit.getWorld("world")) {
			return;
		}

		BPlayer bPlayer = BPlayer.getBPlayer(event.getPlayer());
		for (Ability ability : bPlayer.getBClass().getAbilities()) {
			if (ability.getAbilityType() == AbilityType.DOUBLE_JUMP) {
				bPlayer.getBClass().abilityUsed(BPlayer.getBPlayer(event.getPlayer()), ability);
			}
		}

	}

	// right click item
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {

		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		ItemStack itemStack = event.getItem();

		if (itemStack == null)
			return;
		String tag = ItemUtil.getItemString(itemStack, "ability-id");
		if (tag == null)
			return;

		event.setCancelled(true);

		BPlayer bPlayer = BPlayer.getBPlayer(event.getPlayer());
		for (Ability ability : bPlayer.getBClass().getAbilities()) {
			if (ability.getAbilityType() == AbilityType.ITEM) {
				if (ability.getAbilityId().equals(tag)) {
					bPlayer.getBClass().abilityUsed(BPlayer.getBPlayer(event.getPlayer()), ability);
				}
			}
		}
	}

}
