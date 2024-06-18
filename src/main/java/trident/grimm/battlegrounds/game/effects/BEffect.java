package trident.grimm.battlegrounds.game.effects;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import lombok.Getter;
import net.kyori.adventure.text.TextComponent;

public abstract class BEffect<T> implements Listener {
	private TextComponent displayName;
	protected @Getter Set<Player> players = new HashSet<Player>();

	public BEffect(TextComponent displayName) {
		this.displayName = displayName;
	}

	public TextComponent getDisplayName() {
		return this.displayName;
	}

	// called when a player first gets this effect
	public void playerStart(Player player, T settings) {
	}

	// called when the player or something else (e.g. milk) forcibly stops this
	// effect (not from naturally running out)
	public void onStop(Player player, T settings) {
	}

	// called when the duration of the effect ends and the effect wasn't forcibly
	// removed.
	public void playerEnd(Player player, T settings) {
	}

	// called every tick that a player has this effect
	public void playerTick(Player player, T settings) {
	}

	public boolean shouldReplaceSettings(T current, T candidate) {
		return true;
	}
}
