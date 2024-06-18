package trident.grimm.battlegrounds.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.BConfig;
import trident.grimm.battlegrounds.config.BConfig.ArmorData;

// helper class for working with persistent data containers.
public class ItemUtil {

	// flags are just boolean values
	// internally, they are just strings with "true", but their presence
	// marks the boolean as true and vice versa
	public static void addFlagToItem(ItemStack itemStack, String flag) {
		setItemString(itemStack, flag, "true");
	}

	public static void clearLore(ItemStack itemStack) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.lore(new ArrayList<>());
	}

	public static boolean itemHasFlag(ItemStack itemStack, String flag) {
		return getItemString(itemStack, flag) != null;
	}

	public static boolean itemIsSoulbound(ItemStack itemStack) {
		return itemHasFlag(itemStack, "Soulbound");
	}

	public static void makeItemSoulbound(ItemStack itemStack) {
		addFlagToItem(itemStack, "Soulbound");
	}

	public static boolean itemIsUndroppable(ItemStack itemStack) {
		return itemHasFlag(itemStack, "undroppable");
	}

	public static void makeItemUndroppable(ItemStack itemStack) {
		addFlagToItem(itemStack, "undroppable");
	}

	public static boolean itemIsBackpack(ItemStack itemStack) {
		return itemHasFlag(itemStack, "backpack");
	}

	private static void setBackpackFlag(ItemStack itemStack) {
		ItemUtil.addFlagToItem(itemStack, "backpack");
	}

	public static void setLeatherArmorColor(ItemStack armor, Color color) {
		LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
		meta.setColor(color);
		armor.setItemMeta(meta);
	}

	public static void addLore(ItemStack itemStack, TextComponent lore) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta.hasLore()) {
			List<Component> existingLore = itemMeta.lore();
			existingLore.add(
					Component.text().decorationIfAbsent(TextDecoration.ITALIC, State.FALSE).append(lore).build());
			itemMeta.lore(existingLore);

		} else {
			itemMeta.lore(Arrays.asList(
					Component.text().decorationIfAbsent(TextDecoration.ITALIC, State.FALSE).append(lore).build()));
		}

		itemStack.setItemMeta(itemMeta);
	}

	public static void addLoreData(ItemStack itemStack, TextComponent id, TextComponent value) {
		addLore(itemStack, Component.text()
				.append(id)
				.append(Component.text(": "))
				.append(value)
				.build());
	}

	public static void removeLoreData(ItemStack itemStack, String id) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta.hasLore()) {
			List<Component> existingLore = itemMeta.lore();
			for (int i = 0; i < existingLore.size(); i++) {
				TextComponent lore = (TextComponent) existingLore.get(i);
				String plainText = PlainTextComponentSerializer.plainText().serialize(lore);
				if (plainText.startsWith(id)) {
					existingLore.remove(lore);
					i--;
				}
			}

			itemMeta.lore(existingLore);
		}

		itemStack.setItemMeta(itemMeta);
	}

	public static void setWeaponLore(ItemStack item) {
		BConfig config = App.getInstance().getBConfig();
		HashMap<Material, Double> map = config.getSwordDamages();
		double dmg = config.getDefaultDamage();
		if (map.containsKey(item.getType())) {
			dmg = map.get(item.getType());
		}
		addLoreData(item, Component.text("Damage", NamedTextColor.DARK_PURPLE),
				Component.text(Double.toString(dmg), NamedTextColor.YELLOW));
	}

	public static void setArmorLore(ItemStack armor) {
		Material material = armor.getType();
		HashMap<Material, ArmorData> armorData = App.getInstance().getBConfig().getArmorAttributes();
		addLoreData(armor, Component.text("Armor", NamedTextColor.DARK_PURPLE),
				Component.text(Double.toString(armorData.get(material).getArmor()), NamedTextColor.YELLOW));
		addLoreData(armor, Component.text("Magic Resistance", NamedTextColor.DARK_PURPLE),
				Component.text(Double.toString(armorData.get(material).getMagicResistance()), NamedTextColor.YELLOW));
	}

	// set the item's nbt string to the value
	public static void setItemString(ItemStack itemStack, String name, String value) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta == null)
			return;

		PersistentDataContainer container = itemMeta.getPersistentDataContainer();
		NamespacedKey key = new NamespacedKey(App.getInstance(), name);
		container.set(key, PersistentDataType.STRING, value);

		itemStack.setItemMeta(itemMeta);
	}

	// get the value of the nbt string in the item
	public static String getItemString(ItemStack itemStack, String name) {
		if (itemStack == null)
			return null;
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta == null)
			return null;

		PersistentDataContainer container = itemMeta.getPersistentDataContainer();
		NamespacedKey key = new NamespacedKey(App.getInstance(), name);
		return container.get(key, PersistentDataType.STRING);
	}

	public static String getItemId(ItemStack itemStack) {
		return getItemString(itemStack, "item-id");
	}

	public static void setDisplayName(ItemStack itemStack, TextComponent displayName) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.displayName(
				Component.text().decorationIfAbsent(TextDecoration.ITALIC, State.FALSE).append(displayName).build());
		itemStack.setItemMeta(itemMeta);
	}

	public static void disableAttributes(ItemStack itemStack) {
		if (itemStack == null)
			return;
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_UNBREAKABLE);
		itemStack.setItemMeta(itemMeta);
	}

	@SuppressWarnings("deprecation")
	public static void setRecipes() {
		BConfig config = App.getInstance().getBConfig();
		Set<Material> weaponMaterials = config.getSwordDamages().keySet();
		Set<Material> armorMaterials = config.getArmorAttributes().keySet();
		Set<String> usedKeys = new HashSet<>();

		for (Iterator<Recipe> iterator = Bukkit.recipeIterator(); iterator.hasNext();) {
			Recipe recipe = iterator.next();
			if (recipe instanceof ShapedRecipe) {
				String key = "b-recipe-key-" + recipe.getResult().getType().name();
				if (usedKeys.contains(key)) {
					iterator.remove();
					continue;
				}
				usedKeys.add(key);
				NamespacedKey bRecipeKey = new NamespacedKey(App.getInstance(), key);
				if (weaponMaterials.contains(recipe.getResult().getType())) {
					ItemStack newItem = new ItemStack(recipe.getResult().getType());
					ItemUtil.setWeaponLore(newItem);
					ItemUtil.disableAttributes(newItem);

					ShapedRecipe newRecipe = new ShapedRecipe(bRecipeKey, newItem);
					ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

					newRecipe.shape(shapedRecipe.getShape());
					for (Entry<Character, ItemStack> entry : shapedRecipe.getIngredientMap().entrySet()) {
						newRecipe.setIngredient(entry.getKey(), entry.getValue().getType());
					}
					iterator.remove();
					Bukkit.addRecipe(newRecipe);

				} else if (armorMaterials.contains(recipe.getResult().getType())) {
					ItemStack newItem = new ItemStack(recipe.getResult().getType());
					ItemUtil.setArmorLore(newItem);
					ItemUtil.disableAttributes(newItem);

					ShapedRecipe newRecipe = new ShapedRecipe(bRecipeKey, newItem);
					ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

					newRecipe.shape(shapedRecipe.getShape());
					for (Entry<Character, ItemStack> entry : shapedRecipe.getIngredientMap().entrySet()) {
						ItemStack v = entry.getValue();
						if (v != null) {
							newRecipe.setIngredient(entry.getKey(), v.getType());
						}
					}
					iterator.remove();
					Bukkit.addRecipe(newRecipe);
				}
			}
		}
	}

	public static void setValue(PersistentDataContainer container, String name, String value) {
		NamespacedKey key = new NamespacedKey(App.getInstance(), name);

		container.set(key, PersistentDataType.STRING, value);
	}

	public static String getValue(PersistentDataContainer container, String name) {
		NamespacedKey key = new NamespacedKey(App.getInstance(), name);

		return container.get(key, PersistentDataType.STRING);
	}

	public static void prepareWeapon(ItemStack weapon) {
		prepareWeapon(weapon, true);
	}

	public static void prepareWeapon(ItemStack weapon, boolean soulbound) {
		disableAttributes(weapon);
		if (soulbound) {
			makeItemSoulbound(weapon);
		}
		setWeaponLore(weapon);
	}

	public static void prepareArmor(ItemStack armor) {
		prepareArmor(armor, true);
	}

	public static void prepareArmor(ItemStack armor, boolean soulbound) {
		ItemUtil.disableAttributes(armor);
		if (soulbound) {
			ItemUtil.makeItemSoulbound(armor);
		}
		ItemUtil.setArmorLore(armor);
	}

	public static void prepareLeatherArmor(ItemStack armor, Color color) {
		ItemUtil.disableAttributes(armor);
		ItemUtil.makeItemSoulbound(armor);
		ItemUtil.setLeatherArmorColor(armor, color);
		ItemUtil.setArmorLore(armor);
	}

	// todo: hardcoded backpack item.
	public static ItemStack getBackpackItem() {
		ItemStack itemStack = new ItemStack(Material.CHEST);
		ItemUtil.setDisplayName(itemStack, Component.text("Backpack"));
		ItemUtil.makeItemUndroppable(itemStack);
		ItemUtil.makeItemSoulbound(itemStack);
		setBackpackFlag(itemStack);
		return itemStack;
	}

}
