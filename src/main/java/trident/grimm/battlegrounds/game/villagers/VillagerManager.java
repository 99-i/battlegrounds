package trident.grimm.battlegrounds.game.villagers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import trident.grimm.inventory.ElibGUI;
import trident.grimm.inventory.ElibView;
import trident.grimm.inventory.ElibButton.ElibButtonPressType;
import lombok.Getter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.entity.Entity;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.BChat;
import trident.grimm.battlegrounds.config.BConfig.Shops.ShopProduct;
import trident.grimm.battlegrounds.config.BConfig.Vector;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.BackpackItem;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.util.WorldManager;

public class VillagerManager implements Listener {

	private static @Getter VillagerManager instance = new VillagerManager();

	private App app;

	private ElibGUI gui;
	private NPCRegistry npcRegistry;
	public static final String SHOP_COST_IDENTIFIER = "shop-cost";
	private static final String SHOP_PRODUCT_SHOP_ID = "shop-product-shop-name";

	public VillagerManager() {
		this.app = App.getInstance();
		this.npcRegistry = CitizensAPI.createInMemoryNPCRegistry("VILLAGERS");
		this.gui = new ElibGUI();
		this.setupGui();
	}

	private void setupGui() {
		this.gui.addView("main", 9, Component.text("SHOP"));
		HashMap<String, ArrayList<ShopProduct>> shops = app.getBConfig().getShopInfo().getShops();
		int i = 0;
		for (Map.Entry<String, ArrayList<ShopProduct>> shop : shops.entrySet()) {
			ItemStack display = app.getBConfig().getItems()
					.get(app.getBConfig().getShopInfo().getMeta().get(shop.getKey())).clone();

			this.gui.getView("main").addButton(i, display, (event) -> {
				if (event.type() == ElibButtonPressType.LEFT) {
					this.gui.setPlayerView(event.presser(), "shop-" + shop.getKey());
				}
			});

			ElibView shopView = this.gui.addView("shop-" + shop.getKey(), 54,
					BChat.getMiniMessage().deserialize(shop.getKey()));
			shopView.addButton(0, app.getBConfig().getItems().get("shop_go_back"), (event) -> {
				if (event.type() == ElibButtonPressType.LEFT) {
					gui.setPlayerView(event.presser(), "main");
				}
			});
			for (ShopProduct product : shop.getValue()) {
				ItemStack productItemStack = app.getBConfig().getItems().get(product.getProduct()).clone();
				ItemUtil.setItemString(productItemStack, SHOP_COST_IDENTIFIER, Integer.toString(product.getCost()));
				ItemUtil.setItemString(productItemStack, SHOP_PRODUCT_SHOP_ID, shop.getKey());
				if (EnchantmentTarget.ARMOR.includes(productItemStack)) {
					ItemUtil.prepareArmor(productItemStack);
				} else if (EnchantmentTarget.WEAPON.includes(productItemStack)) {
					ItemUtil.prepareWeapon(productItemStack);
				}

				ItemUtil.addLore(productItemStack, Component.text("--------------", TextColor.color(255, 255, 255)));

				ItemUtil.addLoreData(productItemStack, Component.text("Cost"),
						Component.text(product.getCost() + " Emeralds"));

				shopView.addButton(product.getSlot(), productItemStack, (event) -> {
					if (event.type() == ElibButtonPressType.LEFT) {
						playerTriedBuy(event.presser(), product.getSlot(), shop.getKey());
					}
				});
			}

			i++;
		}
	}

	// spawn all the villagers.
	public void spawnVillagers() {
		ArrayList<Vector> villagerLocations = this.app.getBConfig().getVillagers();

		for (Vector location : villagerLocations) {
			ShopVillagerEntity entity = new ShopVillagerEntity(WorldManager.getGameLocation(location),
					this.npcRegistry);
			entity.spawn();
		}
	}

	@EventHandler
	public void onNPCRightClick(NPCRightClickEvent event) {
		if (event.getNPC().getEntity().getType() == EntityType.WANDERING_TRADER) {
			event.setCancelled(true);
			this.openMainInventory(event.getClicker());
		}
	}

	private void playerTriedBuy(Player player, int slot, String shopName) {
		ArrayList<ShopProduct> shop = null;
		for (Map.Entry<String, ArrayList<ShopProduct>> entries : app.getBConfig().getShopInfo().getShops().entrySet()) {
			if (entries.getKey().equals(shopName)) {
				shop = entries.getValue();
			}
		}
		if (shop == null)
			return;

		ShopProduct product = null;

		for (ShopProduct p : shop) {
			if (p.getSlot() == slot) {
				product = p;
			}
		}
		if (product == null)
			return;

		BPlayer bPlayer = BPlayer.getBPlayer(player);
		if (hasEnough(player, product.getCostMaterial(), product.getCost())) {

			if (ItemUtil.itemHasFlag(app.getBConfig().getItems().get(product.getProduct()), "item-id")) {
				boolean duplicate = false;
				String itemId = ItemUtil.getItemString(app.getBConfig().getItems().get(product.getProduct()),
						"item-id");
				BackpackItem[] items = bPlayer.getBackpack().getItems();
				for (int i = 0; i < items.length; i++) {
					if (items[i] != null)
						if (items[i].getItemId().equals(itemId)) {
							duplicate = true;
							break;
						}
				}

				if (duplicate) {
					player.sendMessage(Component.text("You already have this item!", TextColor.color(255, 120, 120)));
				} else if (!bPlayer.getBackpack().addItem(app.getBConfig().getItems().get(product.getProduct()))) {
					player.sendMessage(Component.text("Your backpack is full!", TextColor.color(255, 120, 120)));
				} else {
					removeMaterialFromInventory(player, product.getCostMaterial(), product.getCost());
					player.sendMessage(
							Component.text("The item was added to your backpack.", TextColor.color(0, 255, 0)));
				}
				return;
			}

			ItemStack reward = app.getBConfig().getItems().get(product.getProduct()).clone();
			if (EnchantmentTarget.ARMOR.includes(reward)) {
				ItemUtil.prepareArmor(reward, false);
			} else if (EnchantmentTarget.WEAPON.includes(reward)) {
				ItemUtil.prepareWeapon(reward, false);
			}
			player.getInventory().addItem(reward);
			removeMaterialFromInventory(player, product.getCostMaterial(), product.getCost());
		} else {
			player.sendMessage(Component.text("You do not have enough emeralds to buy this item!",
					TextColor.color(255, 120, 120)));
		}

	}

	private void openMainInventory(Player player) {
		this.gui.setPlayerView(player, "main");
	}

	// if the player has at least <count> of <material> in his inventory.
	private boolean hasEnough(Player player, Material material, int count) {

		if (material == Material.EMERALD) {
			BPlayer bPlayer = BPlayer.getBPlayer(player);
			return bPlayer.getBackpack().getEmeralds() >= count;
		}

		PlayerInventory playerInventory = player.getInventory();
		int currentCount = 0;

		ItemStack[] contents = playerInventory.getContents();

		for (ItemStack stack : contents) {
			if (stack == null)
				continue;
			if (stack.getType() == material) {
				currentCount += stack.getAmount();
			}
		}

		return currentCount >= count;
	}

	// remove <count> of <material> from the player's inventory.
	private void removeMaterialFromInventory(Player player, Material material, int count) {

		if (material == Material.EMERALD) {
			BPlayer bPlayer = BPlayer.getBPlayer(player);
			bPlayer.getBackpack().setEmeralds(bPlayer.getBackpack().getEmeralds() - count);
			return;
		}
		PlayerInventory playerInventory = player.getInventory();
		int subtracted = 0;
		ItemStack[] contents = playerInventory.getContents();

		for (ItemStack stack : contents) {
			if (stack == null)
				continue;

			if (stack.getType() == material) {
				int amount = stack.getAmount();
				for (int i = 0; i < amount; i++) {
					stack.setAmount(stack.getAmount() - 1);
					subtracted += 1;
					if (subtracted == count) {
						break;
					}
				}
				if (subtracted == count) {
					break;
				}
			}
		}

	}
}
