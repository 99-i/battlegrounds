package trident.grimm.battlegrounds.game.effects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.game.effects.fire.FireEffect;
import trident.grimm.battlegrounds.game.effects.fire.FireEffect.FireEffectSettings;
import trident.grimm.battlegrounds.game.effects.grounded.GroundedEffect;
import trident.grimm.battlegrounds.game.effects.grounded.GroundedEffect.GroundedEffectSettings;
import trident.grimm.battlegrounds.game.effects.regeneration.RegenerationEffect;
import trident.grimm.battlegrounds.game.effects.regeneration.RegenerationEffect.RegenerationEffectSettings;
import trident.grimm.battlegrounds.game.effects.slowness.SlownessEffect;
import trident.grimm.battlegrounds.game.effects.slowness.SlownessEffect.SlownessEffectSettings;
import trident.grimm.battlegrounds.game.effects.speed.SpeedEffect;
import trident.grimm.battlegrounds.game.effects.speed.SpeedEffect.SpeedEffectSettings;
import trident.grimm.battlegrounds.game.effects.stealth.StealthEffect;
import trident.grimm.battlegrounds.game.effects.stealth.StealthEffect.StealthEffectSettings;
import trident.grimm.battlegrounds.game.effects.strength.StrengthEffect;
import trident.grimm.battlegrounds.game.effects.strength.StrengthEffect.StrengthEffectSettings;
import trident.grimm.battlegrounds.game.effects.subdued.SubduedEffect;
import trident.grimm.battlegrounds.game.effects.subdued.SubduedEffect.SubduedEffectSettings;
import trident.grimm.battlegrounds.game.effects.vampire_mark.VampireMarkEffect;
import trident.grimm.battlegrounds.game.effects.vampire_mark.VampireMarkEffect.VampireMarkSettings;
import trident.grimm.battlegrounds.game.effects.vulnerability.VulnerabilityEffect;
import trident.grimm.battlegrounds.game.effects.vulnerability.VulnerabilityEffect.VulnerabilityEffectSettings;

public class BEffects {
	// todo: put this into a config.
	public static final BEffect<GroundedEffectSettings> GROUNDED = new GroundedEffect(
			Component.text()
					.color(TextColor.color(255, 0, 0))
					.append(Component.text("GROUNDED")).build());
	public static final BEffect<FireEffectSettings> FIRE = new FireEffect(
			Component.text()
					.color(TextColor.color(255, 0, 0))
					.append(Component.text("ON FIRE")).build());
	public static final BEffect<SlownessEffectSettings> SLOWNESS = new SlownessEffect(
			Component.text()
					.color(TextColor.color(255, 0, 0))
					.append(Component.text("SLOWED")).build());
	public static final BEffect<VulnerabilityEffectSettings> VULNERABILITY = new VulnerabilityEffect(
			Component.text()
					.color(TextColor.color(255, 0, 0))
					.append(Component.text("VULNERABLE")).build());
	public static final BEffect<RegenerationEffectSettings> REGENERATION = new RegenerationEffect(
			Component.text()
					.color(TextColor.color(2, 48, 32))
					.append(Component.text("REGENERATION")).build());
	public static final BEffect<StrengthEffectSettings> STRENGTH = new StrengthEffect(
			Component.text()
					.color(TextColor.color(101, 112, 3))
					.append(Component.text("STRENGTH")).build());
	public static final BEffect<SpeedEffectSettings> SPEED = new SpeedEffect(
			Component.text()
					.color(TextColor.color(11, 79, 9))
					.append(Component.text("SPEED")).build());
	public static final BEffect<VampireMarkSettings> VAMPIRE_MARK = new VampireMarkEffect(
			Component.text()
					.color(TextColor.color(77, 17, 2))
					.append(Component.text("MARKED")).build());
	public static final BEffect<StealthEffectSettings> STEALTH = new StealthEffect(
			Component.text()
					.color(TextColor.color(127, 127, 127))
					.append(Component.text("STEALTHY")).build());
	public static final BEffect<SubduedEffectSettings> SUBDUED = new SubduedEffect(
			Component.text()
					.color(TextColor.color(168, 42, 10))
					.append(Component.text("SUBDUED")).build());

	public static BEffect<?>[] values = { GROUNDED, FIRE, SLOWNESS, VULNERABILITY, REGENERATION, STRENGTH, SPEED,
			VAMPIRE_MARK, STEALTH, SUBDUED };

}
