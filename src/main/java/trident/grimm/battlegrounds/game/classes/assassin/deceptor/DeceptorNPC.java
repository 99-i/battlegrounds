package trident.grimm.battlegrounds.game.classes.assassin.deceptor;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.classes.BClasses;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;

public class DeceptorNPC {
	private NPC npc;
	private BPlayer owner;
	private NPCRegistry registry;
	private boolean removed = false;

	public DeceptorNPC(BPlayer owner, NPCRegistry registry) {
		this.owner = owner;
		this.registry = registry;
	}

	// spawn the NPC in owner's position, and have it start walking toward
	// the direction owner is walking
	public void start() {
		Player ownerPlayer = this.owner.getPlayer();
		if (ownerPlayer == null)
			return;
		this.npc = this.registry.createNPC(EntityType.PLAYER, ownerPlayer.getName());

		this.setArmor();
		this.setItem();
		this.npc.data().set(NPC.Metadata.SPAWN_NODAMAGE_TICKS, 0);
		this.npc.spawn(ownerPlayer.getLocation());
		this.npc.setProtected(true);
		ItemUtil.setValue(this.npc.getEntity().getPersistentDataContainer(), "deceptor-owner-uuid",
				ownerPlayer.getUniqueId().toString());
		this.navigate();

		Entity npcEntity = this.npc.getEntity();
		if (npcEntity instanceof Player) {
			BPlayer.getBPlayer((Player) npcEntity);
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				remove();
			}
		}.runTaskLater(App.getInstance(), 20 * 10); // todo!: hardcoded number.
	}

	public void explode() {
		Location position = this.npc.getStoredLocation();
		new BukkitRunnable() {
			int counter = 0;

			@Override
			public void run() {
				counter += 1;
				DustOptions dustOptions = new DustOptions(Color.fromRGB(128, 128, 128), 7);
				position.getWorld().spawnParticle(Particle.DUST, position, 70, 1.5, 1.5, 1.5, dustOptions);
				if (counter >= 30) {
					this.cancel();
				}
			}
		}.runTaskTimer(App.getInstance(), 0, 2);

		remove();
	}

	private void remove() {
		if (!removed) {
			BPlayer.removeBPlayerIfPresent((Player) this.npc.getEntity());
			this.npc.destroy();
			BClasses.ASSASSIN.removeNpc(this.owner);
			removed = true;
		}
	}

	private void setArmor() {
		Player ownerPlayer = this.owner.getPlayer();
		this.npc.getOrAddTrait(Equipment.class).set(EquipmentSlot.HELMET,
				ownerPlayer.getInventory().getHelmet());
		this.npc.getOrAddTrait(Equipment.class).set(EquipmentSlot.CHESTPLATE,
				ownerPlayer.getInventory().getChestplate());
		this.npc.getOrAddTrait(Equipment.class).set(EquipmentSlot.LEGGINGS,
				ownerPlayer.getInventory().getLeggings());
		this.npc.getOrAddTrait(Equipment.class).set(EquipmentSlot.BOOTS,
				ownerPlayer.getInventory().getBoots());

	}

	private void setItem() {
		Player ownerPlayer = this.owner.getPlayer();
		ItemStack swordStack = null;

		for (ItemStack itemStack : ownerPlayer.getInventory()) {
			if (itemStack != null) {
				if (EnchantmentTarget.WEAPON.includes(itemStack.getType())) {
					swordStack = itemStack.clone();
				}
			}
		}

		if (swordStack != null) {
			this.npc.getOrAddTrait(Equipment.class).set(EquipmentSlot.HAND,
					swordStack);
		}
	}

	private void navigate() {
		Player ownerPlayer = owner.getPlayer();
		if (ownerPlayer == null)
			return;

		Vector direction = ownerPlayer.getLocation().getDirection().multiply(new Vector(1, 0, 1));

		npc.getNavigator().getLocalParameters().baseSpeed(10f);
		npc.getNavigator().getLocalParameters().speedModifier(1.0f);
		npc.getNavigator().setStraightLineTarget(ownerPlayer.getLocation().add(direction.multiply(44)));
		npc.getNavigator().setPaused(false);
	}
}
