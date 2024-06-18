package trident.grimm.battlegrounds.items.lobby;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import trident.grimm.inventory.ElibGUI;
import trident.grimm.inventory.ElibView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.game.GameManager;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.sidebar.SidebarManager;

public class TeamLobbyItem extends LobbyItem {
	final static String TEAM_LOBBY_ITEM_IDENTIFIER = "team-lobby-item";
	final static TextComponent TEAM_LOBBY_ITEM_DISPLAY_NAME = Component.text("Pick a team!",
			TextColor.color(0, 255, 0));
	final static String TEAM_LOBBY_ITEM_SELECTOR = "team-team";

	private ElibGUI gui;

	public TeamLobbyItem() {
		register(TEAM_LOBBY_ITEM_IDENTIFIER);

		gui = new ElibGUI();

		ElibView main = gui.addView("main", 9, Component.text("Pick a Team"));

		int i = 0;
		for (BTeam bTeam : BTeam.values()) {
			ItemStack displayItemStack = new ItemStack(bTeam.getDisplayMaterial());
			ItemUtil.setDisplayName(displayItemStack, bTeam.getDisplayName());

			main.addButton(i, displayItemStack, (event) -> {
				BPlayer.getBPlayer(event.presser()).joinTeam(bTeam);
				if (!GameManager.getInstance().isStarted()) {
					event.presser().getInventory().setItemInMainHand(getDefaultItem(event.presser()));
				}
				SidebarManager.getInstance().update();
			});

			i++;
		}
	}

	@Override
	public void playerOpenedThisInventory(Player player) {
		gui.setPlayerView(player, "main");
	}

	@Override
	ItemStack getDefaultItem(Player player) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		ItemStack itemStack = new ItemStack(Material.WHITE_DYE);
		if (bPlayer.isInTeam()) {
			itemStack.setType(bPlayer.getTeam().getDisplayMaterial());
		}

		ItemUtil.addFlagToItem(itemStack, TEAM_LOBBY_ITEM_IDENTIFIER);
		ItemUtil.setDisplayName(itemStack, TEAM_LOBBY_ITEM_DISPLAY_NAME);
		return itemStack;
	}

}
