package trident.grimm.battlegrounds.items.lobby;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import trident.grimm.inventory.ElibGUI;
import trident.grimm.inventory.ElibView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.game.classes.BClass;
import trident.grimm.battlegrounds.game.classes.BClasses;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;

public class ClassLobbyItem extends LobbyItem {

	final static String CLASS_LOBBY_ITEM_IDENTIFIER = "class-lobby-item";
	final static TextComponent CLASS_LOBBY_ITEM_DISPLAY_NAME = Component.text("Pick a class!",
			TextColor.color(0, 0, 255));

	private ElibGUI gui;

	public ClassLobbyItem() {
		register(CLASS_LOBBY_ITEM_IDENTIFIER);
		gui = new ElibGUI();

		ElibView main = gui.addView("main", 9, Component.text("Pick a Class"));

		for (int i = 0; i < BClasses.values.length; i++) {
			BClass bClass = BClasses.values[i];
			ItemStack displayItemStack = new ItemStack(bClass.getDisplayMaterial());
			ItemUtil.setDisplayName(displayItemStack, bClass.getDisplayName());
			main.addButton(i, displayItemStack, (event) -> {
				BPlayer.getBPlayer(event.presser()).setClass(bClass, false);
				event.presser().getInventory().setItemInMainHand(getDefaultItem(event.presser()));
			});
		}
	}

	@Override
	public void playerOpenedThisInventory(Player player) {
		gui.setPlayerView(player, "main");
	}

	@Override
	ItemStack getDefaultItem(Player player) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		ItemStack itemStack = new ItemStack(bPlayer.getBClass().getDisplayMaterial());
		ItemUtil.addFlagToItem(itemStack, CLASS_LOBBY_ITEM_IDENTIFIER);
		ItemUtil.setDisplayName(itemStack, CLASS_LOBBY_ITEM_DISPLAY_NAME);

		return itemStack;

	}

}
