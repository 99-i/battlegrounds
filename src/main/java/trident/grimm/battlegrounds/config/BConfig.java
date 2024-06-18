package trident.grimm.battlegrounds.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import trident.grimm.config.ArrayField;
import trident.grimm.config.DataField;
import trident.grimm.config.ElibConfig;
import trident.grimm.config.MapField;
import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.redis.RedisManager;

public class BConfig {
	public static class Vector {

		@DataField
		private @Getter double x;

		@DataField
		private @Getter double y;

		@DataField
		private @Getter double z;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof org.bukkit.util.Vector) {
				org.bukkit.util.Vector vector = (org.bukkit.util.Vector) obj;
				return (vector.getX() == this.x && vector.getY() == this.y && vector.getZ() == this.z);
			}
			return super.equals(obj);
		}
	}

	public static BConfig getBConfig() {
		App app = App.getInstance();
		app.saveDefaultConfig();

		File currentPatchConfigFile = new File("patches/" + RedisManager.getInstance().getCurrentVersion() + ".yml");

		if (!currentPatchConfigFile.exists()) {
			Bukkit.getLogger().severe("Could not find configuration file for current patch.");
		}

		return ElibConfig.readConfigFile(currentPatchConfigFile, BConfig.class);
	}

	@DataField
	@MapField(keyType = BTeam.class, valueType = ArrayList.class, valueArrayType = Vector.class)
	private @Getter HashMap<BTeam, ArrayList<Vector>> spawnPoints;

	@DataField(key = "nexuses")
	@MapField(keyType = BTeam.class, valueType = Vector.class)
	private @Getter HashMap<BTeam, Vector> nexusLocations;

	@DataField
	@MapField(keyType = Material.class, valueType = ArrayList.class, valueArrayType = Vector.class)
	private @Getter HashMap<Material, ArrayList<Vector>> ores;

	@DataField
	@ArrayField(arrayType = Vector.class)
	private @Getter ArrayList<Vector> villagers;

	@DataField
	@ArrayField(arrayType = Vector.class)
	private @Getter ArrayList<Vector> classChangeSigns;

	public static class ClassDescription {
		@DataField
		private @Getter double maxHealth;
		@DataField
		private @Getter double armor;
		@DataField
		private @Getter double magicResistance;
		@DataField
		private @Getter float walkSpeed;
		@DataField
		private @Getter double healthRegen;
		@DataField
		@MapField(valueType = Object.class)
		private @Getter HashMap<String, Object> parameters;
		@DataField
		@ArrayField(arrayType = String.class)
		private @Getter ArrayList<String> items;
	}

	@DataField(key = "classes")
	@MapField(valueType = ClassDescription.class)
	private @Getter HashMap<String, ClassDescription> classEntries;

	@DataField
	@MapField(keyType = Material.class, valueType = Double.class)
	private @Getter HashMap<Material, Double> swordDamages;

	@DataField(key = "sharpness")
	@MapField(keyType = Integer.class, valueType = Double.class)
	private @Getter HashMap<Integer, Double> sharpnessMultipliers;

	@DataField(key = "strength")
	@MapField(keyType = Integer.class, valueType = Double.class)
	private @Getter HashMap<Integer, Double> strengthMultipliers;

	public static class ArmorData {
		@DataField
		private @Getter double armor;
		@DataField
		private @Getter double magicResistance;
	}

	@DataField(key = "armors")
	@MapField(keyType = Material.class, valueType = ArmorData.class)
	private @Getter HashMap<Material, ArmorData> armorAttributes;

	public static class OreDataEntry {
		@DataField(key = "replacement")
		private @Getter Material replacementMaterial;
		@DataField
		private @Getter int tickCooldown;
	}

	@DataField(key = "ore_replacements")
	@MapField(keyType = Material.class, valueType = OreDataEntry.class)
	private @Getter HashMap<Material, OreDataEntry> oreData;

	@DataField
	private @Getter Vector lobbySpawnPoint;

	@DataField
	private @Getter double defaultDamage;

	public static class Shops {
		@DataField
		@MapField(valueType = String.class)
		private @Getter HashMap<String, String> meta;

		public static class ShopProduct {
			@DataField
			private @Getter String product;
			@DataField
			private @Getter int cost;
			@DataField
			private @Getter int slot;
			@DataField
			private @Getter Material costMaterial;
		}

		@DataField
		@MapField(valueType = ArrayList.class, valueArrayType = ShopProduct.class)
		private @Getter HashMap<String, ArrayList<ShopProduct>> shops;

	}

	@DataField
	private @Getter Shops shopInfo;

	@DataField
	@MapField(valueType = ItemStack.class)
	private @Getter HashMap<String, ItemStack> items;
}
