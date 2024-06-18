package trident.grimm.battlegrounds.game.classes.guard;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.game.BlockDisplayManager;
import trident.grimm.battlegrounds.game.players.BPlayer;

public class GuardWall {
	public static enum GuardWallOrientation {
		X_PLANE,
		Z_PLANE
	}

	private GuardWallOrientation orientation;
	private BlockFace facing;
	private Location origin;
	private @Getter BTeam team;
	private @Getter ArrayList<Location> allBlockLocations;
	private ArrayList<ArrayList<Location>> blockLocations;

	private @Getter BPlayer placer;

	public GuardWall(BlockFace facing, Location origin, BTeam team, BPlayer placer) {
		switch (facing) {
			case NORTH:
			case SOUTH:
				this.orientation = GuardWallOrientation.X_PLANE;
				break;
			case EAST:
			case WEST:
				this.orientation = GuardWallOrientation.Z_PLANE;
				break;
			default:
				throw new InvalidParameterException("Facing is not north, south, east, or west!");
		}

		this.facing = facing;
		this.origin = origin;
		this.allBlockLocations = new ArrayList<>();
		this.blockLocations = new ArrayList<>();
		this.team = team;
		this.placer = placer;
	}

	public void start(Runnable afterwards) {

		for (int i = 1; i < 5; i++) {
			ArrayList<Location> rowBlockLocations = new ArrayList<>();
			switch (orientation) {
				case X_PLANE:
					switch (facing) {
						case SOUTH:
							for (double d = -2; d <= 2; d += 1) {
								rowBlockLocations.add(origin.clone().add(new Vector(d, i, 0)));
							}
							break;
						case NORTH:
							for (double d = 2; d >= -2; d -= 1) {
								rowBlockLocations.add(origin.clone().add(new Vector(d, i, 0)));
							}
							break;
						default:
							break;
					}
					break;
				case Z_PLANE:
					switch (facing) {
						case WEST:
							for (double d = -2; d <= 2; d += 1) {
								rowBlockLocations.add(origin.clone().add(new Vector(0, i, d)));
							}
						case EAST:
							for (double d = 2; d >= -2; d -= 1) {
								rowBlockLocations.add(origin.clone().add(new Vector(0, i, d)));
							}
						default:
							break;
					}
					break;
			}
			blockLocations.add(rowBlockLocations);
		}

		for (ArrayList<Location> locs : this.blockLocations) {
			for (Location loc : locs) {
				this.allBlockLocations.add(loc);
			}
		}

		this.doAnimation(afterwards);
	}

	private void doAnimation(Runnable afterwards) {

		new BukkitRunnable() {
			int i = 0;

			@Override
			public void run() {
				ArrayList<Location> rowBlockLocations = GuardWall.this.blockLocations.get(i);

				new BukkitRunnable() {
					int index = 0;
					int iCopy = i;

					@Override
					public void run() {
						Location loc = rowBlockLocations.get(index);
						if (loc.getBlock().getType() == Material.AIR) {
							setBlock(loc);
							index++;
						} else {
							rowBlockLocations.remove(index);
						}

						if (index >= blockLocations.size()) {
							if (iCopy == 3) {
								afterwards.run();
							}
							this.cancel();
						}
					}

				}.runTaskTimer(App.getInstance(), 0, 1);

				i++;

				if (i == 4) {
					this.cancel();
				}
			}

		}.runTaskTimer(App.getInstance(), 0, 5); // todo: hardcoded number for delay

	}

	public void setBlockForPlayer(Player player, Location location) {
		Material glassPaneMaterial = Material.valueOf(this.team.name() + "_STAINED_GLASS_PANE");

		GlassPane pane = (GlassPane) glassPaneMaterial.createBlockData();

		switch (this.orientation) {
			case X_PLANE:
				pane.setFace(BlockFace.EAST, true);
				pane.setFace(BlockFace.WEST, true);
				break;
			case Z_PLANE:
				pane.setFace(BlockFace.SOUTH, true);
				pane.setFace(BlockFace.NORTH, true);
				break;
		}
		BlockDisplayManager.getInstance().setPlayerBlockDisplay(player, location, pane);
	}

	public void setBlock(Location location) {
		Material glassPaneMaterial = Material.valueOf(this.team.name() + "_STAINED_GLASS_PANE");

		GlassPane pane = (GlassPane) glassPaneMaterial.createBlockData();

		switch (this.orientation) {
			case X_PLANE:
				pane.setFace(BlockFace.EAST, true);
				pane.setFace(BlockFace.WEST, true);
				break;
			case Z_PLANE:
				pane.setFace(BlockFace.SOUTH, true);
				pane.setFace(BlockFace.NORTH, true);
				break;
		}
		for (Player player : Bukkit.getOnlinePlayers()) {
			BPlayer bPlayer = BPlayer.getBPlayer(player);
			if (bPlayer.isInTeam()) {
				BlockDisplayManager.getInstance().setPlayerBlockDisplay(player, location, pane);
			}
		}

		Player thisPlacer = this.placer.getPlayer();
		if (thisPlacer == null)
			return;

		BlockDisplayManager.getInstance().setPlayerBlockDisplay(thisPlacer, location, pane, () -> {
			return true;
		});
	}

}
