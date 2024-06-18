package trident.grimm.battlegrounds.game.effects.subdued;

import java.awt.Color;
import java.util.HashMap;

import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.subdued.SubduedEffect.SubduedEffectSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.MovementSpeedSource;
import trident.grimm.battlegrounds.game.players.MovementSpeedSource.MovementSpeedSourceType;

public class SubduedEffect extends BEffect<SubduedEffectSettings> {

	private HashMap<Player, Location> originalLocations = new HashMap<>();
	private HashMap<Player, MovementSpeedSource> subductionSources;

	public static record SubduedEffectSettings() {
	}

	public SubduedEffect(TextComponent displayName) {
		super(displayName);
		this.subductionSources = new HashMap<>();
	}

	@Override
	public void playerStart(Player player, SubduedEffectSettings settings) {
		originalLocations.put(player, player.getLocation());

		BPlayer bPlayer = BPlayer.getBPlayer(player);
		MovementSpeedSource source = bPlayer.addMovementSpeedSource(MovementSpeedSourceType.STOP, 0);

		subductionSources.put(player, source);

		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 200));
	}

	@Override
	public void onStop(Player player, SubduedEffectSettings settings) {
		playerEnd(player, settings);
	}

	@Override
	public void playerEnd(Player player, SubduedEffectSettings settings) {
		playerTick(player, settings);
		player.removePotionEffect(PotionEffectType.JUMP_BOOST);
		originalLocations.remove(player);

		BPlayer bPlayer = BPlayer.getBPlayer(player);
		MovementSpeedSource source = this.subductionSources.get(player);
		if (source == null)
			return;
		bPlayer.removeMovementSpeedSource(source);

		this.subductionSources.remove(player);
	}

	@Override
	public void playerTick(Player player, SubduedEffectSettings settings) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		drawSubductionLines(player, 3);
	}

	private void drawSubductionLines(Player player, int count) {
		Location playerLocation = player.getLocation().add(new Vector(0, 1, 0));
		final double multX = 0.6d;
		final double multY = 0.6d;
		final double multZ = 0.6d;
		GaussianRandomGenerator gen = new GaussianRandomGenerator(new MersenneTwister());
		for (int i = 0; i < count; i++) {

			Location p1 = playerLocation.clone().add(new Vector(gen.nextNormalizedDouble() * multX,
					gen.nextNormalizedDouble() * multY, gen.nextNormalizedDouble() * multZ));
			Location p2 = playerLocation.clone().add(new Vector(gen.nextNormalizedDouble() * multX,
					gen.nextNormalizedDouble() * multY, gen.nextNormalizedDouble() * multZ));
			XParticle.line(p1, p2, 0.2, ParticleDisplay.colored(p1, new Color(0, 0, 0), 0.7f));
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (originalLocations.keySet().contains(player)) {
			Location originalLocation = originalLocations.get(player);
			if (player.getLocation().distance(originalLocation) > 0.3) {
				Location newLocation = originalLocation.clone();
				newLocation.setYaw(player.getLocation().getYaw());
				newLocation.setPitch(player.getLocation().getPitch());
				player.teleport(newLocation);
			}
		}
	}
}
