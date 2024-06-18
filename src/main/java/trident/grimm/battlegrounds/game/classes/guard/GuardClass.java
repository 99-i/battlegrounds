
package trident.grimm.battlegrounds.game.classes.guard;

import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.Parameters;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.game.BlockDisplayManager;
import trident.grimm.battlegrounds.game.CooldownManager;
import trident.grimm.battlegrounds.game.ability.Ability;
import trident.grimm.battlegrounds.game.classes.BClass;
import trident.grimm.battlegrounds.game.players.BPlayer;

// acrobat class
public class GuardClass extends BClass {

	private static Parameters parameters = new Parameters(
			App.getInstance().getBConfig().getClassEntries().get("guard").getParameters());

	private final static int WALL_DURATION_TICKS = parameters.getInt("wall_duration_ticks");
	private final static TextComponent WALL_COOLDOWN_NAME = parameters.getString("wall_cooldown_name");
	private final static int WALL_COOLDOWN_MS = parameters.getInt("wall_cooldown_ms");
	private final static TextComponent FORCEFIELD_COOLDOWN_NAME = parameters.getString("forcefield_cooldown_name");
	private final static int FORCEFIELD_COOLDOWN_MS = parameters.getInt("forcefield_cooldown_ms");
	private final static String FORCEFIELD_ABILITY_ID = parameters.getRawString("forcefield_ability_id");
	private final static int FORCEFIELD_DURATION_TICKS = parameters.getInt("forcefield_duration_ticks");

	final static String CONFIG_SECTION = "guard";

	private HashMap<BTeam, ArrayList<GuardWall>> teamWalls;
	private HashMap<Block, GuardWall> blockWalls;

	public GuardClass() {
		super(Material.SHIELD, Component.text("Guard", TextColor.color(127, 127, 127)), CONFIG_SECTION);

		this.teamWalls = new HashMap<>();
		this.blockWalls = new HashMap<>();

		for (BTeam team : BTeam.values()) {
			this.teamWalls.put(team, new ArrayList<GuardWall>());
		}

		this.startTeamWallCollideListener();

		this.registerFKeyAbility();
		this.registerItemAbility(FORCEFIELD_ABILITY_ID);
	}

	private void startTeamWallCollideListener() {
		new BukkitRunnable() {

			@Override
			public void run() {
				teamWalls.entrySet().forEach(mapEntry -> {
					BTeam bTeam = mapEntry.getKey();
					ArrayList<GuardWall> walls = mapEntry.getValue();

					bTeam.getPlayers().forEach(bPlayer -> {
						Player player = bPlayer.getPlayer();
						if (player == null)
							return;
						Location playerLocation = player.getLocation().add(new Vector(0, 0.8f, 0));

						walls.forEach(guardWall -> {
							ArrayList<Location> locations = guardWall.getAllBlockLocations();

							locations.forEach(location -> {
								if (playerLocation.distanceSquared(location) < 4) {
									BlockDisplayManager.getInstance().setPlayerBlockDisplay(player, location,
											Material.AIR.createBlockData());
								} else {
									guardWall.setBlockForPlayer(player, location);
								}
							});
						});
					});

				});
			}

		}.runTaskTimer(App.getInstance(), 0, 2);
	}

	@Override
	public void abilityUsed(BPlayer bPlayer, Ability ability) {
		switch (ability.getAbilityType()) {
			case F_KEY:
				playerWall(bPlayer);
				break;
			case ITEM:
				if (ability.getAbilityId().equals(FORCEFIELD_ABILITY_ID)) {
					playerForcefield(bPlayer);
				}
				break;
			default:
				break;
		}
	}

	private void playerWall(BPlayer bPlayer) {
		CooldownManager cooldownManager = CooldownManager.getInstance();

		if (!cooldownManager.isCooldownOver(bPlayer, WALL_COOLDOWN_NAME)) {
			return;
		}

		Player player = bPlayer.getPlayer();
		if (player == null)
			return;

		boolean createdWall = createWall(player, bPlayer.getTeam());

		if (createdWall)
			cooldownManager.setCooldown(bPlayer, WALL_COOLDOWN_NAME, WALL_COOLDOWN_MS);
	}

	private boolean createWall(Player player, BTeam bTeam) {
		BlockFace facing = player.getFacing();

		RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(),
				player.getLocation().getDirection(), 5);

		if (result == null)
			return false;

		Block hitBlock = result.getHitBlock();

		if (hitBlock == null)
			return false;

		GuardWall wall = new GuardWall(facing, hitBlock.getLocation(), bTeam, BPlayer.getBPlayer(player));

		wall.start(() -> {
			teamWalls.get(bTeam).add(wall);
			for (Location location : wall.getAllBlockLocations()) {
				this.blockWalls.put(location.getBlock(), wall);
			}
		});

		return true;
	}

	private void playerForcefield(BPlayer bPlayer) {

	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		if (BPlayer.getBPlayer((Player) event.getEntity()).getBClass() != this)
			return;

	}

}
