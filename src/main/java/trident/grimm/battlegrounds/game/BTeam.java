package trident.grimm.battlegrounds.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.google.common.collect.FluentIterable;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.util.WorldManager;

public enum BTeam {

	RED(TextColor.color(255, 20, 20), Color.fromRGB(255, 0, 0), Material.RED_DYE),
	BLUE(TextColor.color(20, 20, 255), Color.fromRGB(0, 0, 255), Material.BLUE_DYE),
	GREEN(TextColor.color(20, 255, 20), Color.fromRGB(0, 255, 0), Material.GREEN_DYE),
	ORANGE(TextColor.color(255, 127, 0), Color.fromRGB(255, 127, 0), Material.ORANGE_DYE);

	final static int DEFAULT_NEXUS_HEALTH = 10;

	private @Getter TextColor textColor;
	private @Getter Color color;
	private @Getter TextComponent displayName;
	private @Getter Material displayMaterial;

	private ArrayList<Location> spawnPoints;

	private @Getter Location nexusLocation;

	private @Getter HashSet<BPlayer> players;
	private @Getter int nexusHealth;
	private @Getter boolean dead = false;

	private BTeam(TextColor textColor, Color color, Material displayMaterial) {
		this.textColor = textColor;
		this.color = color;

		this.displayName = Component.text().append(Component.text(
				this.name().substring(0, 1).toUpperCase(), this.textColor))
				.append(Component.text(this.name().substring(1).toLowerCase(), this.textColor)).build();

		this.displayMaterial = displayMaterial;
		this.players = new HashSet<>();
		this.nexusHealth = DEFAULT_NEXUS_HEALTH;
	}

	// game has to have started (or rather, world created)
	public void updateWithWorldConfig() {
		updateSpawnPoints();
		updateNexusLocation();
	}

	// called when the game world is loaded so that the spawn points are initialized
	// to locations in the game world.
	private void updateSpawnPoints() {
		this.spawnPoints = WorldManager.getGameLocations(App.getInstance().getBConfig().getSpawnPoints().get(this));
	}

	// called when the game world is loaded so that the nexus location is
	// initialized to a location in the game world.
	private void updateNexusLocation() {
		this.nexusLocation = WorldManager.getGameLocation(App.getInstance().getBConfig().getNexusLocations().get(this));
	}

	// a player joined this team.
	public void playerJoined(BPlayer bPlayer) {
		this.players.add(bPlayer);
	}

	// a player joined this team.
	public void playerJoined(Player player) {
		this.players.add(BPlayer.getBPlayer(player));
	}

	public void playerLeft(BPlayer bPlayer) {
		this.players.remove(bPlayer);
	}

	public void playerLeft(Player player) {
		this.players.remove(BPlayer.getBPlayer(player));
	}

	// get a random spawn point.
	public Location getRandomSpawnPoint() {
		if (this.dead) {
			return WorldManager.getLobbyLocation(App.getInstance().getBConfig().getLobbySpawnPoint());
		}
		Random random = new Random();
		int index = random.nextInt(this.spawnPoints.size());
		return this.spawnPoints.get(index);
	}

	// also checks if the nexus is dead and sets the block to bedrock.
	// TODO: maybe some particle effects on hit.
	public void setNexusHealth(int nexusHealth) {
		if (this.nexusHealth <= 0)
			return;
		this.nexusHealth = nexusHealth;
		if (this.nexusHealth <= 0) {
			this.dead = true;
			this.nexusLocation.getBlock().setType(Material.BEDROCK);
		}
	}

	// get a list of all the bteams sorted by their nexus healths
	public static List<BTeam> getSortedByNexusHealth() {
		return FluentIterable.from(BTeam.values()).toSortedList(new Comparator<BTeam>() {
			@Override
			public int compare(BTeam o1, BTeam o2) {
				return o2.getNexusHealth() - o1.getNexusHealth();
			}
		});
	}

	public int getNumPlayers() {
		return this.players.size();
	}

}
