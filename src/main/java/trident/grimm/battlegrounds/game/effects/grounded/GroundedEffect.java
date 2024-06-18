package trident.grimm.battlegrounds.game.effects.grounded;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.grounded.GroundedEffect.GroundedEffectSettings;

public class GroundedEffect extends BEffect<GroundedEffectSettings> {

	public static record GroundedEffectSettings() {
	}

	public GroundedEffect(TextComponent displayName) {
		super(displayName);
	}

	@Override
	public void playerStart(Player player, GroundedEffectSettings settings) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 128));
	}

	@Override
	public void onStop(Player player, GroundedEffectSettings settings) {
		playerEnd(player, settings);
	}

	@Override
	public void playerEnd(Player player, GroundedEffectSettings settings) {
		player.removePotionEffect(PotionEffectType.JUMP_BOOST);
	}

	@Override
	public boolean shouldReplaceSettings(GroundedEffectSettings current, GroundedEffectSettings candidate) {
		return true;
	}

}
