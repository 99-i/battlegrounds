package trident.grimm.battlegrounds.game.classes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import trident.grimm.inventory.ElibGUI;
import trident.grimm.inventory.ElibView;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.util.WorldManager;

public class ClassChangeManager implements Listener {

	public final static String CLASS_PICKER_CLASS_ID_TAG = "class-class-id";
	private final static int CLASS_CHANGE_COOLDOWN_TICKS = 5 * 20;

	private HashMap<BPlayer, Integer> playerChangeClassCooldowns;

	private static @Getter ClassChangeManager instance = new ClassChangeManager();
	private ElibGUI classChangeGUI;

	private ClassChangeManager() {
		playerChangeClassCooldowns = new HashMap<>();
		new BukkitRunnable() {

			@Override
			public void run() {
				for (Iterator<Map.Entry<BPlayer, Integer>> it = playerChangeClassCooldowns.entrySet().iterator(); it
						.hasNext();) {
					Map.Entry<BPlayer, Integer> entry = it.next();
					if (entry.getValue() == 1) {
						it.remove();
					} else {
						entry.setValue(entry.getValue() - 1);
					}
				}
			}

		}.runTaskTimer(App.getInstance(), 0, 1);
		this.classChangeGUI = new ElibGUI();

		ElibView main = this.classChangeGUI.addView("main", 36, Component.text("Pick a class!"));

		for (int i = 0; i < BClasses.values.length; i++) {
			BClass bClass = BClasses.values[i];
			ItemStack displayItemStack = new ItemStack(bClass.getDisplayMaterial());
			ItemUtil.setDisplayName(displayItemStack, bClass.getDisplayName());
			main.addButton(i, displayItemStack, (event) -> {
				playerSwitchClassTo(BPlayer.getBPlayer(event.presser()), bClass);
			});
		}

	}

	@EventHandler
	public void onPlayerOpenSign(PlayerOpenSignEvent event) {
		Sign sign = event.getSign();
		if (WorldManager.getGameLocations(App.getInstance().getBConfig().getClassChangeSigns())
				.contains(sign.getBlock().getLocation())) {
			event.setCancelled(true);
			BPlayer bPlayer = BPlayer.getBPlayer(event.getPlayer());
			if (playerChangeClassCooldowns.containsKey(bPlayer)) {
				event.getPlayer().sendMessage(Component.text("Please wait "
						+ prettyPrintTickCD(playerChangeClassCooldowns.get(bPlayer)) + " before changing class!",
						TextColor.color(255, 0, 0)));
				event.getPlayer().closeInventory();
				return;
			}
			openClassChangeMenu(event.getPlayer());
		}
	}

	private void openClassChangeMenu(Player player) {
		this.classChangeGUI.setPlayerView(player, "main");
	}

	private String prettyPrintTickCD(int ticks) {
		int cd = ticks / 20;
		int min = (cd - (cd % 60)) / 60;
		int sec = cd % 60;
		if (min != 0) {
			return new StringBuilder(Integer.toString(min)).append("m").append(sec).append("s").toString();
		} else {
			return new StringBuilder(Integer.toString(sec)).append("s").toString();
		}
	}

	private void playerSwitchClassTo(BPlayer bPlayer, BClass bClass) {
		Player player = bPlayer.getPlayer();
		if (player == null)
			return;

		for (ItemStack stack : player.getInventory().getContents()) {
			if (stack == null)
				continue;

			if (ItemUtil.itemIsUndroppable(stack) && !ItemUtil.itemIsBackpack(stack)) {
				stack.setAmount(0);
			}
		}
		player.updateInventory();

		ArrayList<ItemStack> contents = bClass.getChangeInventory(bPlayer);

		Inventory inventory = Bukkit.createInventory(null, 36, bClass.getDisplayName());

		for (ItemStack is : contents) {
			inventory.addItem(is);
		}

		player.openInventory(inventory);

		bPlayer.setClass(bClass, false);

		playerChangeClassCooldowns.put(bPlayer, CLASS_CHANGE_COOLDOWN_TICKS);
	}

}
