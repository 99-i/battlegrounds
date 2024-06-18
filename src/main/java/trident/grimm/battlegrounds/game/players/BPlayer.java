package trident.grimm.battlegrounds.game.players;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.BConfig.ArmorData;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.game.CooldownManager;
import trident.grimm.battlegrounds.game.GameManager;
import trident.grimm.battlegrounds.game.HealthManager;
import trident.grimm.battlegrounds.game.StatShower;
import trident.grimm.battlegrounds.game.classes.BClass;
import trident.grimm.battlegrounds.game.classes.BClasses;
import trident.grimm.battlegrounds.game.classes.assassin.AssassinClass;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.BEffects;
import trident.grimm.battlegrounds.game.effects.slowness.SlownessEffect.SlownessEffectSettings;
import trident.grimm.battlegrounds.game.effects.speed.SpeedEffect.SpeedEffectSettings;
import trident.grimm.battlegrounds.game.effects.vulnerability.VulnerabilityEffect.VulnerabilityEffectSettings;
import trident.grimm.battlegrounds.game.players.MovementSpeedSource.MovementSpeedSourceType;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.nms.NametagUtil;
import trident.grimm.battlegrounds.sidebar.SidebarManager;
import trident.grimm.battlegrounds.util.ActionBar;
import trident.grimm.battlegrounds.util.FastUtil;

public class BPlayer {

	private UUID playerUUID;
	private boolean playerInvalidated = false;
	private Player _player;
	private @Getter double health;
	private @Getter @Setter double maxHealth;
	private @Getter double healthRegen; // per second

	private @Getter double baseArmor; // dictated by class. also, some classes may add extra and remove later as part
										// of an ability
	private @Getter double baseMR; // dictated by class. also, some classes may add extra and remove later as part
									// of an ability

	private @Getter double armor; // current armor.
	private @Getter double magicResistance; // current mr.

	private @Getter double baseWalkSpeed; // dictated by class
	private @Getter double walkSpeed; // changes based on effects, abilities etc

	private @Getter @Setter boolean alreadyDied = false; // PlayerDeathEvent is called multiple times, this is used to
															// make it
	private @Getter @Setter boolean slashKill;
	// so that we only register an BPlayer's death once.

	private @Getter BPlayer lastDamager; // the last bplayer who damaged THIS bplayer.
	private BukkitTask cancelLastDamagerTask; // the task for a runnable that runs after a certain time that sets
	// lastdamager to null.

	private @Getter BPlayerBackpack backpack;

	// an effect entry contains settings for an effect, the effect, and the duration
	// remaining.
	public class EffectEntry<T> {
		private @Getter BEffect<T> effect;
		private @Getter @Setter int tickDurationRemaining;
		private @Getter T settings;
		private Class<T> settingsClass;

		public EffectEntry(BEffect<T> effect, int tickDurationRemaining, T settings, Class<T> settingsClass) {
			this.effect = effect;
			this.tickDurationRemaining = tickDurationRemaining;
			this.settings = settings;
			this.settingsClass = settingsClass;
		}

		public void setSettings(Object settings) {
			if (settingsClass.isInstance(settings)) {
				this.settings = settingsClass.cast(settings);
			}
		}

		public void tickForPlayer(Player player) {
			this.effect.playerTick(player, settings);
		}

		public void endForPlayer(Player player) {
			this.effect.getPlayers().remove(player);
			this.effect.playerEnd(player, settings);
		}

		public void stopForPlayer(Player player) {
			this.effect.getPlayers().remove(player);
			this.effect.onStop(player, settings);
		}

		public boolean shouldReplace(Object current, Object candidate) {
			return this.effect.shouldReplaceSettings(settingsClass.cast(current), settingsClass.cast(candidate));
		}

	}

	// keep an array of current effects.
	// each tick all of the durations are reduced by one, and if they're over then
	// they're removed from this array.
	private ArrayList<EffectEntry<?>> effects;
	// this keeps track of effect entries that have already ended, but the player
	// was disconnected while it ended.
	// therefore, when the player reconnects we call all the effect playerEnd
	// methods at once and clear this queue.
	private LinkedList<EffectEntry<?>> queuedEndedEffects;

	// current team
	private @Getter BTeam team;
	// current class
	private @Getter BClass bClass;

	private static HashMap<UUID, BPlayer> bPlayers = new HashMap<>();

	// used by assassin.
	private @Getter boolean isNPC;
	private @Getter BPlayer owner;

	private ArrayList<MovementSpeedSource> movementSpeedSources;

	private BPlayer(Player player) {
		this.playerUUID = player.getUniqueId();
		this._player = player;
		this.effects = new ArrayList<>();
		this.queuedEndedEffects = new LinkedList<>();
		this.backpack = new BPlayerBackpack(this);
		this.movementSpeedSources = new ArrayList<MovementSpeedSource>();
		this.setClass(BClasses.GUARD, true);
		this.startEffectRunnable();

		if (player.hasMetadata("NPC")) {
			this.isNPC = true;
			this.owner = AssassinClass.getOwnerOfNPC(player);
			if (this.owner != null) {
				this.joinTeam(this.owner.getTeam());
			}
		}
	}

	public MovementSpeedSource addMovementSpeedSource(MovementSpeedSourceType type, double value) {
		MovementSpeedSource source = new MovementSpeedSource(type, value);
		this.movementSpeedSources.add(source);
		this.setCorrectWalkSpeed();

		return source;
	}

	public void removeMovementSpeedSource(MovementSpeedSource source) {
		this.movementSpeedSources.remove(source);
		this.setCorrectWalkSpeed();
	}

	// starts a runnable that, every tick, reduces all the durations of the effects
	// in the effect entries array by 1 tick,
	// and removes them (and calls playerEnd) if they're over.
	// also sets correct walk speed.
	private void startEffectRunnable() {
		new BukkitRunnable() {

			@Override
			public void run() {
				Player player;
				player = getPlayer();

				for (int i = 0; i < effects.size(); i++) {
					EffectEntry<?> entry = effects.get(i);
					entry.setTickDurationRemaining(entry.getTickDurationRemaining() - 1);
					BEffect<?> effect = entry.getEffect();
					if (player != null) {
						entry.tickForPlayer(player);
						if (effect == BEffects.REGENERATION) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
									entry.getTickDurationRemaining(), 0));
						}
					}

					if (entry.getTickDurationRemaining() <= 0) {
						effects.remove(i);
						if (entry.getTickDurationRemaining() == 0) {
							if (player != null) {
								entry.endForPlayer(player);
							} else {
								queuedEndedEffects.push(entry);
							}
						}
						i--;
					}
				}
				setCorrectWalkSpeed();
			}
		}.runTaskTimer(App.getInstance(), 0, 1);
	}

	// static method that gets a BPlayer object corresponding to the Player
	// object (or rather, the player's UUID)
	public static BPlayer getBPlayer(Player player) {
		if (bPlayers.containsKey(player.getUniqueId())) {
			return bPlayers.get(player.getUniqueId());
		} else {
			BPlayer bPlayer = new BPlayer(player);
			bPlayers.put(player.getUniqueId(), bPlayer);
			return bPlayer;
		}
	}

	public static Collection<BPlayer> getAllBPlayers() {
		return bPlayers.values();
	}

	private static void removeBPlayer(UUID uuid) {
		BPlayer bPlayer = bPlayers.get(uuid);
		if (bPlayer == null)
			return;

		bPlayer.getTeam().playerLeft(bPlayer.getPlayer());
		bPlayers.remove(uuid);
	}

	public static void removeBPlayer(BPlayer bPlayer) {
		for (Map.Entry<UUID, BPlayer> entry : bPlayers.entrySet()) {
			if (entry.getValue() == bPlayer) {
				removeBPlayer(entry.getKey());
				break;
			}
		}
	}

	public static void removeBPlayerIfPresent(Player player) {
		removeBPlayer(player.getUniqueId());
	}

	// creates the player's custom scoreboard and sets it, and sets the defenses.
	public void createPlayerScoreboard() {
		Player player = this.getPlayer();
		if (player == null)
			return;
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		player.setScoreboard(scoreboard);
		SidebarManager.getInstance().addPlayer(player);
		if (GameManager.getInstance().isStarted()) {
			SidebarManager.getInstance().updatePlayer(player);
		}
	}

	// set the defenses of the scoreboard (armor and mr).

	// also removes the old scoreboard entry and puts the new entry in.
	public void setArmor(double newArmor) {
		Player player = this.getPlayer();
		if (player == null) {
			return;
		}

		this.armor = newArmor;
		StatShower.getInstance().updatePlayerStatline(player);
		// SidebarManager.getInstance().updatePlayer(this.getPlayer());
	}

	// also removes the old scoreboard entry and puts the new entry in.
	public void setMagicResistance(double newMagicResistance) {
		Player player = this.getPlayer();
		if (player == null) {
			return;
		}

		this.magicResistance = newMagicResistance;
		StatShower.getInstance().updatePlayerStatline(player);
	}

	public void slashKill() {
		this.setSlashKill(true);
		this.setHealth(0);
	}

	// joins a team, and if the game started, teleports the player etc.
	public void joinTeam(BTeam team) {
		Player player;
		player = this.getPlayer();
		if (this.team != null) {
			this.team.playerLeft(this);
		}
		this.team = team;
		ActionBar.sendPlayerActionBar(player, Component.text("You have joined the ").append(team.getDisplayName())
				.toBuilder().resetStyle()
				.append(Component.text(" team.")).build());
		this.team.playerJoined(this);

		if (GameManager.getInstance().isStarted() && !this.isNPC) {
			player.teleport(this.team.getRandomSpawnPoint());
			this.spawnIn();
		}
	}

	public void setClass(BClass bClass, boolean isDefault) {
		BClass oldBClass = this.bClass;

		if (oldBClass != null) {
			oldBClass.playerLeftThisClass(this);
		}

		this.bClass = bClass;
		this.setBClassData(isDefault);

		this.bClass.playerBecameThisClass(this);

		if (!isDefault) {
			Player player = this.getPlayer();
			if (player != null) {
				ActionBar.sendPlayerActionBar(player,
						Component.text("You have chosen the ")
								.toBuilder()
								.append(bClass.getDisplayName())
								.resetStyle()
								.append(Component.text(" class."))
								.build());
			}
		}
	}

	// sets all the bclass atributes (mr, max health, etc).
	private void setBClassData(boolean isDefault) {
		Player player = this.getPlayer();
		if (player == null)
			return;

		this.maxHealth = this.bClass.getMaxHealth();
		if (!GameManager.getInstance().isStarted() || this.health > this.maxHealth) {
			this.health = this.maxHealth;
		}
		this.healthRegen = this.bClass.getHealthRegen();

		this.baseArmor = this.bClass.getArmor();
		this.baseMR = this.bClass.getMagicResistance();

		this.armor = this.baseArmor;
		this.magicResistance = this.baseMR;

		this.baseWalkSpeed = this.bClass.getWalkSpeed();
		this.setCorrectWalkSpeed();
	}

	// get the underlying Spigot Player object corresponding to this BPlayer
	// object. throws an exception if the player is not in the server.
	// if the player is found (e.g. they disconnected and reconnected) then loop
	// through the queued effects and end all of them, create the new scoreboard,
	// and update walk speed.
	public Player getPlayer() {
		boolean invalid = this.playerInvalidated;
		if (invalid) {
			this._player = Bukkit.getPlayer(this.playerUUID);
			if (this._player == null) {
				return null;
			}
			this.playerInvalidated = false;

			for (Iterator<EffectEntry<?>> iterator = this.queuedEndedEffects.iterator(); iterator.hasNext();) {
				EffectEntry<?> entry = iterator.next();
				entry.endForPlayer(this._player);
			}

			this.queuedEndedEffects.clear();

			this.setCorrectWalkSpeed();
		}
		if (invalid) {
			this.createPlayerScoreboard();
		}
		return this._player;
	}

	// this method tells this BPlayer object that the underlying Player object
	// (_player) should be discarded (e.g. the player disconnected)
	public void invalidatePlayer() {
		this.playerInvalidated = true;
	}

	// called by CooldownManager everytime ANY cooldown ends
	public void cooldownEnded(TextComponent cooldownName) {
		this.setCorrectWalkSpeed();
		this.bClass.cooldownEnded(this, cooldownName);
	}

	// sets the underlying player's walk speed, based on speed effect level,
	// slowness effect level, meditation status, and possibly other things.
	public void setCorrectWalkSpeed() {
		Player player = this.getPlayer();
		if (player == null) {
			return;
		}

		float newWalkSpeed = getCorrectWalkSpeed();
		if (player.getWalkSpeed() != newWalkSpeed) {
			player.setWalkSpeed((float) newWalkSpeed);
		}
	}

	private float getCorrectWalkSpeed() {
		if (!this.isInTeam()) {
			return 0.2f;
		}

		List<MovementSpeedSource> additiveSources = this.movementSpeedSources.stream()
				.filter(source -> source.type() == MovementSpeedSourceType.ADDITIVE).collect(Collectors.toList());

		List<MovementSpeedSource> multiplicativeSources = this.movementSpeedSources.stream()
				.filter(source -> source.type() == MovementSpeedSourceType.MULTIPLICATIVE).collect(Collectors.toList());

		if (this.movementSpeedSources.stream().anyMatch(source -> source.type() == MovementSpeedSourceType.STOP)) {
			return 0.0f;
		}

		double newWalkSpeed;
		double newBaseWalkSpeed = this.baseWalkSpeed;
		for (MovementSpeedSource source : additiveSources) {
			newBaseWalkSpeed += source.value();
		}

		newWalkSpeed = newBaseWalkSpeed;

		for (MovementSpeedSource source : multiplicativeSources) {
			newWalkSpeed += (source.value() * newBaseWalkSpeed);
		}
		return (float) newWalkSpeed;
	}

	// get the reduction formula for a defense
	public double getDefenseReduction(double defense) {

		int vulnerabilityLevel = 0;
		for (EffectEntry<?> entry : effects) {
			if (entry.effect == BEffects.VULNERABILITY) {
				VulnerabilityEffectSettings settings = (VulnerabilityEffectSettings) entry.settings;
				vulnerabilityLevel = settings.mult;
			}
		}

		double percentReduction = (double) (1.4 * ((1 / (1 + Math.exp(-0.03 * (Math.pow(defense / 10, 2))))) - 0.5))
				* (double) (1 + (0.1 * vulnerabilityLevel));

		return percentReduction;
	}

	public static enum BDamageType {
		PHYSICAL, MAGIC, TRUE
	}

	// damage THIS player.
	public void damage(double damage, BPlayer damager, BDamageType damageType) {
		if (this.isNPC) {
			BClasses.ASSASSIN.npcWasDamaged(damager.getPlayer(), this.getPlayer());
			return;
		}

		double reduction = 1;
		TextColor indicatorColor = NamedTextColor.WHITE;
		switch (damageType) {
			case PHYSICAL:
				reduction = 1 - getDefenseReduction(this.armor);
				indicatorColor = NamedTextColor.DARK_RED;
				break;
			case MAGIC:
				reduction = 1 - getDefenseReduction(this.magicResistance);
				indicatorColor = NamedTextColor.DARK_PURPLE;
				break;
			case TRUE:
				reduction = 1;
				indicatorColor = NamedTextColor.WHITE;
		}

		Player thisPlayer = this.getPlayer();
		if (thisPlayer == null) {
			return;
		}

		Player damagerPlayer = damager.getPlayer();
		if (damagerPlayer == null) {
			return;
		}
		double totalDamage = damage * reduction;
		this.setHealth(this.health - (totalDamage));
		thisPlayer.damage(1);
		HealthManager.getInstance().updatePlayerHealth(thisPlayer);

		NametagUtil.spawnNametag(Arrays.asList(damagerPlayer, thisPlayer), thisPlayer.getLocation(),
				Component.text(FastUtil.doubleToString(totalDamage), indicatorColor),
				30);
		setLastDamager(damager);
	}

	private void setLastDamager(BPlayer damager) {
		this.lastDamager = damager;
		if (this.cancelLastDamagerTask != null) {
			this.cancelLastDamagerTask.cancel();
		}
		this.cancelLastDamagerTask = new BukkitRunnable() {

			@Override
			public void run() {
				lastDamager = null;
			}

			// todo: hardcoded number 50
		}.runTaskLater(App.getInstance(), 50);
	}

	// called when THIS bplayer kills VICTIM
	public void killedPlayer(BPlayer victim) {
		this.getBClass().killedPlayer(this, victim);
	}

	// check armor pieces on underlying Spigot Player and update defenses
	// accordingly.
	public void refreshDefenseChanges() {
		Player player = this.getPlayer();
		if (player == null) {
			return;
		}

		ItemStack[] contents = player.getInventory().getArmorContents();

		HashMap<Material, ArmorData> armorData = App.getInstance().getBConfig().getArmorAttributes();

		this.armor = this.baseArmor;
		this.magicResistance = this.baseMR;

		for (int i = 0; i < contents.length; i++) {
			ItemStack armorPiece = contents[i];
			if (armorPiece == null)
				continue;
			Material type = armorPiece.getType();
			if (armorData.containsKey(type)) {
				this.armor += armorData.get(type).getArmor();
				this.magicResistance += armorData.get(type).getMagicResistance();
			}
		}

		StatShower.getInstance().updatePlayerStatline(player);
	}

	public void setBaseArmor(double baseArmor) {
		this.baseArmor = baseArmor;
	}

	public void resetBaseArmor() {
		this.baseArmor = this.bClass.getArmor();
	}

	public void setBaseMR(double baseMR) {
		this.baseMR = baseMR;
	}

	public void resetBaseMR() {
		this.baseMR = this.bClass.getMagicResistance();
	}

	// set the player's health.
	// todo: absorption?
	public void setHealth(double health) {

		this.health = health;
		if (this.health >= this.maxHealth) {
			this.health = this.maxHealth;
		}

		Player player = this.getPlayer();
		if (player != null) {
			double dHealth = Math.abs(this.health - health);
			if (this.health <= 0) {
				player.setHealth(0);
				onDeath();
			}
			if (dHealth > (0.1 * this.maxHealth)) {
				HealthManager.getInstance().updatePlayerHealth(player);
			}
		}
	}

	private void onDeath() {
		this.lastDamager = null;
	}

	public boolean isInTeam() {
		return this.team != null;
	}

	// set the player's inventory based on class, update defenses. doesn't actually
	// teleport the player to a spawnpoint.
	public void spawnIn() {
		if (this.team == null)
			return;
		Player player = this.getPlayer();
		if (player == null) {
			return;
		}

		player.getInventory().clear();
		this.bClass.setStarterInventory(this);
		this.setPlayerBackpackChest();
		new BukkitRunnable() {
			@Override
			public void run() {
				refreshDefenseChanges();
			}
		}.runTask(App.getInstance());
	}

	// get the settings object of an effect.
	public <T> T getEffectSettings(BEffect<T> effect, Class<T> settingsType) {
		for (EffectEntry<?> entry : effects) {
			if (entry.getEffect() == effect) {
				if (settingsType.isInstance(entry.settings)) {
					return settingsType.cast(entry.settings);
				}
			}
		}
		return null;
	}

	// give an effect to this bplayer.
	// logic:
	// if the player does not already have this effect, give the player this effect
	// and this settings.
	// if the player already has this effect, then BEffect#shouldReplaceSettings
	// determines if to replace the settings or not.
	// the effect being replaced means: effect is first stopped. then a new effect
	// entry is created.
	// the duration is only replaced if it is higher than the current duration.
	public <T> void giveEffect(BEffect<T> effect, int tickDuration, T settings, Class<T> settingsClass) {
		Player player = this.getPlayer();
		if (player == null) {
			return;
		}

		boolean shouldNotReplace = false;
		CooldownManager cooldownManager = CooldownManager.getInstance();

		for (EffectEntry<?> entry : effects) {
			if (entry.getEffect() == effect) {
				if (entry.shouldReplace(entry.settings, settings)) {
					entry.stopForPlayer(player);
				} else {
					shouldNotReplace = true;
				}
				break;
			}
		}
		if (shouldNotReplace)
			return;

		EffectEntry<T> entry = new EffectEntry<>(effect, tickDuration, settings, settingsClass);
		effects.add(entry);
		effect.getPlayers().add(this.getPlayer());
		effect.playerStart(player, settings);
		cooldownManager.setCooldown(this, effect.getDisplayName(), tickDuration * 50);
	}

	public void removeEffect(BEffect<?> effect) {
		for (EffectEntry<?> entry : this.effects) {
			if (entry.getEffect() == effect) {
				entry.setTickDurationRemaining(0);
				Player player = this.getPlayer();
				if (player != null) {
					entry.stopForPlayer(player);
					CooldownManager.getInstance().setCooldown(this, entry.effect.getDisplayName(), 0);
				} else {
					this.queuedEndedEffects.push(entry);
				}
			}
		}
	}

	// clear all effects.
	public void clearEffects() {
		Player player = this.getPlayer();
		for (EffectEntry<?> entry : effects) {
			entry.setTickDurationRemaining(0);
			if (player != null) {
				entry.stopForPlayer(player);
				CooldownManager.getInstance().setCooldown(this, entry.effect.getDisplayName(), 0);
			}
		}
	}

	public boolean hasEffect(BEffect<?> effect) {
		for (EffectEntry<?> entry : effects) {
			if (entry.effect == effect) {
				return true;
			}
		}
		return false;
	}

	public void setPlayerBackpackChest() {
		Player player = this.getPlayer();
		if (player == null) {
			return;
		}

		player.getInventory().setItem(9, ItemUtil.getBackpackItem());
	}

	public void openBackpackInventory() {
		Player player = this.getPlayer();
		if (player == null) {
			return;
		}
		this.backpack.updateGUI();
		this.backpack.getGui().setPlayerView(player, "main");
	}

}
