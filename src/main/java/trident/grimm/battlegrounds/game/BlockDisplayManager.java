package trident.grimm.battlegrounds.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Supplier;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.util.WorldManager;

public class BlockDisplayManager implements Listener {
	private static @Getter BlockDisplayManager instance = new BlockDisplayManager();

	private static record BlockDisplayEntry(Location location, BlockData blockData, Supplier<Boolean> onBreak) {
	}

	private HashMap<UUID, ArrayList<BlockDisplayEntry>> blockDisplayEntries;
	private ArrayList<UUID> ignoreBlockChangePacketList;

	private BlockDisplayManager() {
		this.blockDisplayEntries = new HashMap<>();
		this.ignoreBlockChangePacketList = new ArrayList<>();
	}

	public void start() {
		this.startListener();
		this.startUpdateRunnable();
	}

	private void startUpdateRunnable() {
		new BukkitRunnable() {

			@Override
			public void run() {
				BlockDisplayManager.this.blockDisplayEntries.entrySet().forEach((entry) -> {
					Player player = Bukkit.getPlayer(entry.getKey());
					if (player == null)
						return;

					ignoreBlockChangePacketList.add(entry.getKey());

					entry.getValue().forEach(blockDisplayEntry -> {
						player.sendBlockChange(blockDisplayEntry.location(), blockDisplayEntry.blockData());
					});
					ignoreBlockChangePacketList.remove(entry.getKey());
				});
			}

		}.runTaskTimer(App.getInstance(), 0, 20);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();

		if (!blockDisplayEntries.containsKey(player.getUniqueId()))
			return;

		ArrayList<BlockDisplayEntry> entries = blockDisplayEntries.get(player.getUniqueId());

		entries.forEach(entry -> {
			if (event.getBlock().getLocation().equals(entry.location())) {
				if (!entry.onBreak().get()) {
					event.setCancelled(true);
					refreshBlockChanges(player);
				}
			}
		});

	}

	private void refreshBlockChanges(Player player) {
		ArrayList<BlockDisplayEntry> entries = this.blockDisplayEntries.get(player.getUniqueId());
		if (entries == null)
			return;
		ignoreBlockChangePacketList.add(player.getUniqueId());

		entries.forEach(blockDisplayEntry -> {
			player.sendBlockChange(blockDisplayEntry.location(), blockDisplayEntry.blockData());
		});
		ignoreBlockChangePacketList.remove(player.getUniqueId());

	}

	public void setPlayerBlockDisplay(Player player, Location location, BlockData blockData,
			Supplier<Boolean> onBlockBreak) {
		ArrayList<BlockDisplayEntry> entries = blockDisplayEntries.get(player.getUniqueId());
		if (entries == null) {
			entries = new ArrayList<>();
		}

		entries.removeIf(entry -> entry.location().equals(location.getBlock().getLocation()));

		ignoreBlockChangePacketList.add(player.getUniqueId());

		player.sendBlockChange(location, blockData);
		ignoreBlockChangePacketList.remove(player.getUniqueId());

		entries.add(new BlockDisplayEntry(location.getBlock().getLocation(), blockData, onBlockBreak));

		blockDisplayEntries.put(player.getUniqueId(), entries);

	}

	public void setPlayerBlockDisplay(Player player, Location location, BlockData blockData) {
		setPlayerBlockDisplay(player, location, blockData, () -> {
			return false;
		});
	}

	public void removePlayerBlockDisplay(Player player, Location location) {
		ArrayList<BlockDisplayEntry> entries = blockDisplayEntries.get(player.getUniqueId());
		if (entries == null)
			return;

		entries.removeIf(entry -> entry.location.equals(location.getBlock().getLocation()));

		player.sendBlockChange(location, location.getBlock().getBlockData());
	}

	private void startListener() {
		App.getInstance().getProtocolManager().addPacketListener(
				new PacketAdapter(App.getInstance(), ListenerPriority.HIGHEST,
						PacketType.Play.Server.BLOCK_CHANGE) {

					@Override
					public void onPacketSending(PacketEvent event) {
						if (!GameManager.getInstance().isStarted())
							return;

						Player player = event.getPlayer();
						if (ignoreBlockChangePacketList.contains(player.getUniqueId()))
							return;

						Location location = event.getPacket().getBlockPositionModifier().read(0)
								.toLocation(WorldManager.getInstance().getMapWorld());

						ArrayList<BlockDisplayEntry> entries = blockDisplayEntries.get(player.getUniqueId());
						if (entries == null)
							return;

						if (entries.stream()
								.anyMatch(entry -> entry.location.equals(location.getBlock().getLocation()))) {
							event.setCancelled(true);
						}
					}
				});
	}
}
