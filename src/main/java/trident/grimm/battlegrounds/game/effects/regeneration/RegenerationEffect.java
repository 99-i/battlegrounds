package trident.grimm.battlegrounds.game.effects.regeneration;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.game.effects.BEffect;
import trident.grimm.battlegrounds.game.effects.regeneration.RegenerationEffect.RegenerationEffectSettings;

public class RegenerationEffect extends BEffect<RegenerationEffectSettings> {

	public static record RegenerationEffectSettings(int mult) {
	}

	public RegenerationEffect(TextComponent displayName) {
		super(displayName);
	}

	@Override
	public void onStop(Player player, RegenerationEffectSettings settings) {
		playerEnd(player, settings);
	}

	@EventHandler
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {
		event.setCancelled(true);
	}

	@Override
	public boolean shouldReplaceSettings(RegenerationEffectSettings current, RegenerationEffectSettings candidate) {
		return candidate.mult > current.mult;
	}

}
