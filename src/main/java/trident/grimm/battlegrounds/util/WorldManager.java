package trident.grimm.battlegrounds.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.BConfig.Vector;
import trident.grimm.battlegrounds.redis.RedisManager;

public class WorldManager {

	public static @Getter WorldManager instance = new WorldManager();

	private World mapWorld;
	private File mapDirectory;
	boolean mapLoaded = false;

	private class WorldCopyFileFilter implements FileFilter {
		HashSet<String> ignore = new HashSet<>();

		public WorldCopyFileFilter() {
			ignore.add("uid.dat");
			ignore.add("session.lock");
			ignore.add("session.dat");
		}

		@Override
		public boolean accept(File pathName) {
			if (ignore.contains(pathName.getName())) {
				return false;
			} else {
				return true;
			}
		}
	}

	public void loadMap() {
		Random random = new Random();
		String folderName = "map_" + Math.abs(random.nextInt() * random.nextInt() * 100);
		File worldFile = new File("maps/" + RedisManager.getInstance().getCurrentVersion());
		this.mapDirectory = new File(folderName);

		try {
			FileUtils.copyDirectory(worldFile, this.mapDirectory, new WorldCopyFileFilter());
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.mapWorld = Bukkit.createWorld(new WorldCreator(folderName));
		this.mapWorld.setAutoSave(false);
		this.mapWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
		this.mapWorld.setGameRule(GameRule.NATURAL_REGENERATION, false);
		this.mapWorld.setDifficulty(Difficulty.NORMAL);
		this.mapWorld.setTime(1000);
		this.mapLoaded = true;
	}

	public void deleteMap() {
		if (!this.mapLoaded)
			return;
		for (Player player : this.mapWorld.getPlayers()) {
			player.teleport(getLobbyLocation(App.getInstance().getBConfig().getLobbySpawnPoint()));
		}

		Bukkit.unloadWorld(this.mapWorld, false);
		try {
			FileUtils.deleteDirectory(this.mapDirectory);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<Location> getGameLocations(ArrayList<Vector> vectors) {
		ArrayList<Location> locations = new ArrayList<>();

		for (Vector vector : vectors) {
			locations.add(getGameLocation(vector));
		}

		return locations;
	}

	public static Location getGameLocation(Vector vector) {
		return getLoc(WorldManager.getInstance().getMapWorld(), vector);
	}

	public static Location getLobbyLocation(Vector vector) {
		return getLoc(Bukkit.getWorld("world"), vector);
	}

	private static Location getLoc(World world, Vector vec) {
		return new Location(world, vec.getX(), vec.getY(), vec.getZ());
	}

	public World getMapWorld() {
		return this.mapWorld;
	}
}
