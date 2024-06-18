package trident.grimm.battlegrounds.game.effects.slowness;

import java.util.HashMap;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.BEffects;
import trident.grimm.battlegrounds.game.effects.slowness.SlownessEffect.SlownessEffectSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.MovementSpeedSource;
import trident.grimm.battlegrounds.game.players.MovementSpeedSource.MovementSpeedSourceType;

public class SlownessEffect extends BEffect<SlownessEffectSettings> {

	private HashMap<Player, MovementSpeedSource> slownessSources;

	public static record SlownessEffectSettings(BPlayer inflicter, int mult) {
	}

	public SlownessEffect(TextComponent displayName) {
		super(displayName);
		this.slownessSources = new HashMap<>();
	}

	@Override
	public void onStop(Player player, SlownessEffectSettings settings) {
		playerEnd(player, settings);
	}

	@Override
	public void playerEnd(Player player, SlownessEffectSettings settings) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		MovementSpeedSource source = this.slownessSources.get(player);
		if (source == null)
			return;
		bPlayer.removeMovementSpeedSource(source);

		this.slownessSources.remove(player);
	}

	@Override
	public void playerStart(Player player, SlownessEffectSettings settings) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		MovementSpeedSource source = bPlayer.addMovementSpeedSource(MovementSpeedSourceType.MULTIPLICATIVE,
				(((double) -settings.mult) / 100d));

		slownessSources.put(player, source);
	}

	@Override
	public boolean shouldReplaceSettings(SlownessEffectSettings current, SlownessEffectSettings candidate) {
		return candidate.mult > current.mult;
	}

}
