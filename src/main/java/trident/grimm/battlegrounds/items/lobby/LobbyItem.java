package trident.grimm.battlegrounds.items.lobby;

import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

//an interface for a lobby item (e.g. vote picker, team picker)
public abstract class LobbyItem {

	// the registry is used to register lobby items so that players get them when
	// they spawn in.
	public class LobbyItemRegistry {
		String identifier;
		LobbyItem lobbyItem;

		public LobbyItemRegistry(String identifier, LobbyItem lobbyItem) {
			this.identifier = identifier;
			this.lobbyItem = lobbyItem;
		}
	}

	static ArrayList<LobbyItemRegistry> lobbyItems = new ArrayList<>();

	// called by a LobbyItem implementation in its constructor. registers the
	// LobbyItem instance in the registry.
	void register(String identifier) {
		LobbyItemRegistry item = new LobbyItemRegistry(identifier, this);
		lobbyItems.add(item);
	}

	abstract void playerOpenedThisInventory(Player player);

	abstract ItemStack getDefaultItem(Player player);
}
