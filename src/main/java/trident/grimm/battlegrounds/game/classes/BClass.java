package trident.grimm.battlegrounds.game.classes;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.BConfig;
import trident.grimm.battlegrounds.game.ability.Ability;
import trident.grimm.battlegrounds.game.ability.Ability.AbilityType;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;

// abstract class that the class implementations extend.
// this class also contains static objects of each class (makeshift enum
// basically)
public abstract class BClass implements Listener {

	// the material shown in the class picker inventory.
	private @Getter Material displayMaterial;
	// the display name of the class (used everywhere).
	private @Getter TextComponent displayName;

	// the default max health of players using this class.
	protected @Getter double maxHealth;
	// the default armor of players using this class.
	protected @Getter double armor;
	// the default magic resistance of players using this class.
	protected @Getter double magicResistance;
	// the default walk speed of players using this class.
	protected @Getter double walkSpeed;
	// the default health regen of players using this class.
	protected @Getter double healthRegen;

	protected @Getter ArrayList<ItemStack> items;

	public BClass(Material displayMaterial, TextComponent displayName, String configSection) {
		this.displayMaterial = displayMaterial;
		this.displayName = displayName;

		// find these fields in the config.
		BConfig config = App.getInstance().getBConfig();
		this.maxHealth = config.getClassEntries().get(configSection).getMaxHealth();
		this.armor = config.getClassEntries().get(configSection).getArmor();
		this.magicResistance = config.getClassEntries().get(configSection).getMagicResistance();
		this.walkSpeed = config.getClassEntries().get(configSection).getWalkSpeed();
		this.healthRegen = config.getClassEntries().get(configSection).getHealthRegen();

		this.items = new ArrayList<ItemStack>(
				config.getClassEntries().get(configSection).getItems().stream().map((str) -> {
					return config.getItems().get(str);
				}).collect(Collectors.toList()));
	}

	private ArrayList<ItemStack> getItemsForPlayer(BPlayer bPlayer) {
		return new ArrayList<ItemStack>(items.stream().map((itemStack) -> {
			ItemStack newItemStack = itemStack.clone();
			if (EnchantmentTarget.ARMOR.includes(itemStack.getType())) {
				if (itemStack.getType().name().startsWith("LEATHER_")) {
					ItemUtil.prepareLeatherArmor(newItemStack, bPlayer.getTeam().getColor());
				} else {
					ItemUtil.prepareArmor(newItemStack);
				}
			} else if (EnchantmentTarget.WEAPON.includes(itemStack.getType())) {
				ItemUtil.prepareWeapon(newItemStack);
			}
			ItemUtil.disableAttributes(newItemStack);
			ItemUtil.makeItemSoulbound(newItemStack);
			return newItemStack;
		}).collect(Collectors.toList()));
	}

	// called when a player first spawns in. up to imp.
	public void setStarterInventory(BPlayer bPlayer) {
		Player player = bPlayer.getPlayer();
		if (player == null)
			return;
		ArrayList<ItemStack> itemStacks = getItemsForPlayer(bPlayer);

		for (ItemStack itemStack : itemStacks) {
			if (EnchantmentTarget.ARMOR.includes(itemStack.getType())) {
				String name = itemStack.getType().name();
				if (name.endsWith("_HELMET")) {
					player.getInventory().setHelmet(itemStack);
				} else if (name.endsWith("_CHESTPLATE")) {
					player.getInventory().setChestplate(itemStack);
				} else if (name.endsWith("_LEGGINGS")) {
					player.getInventory().setLeggings(itemStack);
				} else if (name.endsWith("_BOOTS")) {
					player.getInventory().setBoots(itemStack);
				}
			} else {
				player.getInventory().addItem(itemStack);
			}
		}
	}

	// called when a player changes to this class during the game. this is the
	// inventory that gets showed.
	public ArrayList<ItemStack> getChangeInventory(BPlayer bPlayer) {
		return getItemsForPlayer(bPlayer);
	}

	// internal array of abilities that get added to by the register[Type]Ability()
	// methods.
	private ArrayList<Ability> abilities = new ArrayList<>();

	public ArrayList<Ability> getAbilities() {
		return this.abilities;
	}

	// registers an item ability that is called whenever an item with the ability-id
	// nbt flag is right clicked.
	protected void registerItemAbility(String abilityId) {
		Ability ability = new Ability(AbilityType.ITEM, abilityId);
		abilities.add(ability);
	}

	// register f key ability
	protected void registerFKeyAbility() {
		Ability ability = new Ability(AbilityType.F_KEY);
		abilities.add(ability);
	}

	// register double jump ability
	protected void registerDoubleJumpAbility() {
		Ability ability = new Ability(AbilityType.DOUBLE_JUMP);
		abilities.add(ability);
	}

	// called on the imp. whenever any ability is used.
	public void abilityUsed(BPlayer bPlayer, Ability ability) {
	}

	// called when bPlayer kills another player. useful for resets etc
	public void killedPlayer(BPlayer killer, BPlayer victim) {
	}

	// called when any cooldown ends
	public void cooldownEnded(BPlayer bPlayer, TextComponent cooldownName) {
	}

	// called on both the damager and the victim's classes.
	public void handleHit(BPlayer damager, BPlayer victim) {
	}

	// called when a player switches into this class.
	public void playerBecameThisClass(BPlayer bPlayer) {
	}

	// called when a player switches to another class.
	public void playerLeftThisClass(BPlayer bPlayer) {
	}

	// called on all members of this class when the game starts
	// and no where else
	public void gameStartForClassPlayer(BPlayer bPlayer) {

	}
}
