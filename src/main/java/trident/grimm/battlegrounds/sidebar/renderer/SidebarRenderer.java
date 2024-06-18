package trident.grimm.battlegrounds.sidebar.renderer;

import org.bukkit.entity.Player;

import trident.grimm.battlegrounds.sidebar.Sidebar;

public interface SidebarRenderer {

	void add(Player player, Sidebar sidebar);

	void remove(Player player, Sidebar sidebar);

	void render(Sidebar sidebar);

	void renderForPlayer(Player player, Sidebar sidebar);

}
