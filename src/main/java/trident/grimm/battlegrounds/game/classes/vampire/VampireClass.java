package trident.grimm.battlegrounds.game.classes.vampire;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.Parameters;
import trident.grimm.battlegrounds.game.CooldownManager;
import trident.grimm.battlegrounds.game.ability.Ability;
import trident.grimm.battlegrounds.game.classes.BClass;
import trident.grimm.battlegrounds.game.classes.BClassUtil;
import trident.grimm.battlegrounds.game.effects.BEffects;
import trident.grimm.battlegrounds.game.effects.vampire_mark.VampireMarkEffect.VampireMarkSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.BPlayer.BDamageType;

public class VampireClass extends BClass {

	final static String CONFIG_SECTION = "vampire";
	private static Parameters parameters = new Parameters(
			App.getInstance().getBConfig().getClassEntries().get("vampire").getParameters());

	private final static String FANGS_ABILITY_ID = parameters.getRawString("fangs_ability_id");
	private final static TextComponent FANGS_COOLDOWN_NAME = parameters.getString("fangs_cooldown_name");
	private final static double FANGS_RANGE = parameters.getDouble("fangs_range");
	private final static int FANGS_DURATION_TICKS = parameters.getInt("fangs_duration_ticks");
	private final static int FANGS_COOLDOWN_MS = parameters.getInt("fangs_cooldown_ms");

	private final static TextComponent BLOODRUSH_COOLDOWN_NAME = parameters.getString("bloodrush_cooldown_name");
	private final static TextComponent BLOODRUSH_EFFECT_NAME = parameters.getString("bloodrush_effect_name");
	private final static int BLOODRUSH_COOLDOWN_MS = parameters.getInt("bloodrush_cooldown_ms");
	private final static int BLOODRUSH_DURATION_TICKS = parameters.getInt("bloodrush_duration_ticks");

	private final static double STEAL_HEALTH_DAMAGE = parameters.getDouble("steal_health_damage");
	private final static double BLOODRUSH_STEAL_HEALTH_CHANCE = parameters.getDouble("bloodrush_steal_health_chance");

	public VampireClass() {
		super(Material.REDSTONE, Component.text("Vampire", TextColor.color(255, 40, 40)), CONFIG_SECTION);
		this.registerFKeyAbility();
		this.registerItemAbility(FANGS_ABILITY_ID);

	}

	@Override
	public void abilityUsed(BPlayer bPlayer, Ability ability) {
		switch (ability.getAbilityType()) {
			case F_KEY:
				playerBloodrush(bPlayer);
				break;
			case ITEM:
				if (ability.getAbilityId().equals(FANGS_ABILITY_ID)) {
					playerFangs(bPlayer);
				}
				break;
			default:
				break;
		}
	}

	private void playerFangs(BPlayer bPlayer) {
		CooldownManager cooldownManager = CooldownManager.getInstance();

		if (!cooldownManager.isCooldownOver(bPlayer, FANGS_COOLDOWN_NAME)) {
			return;
		}

		Player player = bPlayer.getPlayer();
		if (player == null)
			return;
		ArrayList<BPlayer> nearbyPlayers = BClassUtil.getNearbyBPlayers(player, FANGS_RANGE);
		for (BPlayer nearby : nearbyPlayers) {
			nearby.giveEffect(BEffects.VAMPIRE_MARK, FANGS_DURATION_TICKS, new VampireMarkSettings(bPlayer),
					VampireMarkSettings.class);
		}

		cooldownManager.setCooldown(bPlayer, FANGS_COOLDOWN_NAME, FANGS_COOLDOWN_MS);

	}

	private void playerBloodrush(BPlayer bPlayer) {
		CooldownManager cooldownManager = CooldownManager.getInstance();

		if (!cooldownManager.isCooldownOver(bPlayer, BLOODRUSH_COOLDOWN_NAME))
			return;

		if (!cooldownManager.isCooldownOver(bPlayer, BLOODRUSH_EFFECT_NAME))
			return;

		cooldownManager.setCooldown(bPlayer, BLOODRUSH_EFFECT_NAME, BLOODRUSH_DURATION_TICKS * 50);

	}

	public static boolean playerIsInBloodrush(BPlayer bPlayer) {
		return !CooldownManager.getInstance().isCooldownOver(bPlayer, BLOODRUSH_EFFECT_NAME);
	}

	@Override
	public void handleHit(BPlayer damager, BPlayer victim) {
		if (damager.getBClass() != this)
			return;

		double healthStolen = STEAL_HEALTH_DAMAGE;
		boolean bloodrush = playerIsInBloodrush(damager);

		if (!bloodrush) {
			return;
		}

		if (Math.random() <= BLOODRUSH_STEAL_HEALTH_CHANCE) {
			victim.damage(healthStolen, damager, BDamageType.TRUE);
			doHeal(damager, victim, healthStolen);
		}

		if (victim.hasEffect(BEffects.VAMPIRE_MARK)) {
			if (victim.getEffectSettings(BEffects.VAMPIRE_MARK, VampireMarkSettings.class).markInflicter() == damager) {
				victim.removeEffect(BEffects.VAMPIRE_MARK);
				victim.damage(30, damager, BDamageType.TRUE);
			}

		}

	}

	private static void doHeal(BPlayer vampire, BPlayer victim, double healthStolen) {
		Player vampirePlayer = vampire.getPlayer();

		Player victimPlayer = victim.getPlayer();

		if (vampirePlayer != null && victimPlayer != null) {
			vampirePlayer.getWorld().spawnParticle(Particle.HEART, victimPlayer.getLocation().add(0, 1, 0), 8, 0.2, 0.2,
					0.2);
		}

		vampire.setHealth(vampire.getHealth() + healthStolen);
	}

	@Override
	public void killedPlayer(BPlayer killer, BPlayer victim) {
		Player player = killer.getPlayer();
		if (player == null)
			return;
		player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 8, 0.2, 0.2,
				0.2);
		killer.setHealth(killer.getHealth() + 60.0d); // TODO: hardcoded number
	}

	@Override
	public void cooldownEnded(BPlayer bPlayer, TextComponent cooldownName) {
		CooldownManager cooldownManager = CooldownManager.getInstance();
		if (cooldownName.equals(BLOODRUSH_EFFECT_NAME)) {
			cooldownManager.setCooldown(bPlayer, BLOODRUSH_COOLDOWN_NAME, BLOODRUSH_COOLDOWN_MS);
		}
	}

}
