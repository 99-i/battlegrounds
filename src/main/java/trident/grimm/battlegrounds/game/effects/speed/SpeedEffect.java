package trident.grimm.battlegrounds.game.effects.speed;

import java.util.HashMap;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.speed.SpeedEffect.SpeedEffectSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.MovementSpeedSource;
import trident.grimm.battlegrounds.game.players.MovementSpeedSource.MovementSpeedSourceType;

public class SpeedEffect extends BEffect<SpeedEffectSettings> {

	private HashMap<Player, MovementSpeedSource> speedSources;

	public static record SpeedEffectSettings(int mult) {
	}

	public SpeedEffect(TextComponent displayName) {
		super(displayName);
		this.speedSources = new HashMap<>();
	}

	@Override
	public void onStop(Player player, SpeedEffectSettings settings) {
		playerEnd(player, settings);
	}

	@Override
	public void playerEnd(Player player, SpeedEffectSettings settings) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		MovementSpeedSource source = this.speedSources.get(player);
		if (source == null)
			return;
		bPlayer.removeMovementSpeedSource(source);
		this.speedSources.remove(player);
	}

	@Override
	public void playerStart(Player player, SpeedEffectSettings settings) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		MovementSpeedSource source = bPlayer.addMovementSpeedSource(MovementSpeedSourceType.MULTIPLICATIVE,
				(((double) settings.mult) / 100d));

		speedSources.put(player, source);
	}

	@Override
	public boolean shouldReplaceSettings(SpeedEffectSettings current, SpeedEffectSettings candidate) {
		return candidate.mult() >= current.mult();
	}

}
