package trident.grimm.battlegrounds.game.classes.acrobat;

import java.awt.Color;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;

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
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.BPlayer.BDamageType;
import trident.grimm.battlegrounds.util.Pair;

// acrobat class
public class AcrobatClass extends BClass {

	private static Parameters parameters = new Parameters(
			App.getInstance().getBConfig().getClassEntries().get("acrobat").getParameters());

	private static final TextComponent DOUBLE_JUMP_COOLDOWN_NAME = parameters.getString("double_jump_cooldown_name");
	private static final int DOUBLE_JUMP_COOLDOWN_MS = parameters.getInt("double_jump_cooldown_ms");
	private static final TextComponent DAGGER_COOLDOWN_NAME = parameters.getString("dagger_cooldown_name");
	private static final double DAGGER_RANGE = parameters.getDouble("dagger_range");
	private static final double DAGGER_DAMAGE = parameters.getDouble("dagger_damage");
	private static final int DAGGER_COOLDOWN_MS = parameters.getInt("dagger_cooldown_ms");

	// the config section of this class.
	final static String CONFIG_SECTION = "acrobat";

	public AcrobatClass() {
		super(Material.FEATHER, Component.text("Acrobat", TextColor.color(127, 127, 127)), CONFIG_SECTION);

		this.registerDoubleJumpAbility();
		this.registerFKeyAbility();
	}

	@Override
	public void abilityUsed(BPlayer bPlayer, Ability ability) {
		switch (ability.getAbilityType()) {
			case F_KEY:
				playerDagger(bPlayer);
				break;
			case DOUBLE_JUMP:
				playerDoubleJump(bPlayer);
				break;
			default:
				break;
		}
	}

	private void playerDoubleJump(BPlayer bPlayer) {

		Player player = bPlayer.getPlayer();
		if (player == null)
			return;

		if (bPlayer.hasEffect(BEffects.GROUNDED)) {
			player.sendMessage(Component.text("You are grounded and ")
					.append(Component.text("cannot double jump!").color(TextColor.color(255, 100, 100))));
			return;
		} else if (bPlayer.hasEffect(BEffects.SUBDUED)) {
			player.sendMessage(Component.text("You are subdued and ")
					.append(Component.text("cannot double jump!").color(TextColor.color(255, 100, 100))));
			return;
		}

		CooldownManager cooldownManager = CooldownManager.getInstance();

		if (!cooldownManager.isCooldownOver(bPlayer, DOUBLE_JUMP_COOLDOWN_NAME)) {
			return;
		}

		player.setVelocity(player.getLocation().getDirection().multiply(1.5).setY(1));

		player.setAllowFlight(false);

		cooldownManager.setCooldown(bPlayer, DOUBLE_JUMP_COOLDOWN_NAME, DOUBLE_JUMP_COOLDOWN_MS);
	}

	private void playerDagger(BPlayer bPlayer) {
		CooldownManager cooldownManager = CooldownManager.getInstance();

		if (!cooldownManager.isCooldownOver(bPlayer, DAGGER_COOLDOWN_NAME)) {
			return;
		}

		Player player = bPlayer.getPlayer();
		if (player == null)
			return;
		Pair<Entity, Vector> target = BClassUtil.getTargetEntity(player, DAGGER_RANGE, 0.4);
		if (target == null)
			return;
		if (target.first() instanceof LivingEntity) {
			BClassUtil.playerDamagedEntity(bPlayer, (LivingEntity) target.first(), BDamageType.MAGIC, DAGGER_DAMAGE);
		}
		playerDaggerEffect(bPlayer, target.second());

		cooldownManager.setCooldown(bPlayer, DAGGER_COOLDOWN_NAME, DAGGER_COOLDOWN_MS);

	}

	// calculates the positions for origin and end of doDaggerParticleEffect, then
	// calls that function.
	private void playerDaggerEffect(BPlayer bPlayer, Vector hitLocation) {
		Player player = bPlayer.getPlayer();
		if (player == null)
			return;
		Location start = player.getEyeLocation();
		Color color = new Color(bPlayer.getTeam().getColor().getRed(), bPlayer.getTeam().getColor().getGreen(),
				bPlayer.getTeam().getColor().getBlue());
		XParticle.line(start, hitLocation.toLocation(player.getWorld()), 0.1,
				ParticleDisplay.colored(start, color, 0.5f));
	}

	// prevent fall damage.
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		if (BPlayer.getBPlayer((Player) event.getEntity()).getBClass() != this)
			return;

		if (event.getCause() == DamageCause.FALL) {
			event.setCancelled(true);
		}
	}

	@Override
	public void cooldownEnded(BPlayer bPlayer, TextComponent cooldownName) {
		if (cooldownName.equals(DOUBLE_JUMP_COOLDOWN_NAME)) {
			Player player = bPlayer.getPlayer();
			if (player == null)
				return;

			player.setAllowFlight(true);
		}
	}

	// called when an acrobat kills a player. used to reset acro's double jump
	// cooldown.
	@Override
	public void killedPlayer(BPlayer killer, BPlayer victim) {
		CooldownManager.getInstance().setCooldown(killer, DOUBLE_JUMP_COOLDOWN_NAME, 0);
		Player killerPlayer = killer.getPlayer();
		if (killerPlayer == null)
			return;
		killerPlayer.setAllowFlight(true);
	}

	@Override
	public void playerLeftThisClass(BPlayer bPlayer) {
		Player player = bPlayer.getPlayer();
		if (player == null)
			return;
		player.setAllowFlight(false);
	}

	@Override
	public void gameStartForClassPlayer(BPlayer bPlayer) {
		Player player = bPlayer.getPlayer();
		if (player == null)
			return;
		player.setAllowFlight(true);
	}

	@Override
	public void playerBecameThisClass(BPlayer bPlayer) {
		if (CooldownManager.getInstance().isCooldownOver(bPlayer, DOUBLE_JUMP_COOLDOWN_NAME)) {
			Player player = bPlayer.getPlayer();
			if (player == null)
				return;

			player.setAllowFlight(true);
		}
	}

}
