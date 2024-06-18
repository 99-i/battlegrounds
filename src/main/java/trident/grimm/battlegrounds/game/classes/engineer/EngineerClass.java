package trident.grimm.battlegrounds.game.classes.engineer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import lombok.Getter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.Parameters;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.game.CombatManager;
import trident.grimm.battlegrounds.game.CooldownManager;
import trident.grimm.battlegrounds.game.GameManager;
import trident.grimm.battlegrounds.game.ability.Ability;
import trident.grimm.battlegrounds.game.classes.BClass;
import trident.grimm.battlegrounds.game.classes.BClassUtil;
import trident.grimm.battlegrounds.game.classes.engineer.Sentry.UpgradeType;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.MovementSpeedSource;
import trident.grimm.battlegrounds.game.players.BPlayer.BDamageType;
import trident.grimm.battlegrounds.game.players.MovementSpeedSource.MovementSpeedSourceType;
import trident.grimm.battlegrounds.items.ItemUtil;

public class EngineerClass extends BClass {

	private static Parameters parameters = new Parameters(
			App.getInstance().getBConfig().getClassEntries().get("engineer").getParameters());
	private static final String MINES_ABILITY_ID = parameters.getRawString("mines_ability_id");
	private static final TextComponent MINES_COOLDOWN_NAME = parameters.getString("mines_cooldown_name");
	private static final int MINES_COOLDOWN_MS = parameters.getInt("mines_cooldown_ms");
	private static final TextComponent SENTRY_COOLDOWN_NAME = parameters.getString("sentry_cooldown_name");
	private static final int SENTRY_COOLDOWN_MS = parameters.getInt("sentry_cooldown_ms");

	final static String CONFIG_SECTION = "engineer";
	private HashMap<Sentry, BPlayer> playerSentries = new HashMap<>();

	private HashMap<Mine, BPlayer> playerMines = new HashMap<>();

	private HashMap<BTeam, ArrayList<Location>> buildingLocations;
	private @Getter NPCRegistry npcRegistry;

	private HashMap<Entity, Sentry> entitySentries; // map of pillager/pig entities to their respective sentries.
													// therefore, this map grows by 2 for every sentry in the game.
													// todo: maybe there's a better way to do this?

	private HashMap<BPlayer, MovementSpeedSource> intuitiveSources; // movementspeedsources for intuitive (passive).

	public EngineerClass() {
		// todo!: put this in the config.
		super(Material.NETHERITE_PICKAXE, Component.text("Engineer", TextColor.color(0, 255, 0)), CONFIG_SECTION);
		npcRegistry = CitizensAPI.createInMemoryNPCRegistry("ENGINEER_NPCS");

		this.registerFKeyAbility();
		this.registerItemAbility(MINES_ABILITY_ID);
		this.startPassiveListener();
		this.buildingLocations = new HashMap<>();

		for (BTeam team : BTeam.values()) {
			this.buildingLocations.put(team, new ArrayList<>());
		}

		this.entitySentries = new HashMap<>();
		this.intuitiveSources = new HashMap<>();
	}

	private void startPassiveListener() {
		new BukkitRunnable() {
			@Override
			public void run() {
				List<BPlayer> engineers = BClassUtil.getBPlayersOfClass(EngineerClass.this);

				for (BPlayer engineer : engineers) {
					if (!engineer.isInTeam())
						continue;
					boolean inRangeOfBuilding = false;
					Player player = engineer.getPlayer();
					if (player == null)
						continue;

					ArrayList<Location> buildingLocationsForPlayersTeam = buildingLocations.get(engineer.getTeam());
					Location playerLocation = player.getLocation();
					for (Location location : buildingLocationsForPlayersTeam) {
						if (playerLocation.distanceSquared(location) <= 100) {
							inRangeOfBuilding = true;
							break;
						}
					}

					if (inRangeOfBuilding) {
						if (!intuitiveSources.containsKey(engineer)) {
							MovementSpeedSource source = engineer
									.addMovementSpeedSource(MovementSpeedSourceType.MULTIPLICATIVE, 0.1);
							intuitiveSources.put(engineer, source);
						}
					} else {
						if (intuitiveSources.containsKey(engineer)) {
							engineer.removeMovementSpeedSource(intuitiveSources.get(engineer));
							intuitiveSources.remove(engineer);
						}
					}

				}

			}
		}.runTaskTimer(App.getInstance(), 0, 2);
	}

	@Override
	public void abilityUsed(BPlayer bPlayer, Ability ability) {
		switch (ability.getAbilityType()) {
			case F_KEY:
				playerSentry(bPlayer);
				break;
			case ITEM:
				playerMine(bPlayer);
			default:
				break;
		}
	}

	private boolean playerHasMineSet(BPlayer bPlayer) {
		for (Map.Entry<Mine, BPlayer> entry : playerMines.entrySet()) {
			if (entry.getValue() == bPlayer) {
				return true;
			}
		}
		return false;
	}

	private Mine getPlayerMine(BPlayer bPlayer) {
		for (Map.Entry<Mine, BPlayer> entry : playerMines.entrySet()) {
			if (entry.getValue() == bPlayer) {
				return entry.getKey();
			}
		}
		return null;
	}

	private void playerMine(BPlayer bPlayer) {
		if (playerHasMineSet(bPlayer)) {
			playerDetonateMine(bPlayer);
		} else {
			playerPlaceMine(bPlayer);
		}
	}

	private void playerPlaceMine(BPlayer bPlayer) {
		Player player = bPlayer.getPlayer();

		if (player == null)
			return;

		if (!CooldownManager.getInstance().isCooldownOver(bPlayer, MINES_COOLDOWN_NAME))
			return;

		RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(),
				player.getLocation().getDirection(), 5);

		if (result == null)
			return;

		Mine mine = new Mine(bPlayer, result.getHitPosition().toLocation(player.getWorld()));
		playerMines.put(mine, bPlayer);
		MineStatus.setPlayerMineStatus(bPlayer, MineStatus.READYING);
		buildingLocations.get(bPlayer.getTeam()).add(mine.getPosition());
		mine.ready();
	}

	private void playerDetonateMine(BPlayer bPlayer) {
		Mine mine = getPlayerMine(bPlayer);
		if (mine == null)
			return;

		if (!mine.isEnabled())
			return;

		mine.detonate();
		buildingLocations.get(bPlayer.getTeam()).remove(mine.getPosition());
		playerMines.remove(mine);
		// todo!: hardcoded everything lol
		CooldownManager.getInstance().setCooldown(bPlayer, MINES_COOLDOWN_NAME, MINES_COOLDOWN_MS);
		MineStatus.setPlayerMineStatus(bPlayer, MineStatus.DISABLED);
	}

	private void playerSentry(BPlayer bPlayer) {
		CooldownManager cooldownManager = CooldownManager.getInstance();
		Player player = bPlayer.getPlayer();
		if (player == null)
			return;

		if (!cooldownManager.isCooldownOver(bPlayer, SENTRY_COOLDOWN_NAME))
			return;
		// todo!: hardcoded number of max sentries
		if (numberOfSentriesPlayerHas(bPlayer) >= 3) {
			player.sendMessage(
					Component.text("You have the maximum number of sentries! (3)", TextColor.color(255, 0, 0)));
			return;
		}

		RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(),
				player.getLocation().getDirection(), 5);

		if (result == null)
			return;

		Sentry sentry = new Sentry(result.getHitPosition().toLocation(player.getWorld()), bPlayer, npcRegistry);

		sentry.spawn();

		entitySentries.put(sentry.getPigEntity().getNpc().getEntity(), sentry);
		entitySentries.put(sentry.getPillagerEntity().getNpc().getEntity(), sentry);

		cooldownManager.setCooldown(bPlayer, SENTRY_COOLDOWN_NAME, SENTRY_COOLDOWN_MS);

		playerSentries.put(sentry, bPlayer);
		buildingLocations.get(bPlayer.getTeam()).add(sentry.getLocation());
	}

	@Override
	public void playerBecameThisClass(BPlayer bPlayer) {
		if (GameManager.getInstance().isStarted())
			MineStatus.setPlayerMineStatus(bPlayer, MineStatus.DISABLED);
	}

	@Override
	public void playerLeftThisClass(BPlayer bPlayer) {
		ArrayList<Sentry> sentriesToRemove = new ArrayList<>();
		for (Map.Entry<Sentry, BPlayer> entry : playerSentries.entrySet()) {
			if (entry.getValue() == bPlayer) {
				sentriesToRemove.add(entry.getKey());
			}
		}
		for (Sentry sentry : sentriesToRemove) {
			buildingLocations.get(sentry.getPlacer().getTeam()).remove(sentry.getLocation());
			sentry.died();
			playerSentries.remove(sentry);
		}

		playerMines.remove(getPlayerMine(bPlayer));

		MineStatus.removePlayerMineStatus(bPlayer);
	}

	@Override
	public void gameStartForClassPlayer(BPlayer bPlayer) {
		MineStatus.setPlayerMineStatus(bPlayer, MineStatus.DISABLED);
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!event.getEntity().hasMetadata("NPC"))
			return;
		Entity entity = event.getEntity();
		Entity damager = event.getDamager();

		if (!(damager instanceof Player)) {
			return;
		}

		if (!entitySentries.containsKey(entity))
			return;
		event.setCancelled(true);

		double damage = CombatManager.getPlayerDamage(BPlayer.getBPlayer((Player) damager),
				((Player) event.getDamager()).getEquipment().getItem(EquipmentSlot.HAND));
		event.setDamage(0);

		Sentry sentry = entitySentries.get(entity);

		BTeam placerTeam = sentry.getPlacer().getTeam();
		BTeam damagerTeam = BPlayer.getBPlayer((Player) damager).getTeam();

		if (placerTeam == damagerTeam) {
			return;
		}

		sentry.wasDamaged(damage);
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (!event.getEntity().hasMetadata("NPC"))
			return;
		if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
				|| event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
				|| event.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
			return;
		} else if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
			Bukkit.broadcast(Component.text("TODO: Sentry NPC received Projectile Hit!")
					.color(TextColor.color(255, 0, 0)));
			return;
		}
		Entity entity = event.getEntity();

		if (!entitySentries.containsKey(entity))
			return;

		// Sentry sentry = entitySentries.get(entity);
		// sentry.wasDamaged(event.getDamage());

		Bukkit.broadcast(Component
				.text("Sentry NPC received EntityDamageEvent (" + event.getCause().name() + ")! Mark this edge case!")
				.color(TextColor.color(255, 0, 0)));
		Bukkit.broadcast(Component.text(event.getCause().name()));
		event.setDamage(0);
	}

	@EventHandler
	public void onNPCRightClick(NPCRightClickEvent event) {

		NPC npc = event.getNPC();
		Entity entity = npc.getEntity();

		if (!this.entitySentries.containsKey(entity))
			return;

		Sentry sentry = this.entitySentries.get(entity);

		if (sentry == null)
			return;

		BPlayer clickerBPlayer = BPlayer.getBPlayer(event.getClicker());
		if (sentry.getPlacer() != clickerBPlayer)
			return;

		sentry.openUpgradeGUIForPlayer(event.getClicker());
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		Projectile projectile = event.getEntity();
		if (projectile instanceof Arrow) {
			Arrow arrow = (Arrow) projectile;
			Sentry sentry = getSentryFromArrow(arrow);
			if (sentry == null)
				return;
			if (event.getHitEntity() != null) {
				if (event.getHitEntity() instanceof Player) {
					BPlayer hitBPlayer = BPlayer.getBPlayer((Player) event.getHitEntity());
					if (hitBPlayer.getTeam() == sentry.getPlacer().getTeam()) {
						event.setCancelled(true);
					} else {
						hitBPlayer.damage(sentry.getUpgradeValue(UpgradeType.DAMAGE), sentry.getPlacer(),
								BDamageType.PHYSICAL);
					}
				}
			}
		}
	}

	private Sentry getSentryFromArrow(Arrow arrow) {
		String pillagerId = ItemUtil.getValue(arrow.getPersistentDataContainer(),
				"sentry-pillager-id");
		if (pillagerId != null) {
			Pillager pillager = (Pillager) Bukkit.getEntity(UUID.fromString(pillagerId));
			return this.entitySentries.get(pillager);
		}

		return null;
	}

	private int numberOfSentriesPlayerHas(BPlayer bPlayer) {
		if (bPlayer.getBClass() != this)
			return 0;

		int count = 0;
		for (Map.Entry<Sentry, BPlayer> entry : playerSentries.entrySet()) {
			if (entry.getValue() == bPlayer) {
				count++;
			}
		}

		return count;
	}

	public void sentryDied(Sentry sentry) {
		buildingLocations.get(sentry.getPlacer().getTeam()).remove(sentry.getLocation());
		playerSentries.remove(sentry);
		entitySentries.entrySet().removeIf(entry -> entry.getValue() == sentry);
	}
}
