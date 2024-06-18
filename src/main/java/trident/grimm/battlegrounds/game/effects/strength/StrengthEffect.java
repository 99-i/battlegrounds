package trident.grimm.battlegrounds.game.effects.strength;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.strength.StrengthEffect.StrengthEffectSettings;

public class StrengthEffect extends BEffect<StrengthEffectSettings> {

	public static record StrengthEffectSettings(int mult) {
	}

	public StrengthEffect(TextComponent displayName) {
		super(displayName);
	}

	@Override
	public boolean shouldReplaceSettings(StrengthEffectSettings current, StrengthEffectSettings candidate) {
		return candidate.mult() > current.mult();
	}

}
