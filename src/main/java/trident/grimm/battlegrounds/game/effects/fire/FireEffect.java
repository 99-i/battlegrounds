package trident.grimm.battlegrounds.game.effects.fire;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.fire.FireEffect.FireEffectSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.BPlayer.BDamageType;

public class FireEffect extends BEffect<FireEffectSettings> {

	public static record FireEffectSettings(BPlayer inflicter, int mult) {
	}

	public FireEffect(TextComponent displayName) {
		super(displayName);
	}

	@Override
	public void playerEnd(Player player, FireEffectSettings settings) {
		player.setFireTicks(0);
	}

	@Override
	public void playerTick(Player player, FireEffectSettings settings) {
		player.setFireTicks(100);
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		bPlayer.damage((double) settings.mult, settings.inflicter, BDamageType.PHYSICAL);
	}

	@Override
	public boolean shouldReplaceSettings(FireEffectSettings current, FireEffectSettings candidate) {
		return candidate.mult > current.mult;
	}
}
