package trident.grimm.battlegrounds.game.effects.vampire_mark;

import java.awt.Color;

import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.vampire_mark.VampireMarkEffect.VampireMarkSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;

public class VampireMarkEffect extends BEffect<VampireMarkSettings> {

	public static record VampireMarkSettings(BPlayer markInflicter) {
	}

	public VampireMarkEffect(TextComponent displayName) {
		super(displayName);
	}

	@Override
	public void playerStart(Player player, VampireMarkSettings settings) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
	}

	@Override
	public void playerTick(Player player, VampireMarkSettings settings) {
		drawMarkLines(player);
	}

	private void drawMarkLines(Player player) {
		Location playerLocation = player.getLocation().add(new Vector(0, 1, 0));
		final double multX = 0.3d;
		final double multY = 0.3d;
		final double multZ = 0.3d;
		GaussianRandomGenerator gen = new GaussianRandomGenerator(new MersenneTwister());
		for (int i = 0; i < 2; i++) {

			Location p1 = playerLocation.clone().add(new Vector(gen.nextNormalizedDouble() * multX,
					gen.nextNormalizedDouble() * multY, gen.nextNormalizedDouble() * multZ));
			Location p2 = playerLocation.clone().add(new Vector(gen.nextNormalizedDouble() * multX,
					gen.nextNormalizedDouble() * multY, gen.nextNormalizedDouble() * multZ));
			XParticle.line(p1, p2, 0.2, ParticleDisplay.colored(p1, new Color(255, 255, 255), 0.7f));
		}
	}

	@Override
	public void onStop(Player player, VampireMarkSettings settings) {
		playerEnd(player, settings);
	}

	@Override
	public void playerEnd(Player player, VampireMarkSettings settings) {
		player.removePotionEffect(PotionEffectType.GLOWING);
	}

	@Override
	public boolean shouldReplaceSettings(VampireMarkSettings current, VampireMarkSettings candidate) {
		return false;
	}

}
