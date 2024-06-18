package trident.grimm.battlegrounds.game.players;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import trident.grimm.inventory.ElibButton.ElibButtonPressType;
import trident.grimm.inventory.ElibGUI;
import trident.grimm.inventory.ElibView;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.items.ItemUtil;

public class BPlayerBackpack {

	public static final int BACKPACK_SIZE = 6;

	private @Getter BackpackItem[] items;
	private @Getter ElibGUI gui;
	private @Getter BPlayer bPlayer;
	private @Getter @Setter int emeralds = 0;

	public BPlayerBackpack(BPlayer bPlayer) {
		this.bPlayer = bPlayer;
		this.items = new BackpackItem[BACKPACK_SIZE];
		this.gui = new ElibGUI();
	}

	public void updateGUI() {
		ElibView mainView = gui.getView("main");
		if (mainView == null) {
			mainView = gui.addView("main", 9, Component.text("Backpack"));
		}

		final ElibView main = mainView;

		for (int i = 0; i < this.items.length; i++) {
			BackpackItem item = this.items[i];
			ItemStack itemStack = item == null ? App.getInstance().getBConfig().getItems().get("backpack_empty")
					: item.getDisplayItemStack().clone();
			final int slot = i;

			main.addButton(slot, itemStack, (event) -> {
				if (event.type() == ElibButtonPressType.Q) {
					sellItem(slot);
					main.setItem(slot, App.getInstance().getBConfig().getItems().get("backpack_empty"));
					main.setItem(8, new ItemStack(Material.EMERALD, emeralds));
				}
			});

		}

		main.setItem(8, new ItemStack(Material.EMERALD, emeralds));
		main.setPlaceholders(App.getInstance().getBConfig().getItems().get("placeholder"));

	}

	private void sellItem(int slot) {
		if (this.items[slot] == null)
			return;
		removeItemAtSlot(slot);
		// todo: sell price.
		setEmeralds(emeralds + 1);
	}

	public boolean addItem(ItemStack itemStack) {
		int firstOpenSlot = getFirstOpenSlot();
		if (firstOpenSlot == -1)
			return false;

		BackpackItem item = getItemByItemStack(itemStack);
		this.items[firstOpenSlot] = item;
		return true;
	}

	private int getFirstOpenSlot() {
		for (int i = 0; i < this.items.length; i++) {
			if (this.items[i] == null) {
				return i;
			}
		}
		return -1;
	}

	private static BackpackItem getItemByItemStack(ItemStack itemStack) {
		String itemId = ItemUtil.getItemId(itemStack);
		if (itemId == null)
			return null;
		BackpackItem item = null;
		for (BackpackItem backpackItem : BackpackItem.values()) {
			if (backpackItem.getItemId().equals(itemId)) {
				item = backpackItem;
			}
		}
		return item;
	}

	// returns whether there is an item at that slot.
	public boolean removeItemAtSlot(int slot) {
		boolean hadItem = this.items[slot] != null;
		this.items[slot] = null;
		return hadItem;
	}

}
