package trident.grimm.battlegrounds.game;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.BConfig;
import trident.grimm.battlegrounds.config.BConfig.OreDataEntry;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.util.WorldManager;

public class OresManager implements Listener {

	public static @Getter OresManager instance = new OresManager();

	private App app;
	private BConfig bConfig;
	private ArrayList<Location> blocksOnCooldown;

	public OresManager() {
		this.app = App.getInstance();
		this.bConfig = app.getBConfig();
		this.blocksOnCooldown = new ArrayList<>();
	}

	public static boolean isUsableTool(ItemStack tool, Block block) {
		return true;
		// TODO!
		// net.minecraft.server.v1_16_R3.Block nmsBlock =
		// org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers
		// .getBlock(block.getType());
		// if (nmsBlock == null) {
		// return false;
		// }
		// net.minecraft.server.v1_16_R3.IBlockData data = nmsBlock.getBlockData();
		// net.minecraft.server.v1_16_R3.Item item =
		// org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers
		// .getItem(tool.getType());
		// if (!data.isRequiresSpecialTool() &&
		// /!block.getType().name().endsWith("_LOG")) {
		// return true;
		// }
		// net.minecraft.server.v1_16_R3.ItemStack nmsItemStack =
		// CraftItemStack.asNMSCopy(tool);
		// double speed = item.getDestroySpeed(nmsItemStack, data);
		// Collection<ItemStack> drops = block.getDrops(tool);

		// return !drops.isEmpty() && speed > 1.0;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getWorld() != WorldManager.getInstance().getMapWorld()) {
			return;
		}

		if (blocksOnCooldown.contains(block.getLocation())) {
			event.setCancelled(true);
			return;
		}

		ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
		if (!isUsableTool(tool, block)) {
			event.setCancelled(true);
			return;
		}
		Material blockType = block.getType();

		if (bConfig.getOreData().containsKey(blockType)) {

			event.setDropItems(false);
			handleOreBreak(event.getPlayer(), block);
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (bConfig.getOreData().containsKey(event.getBlock().getType())
				|| event.getBlock().getType().name().startsWith("STRIPPED_")) {
			event.setCancelled(true);
		}
	}

	// gives the player the loot, sets the block's type to whatever it should be and
	// then sets it back later.
	private void handleOreBreak(Player player, Block block) {
		Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);
		for (ItemStack itemStack : drops) {
			if (itemStack.getType() == Material.EMERALD) {
				BPlayer bPlayer = BPlayer.getBPlayer(player);
				bPlayer.getBackpack().setEmeralds(bPlayer.getBackpack().getEmeralds() + 1);
			} else {
				player.getInventory().addItem(itemStack);
			}
		}

		ArrayList<Location> locations = WorldManager.getGameLocations(bConfig.getOres().get(block.getType()));
		if (locations.contains(block.getLocation())) {
			doBlockCooldown(block);
		}
	}

	private void doBlockCooldown(Block block) {
		OreDataEntry entry = bConfig.getOreData().get(block.getType());
		if (entry == null) {
			return;
		}
		String originalName = block.getType().name();
		if (originalName.startsWith("STRIPPED_")) {
			originalName = originalName.substring(9);
		}
		Material original = Material.valueOf(originalName);
		block.setType(entry.getReplacementMaterial());
		blocksOnCooldown.add(block.getLocation());
		new BukkitRunnable() {

			@Override
			public void run() {
				block.setType(original);
				blocksOnCooldown.remove(block.getLocation());
			}

		}.runTaskLater(app, entry.getTickCooldown());
	}

}
