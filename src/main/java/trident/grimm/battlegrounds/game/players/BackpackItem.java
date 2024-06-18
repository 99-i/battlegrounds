package trident.grimm.battlegrounds.game.players;

import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.items.ItemUtil;

//todo: config backpack items.
public enum BackpackItem {
	ENCHANTING_CHARM("enchanting_charm"),
	DRAGONS_TOOTH("dragons_tooth"),
	MAGIC_WAND("magic_wand");

	// stored in nbt tag 'item-id'
	private @Getter String itemId;
	private @Getter ItemStack itemStack;

	private @Getter ItemStack displayItemStack;

	BackpackItem(String configItemStack) {
		this.itemId = this.name().toLowerCase();
		this.itemStack = App.getInstance().getBConfig().getItems().get(configItemStack);
		ItemUtil.setItemString(itemStack, "item-id", this.itemId);

		this.displayItemStack = this.itemStack.clone();
		ItemUtil.addLore(displayItemStack, Component.text("Press Q to sell", TextColor.color(255, 255, 255)));
	}

}
