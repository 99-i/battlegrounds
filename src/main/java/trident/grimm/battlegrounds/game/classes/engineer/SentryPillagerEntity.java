package trident.grimm.battlegrounds.game.classes.engineer;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.entity.Pig;

import lombok.Getter;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.util.MathUtil;

public class SentryPillagerEntity {
	private @Getter NPC npc;
	private NPCRegistry registry;
	private @Getter Sentry sentry;
	private Location location;

	public SentryPillagerEntity(Location location, Sentry sentry, NPCRegistry registry) {
		this.registry = registry;
		this.sentry = sentry;
		this.location = location;
	}

	public void start() {
		this.npc = this.registry.createNPC(EntityType.PILLAGER, "");

		this.npc.getNavigator().setPaused(false);
		this.npc.data().set(NPC.Metadata.RESET_PITCH_ON_TICK, false);
		this.npc.data().set(NPC.Metadata.USE_MINECRAFT_AI, false);
		this.npc.data().set(NPC.Metadata.AMBIENT_SOUND, "none");
		this.npc.data().set(NPC.Metadata.DEATH_SOUND, "none");
		this.npc.data().set(NPC.Metadata.HURT_SOUND, "none");
		this.npc.data().set(NPC.Metadata.COLLIDABLE, false);
		this.npc.data().set(NPC.Metadata.FLUID_PUSHABLE, false);
		this.npc.data().set(NPC.Metadata.FLYABLE, true);
		this.npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, false);

		this.npc.spawn(location);
		this.npc.setProtected(false);
		Entity thisEntity = this.npc.getEntity();
		thisEntity.setGravity(false);
		((Pillager) thisEntity).getEquipment().setItem(EquipmentSlot.HAND, new ItemStack(Material.CROSSBOW));
	}

	public void ride(SentryPigEntity pig) {
		NPC pigNPC = pig.getNpc();
		Entity pigEntity = pigNPC.getEntity();

		Entity thisEntity = this.npc.getEntity();

		((Pig) pigEntity).addPassenger(thisEntity);
	}

	public void shoot(Entity target) {
		shootArrow(target.getLocation());
	}

	private void shootArrow(Location target) {
		Pillager thisPillager = (Pillager) this.npc.getEntity();
		Location ourLocation = thisPillager.getEyeLocation();
		// faster the farther away
		double distance = thisPillager.getEyeLocation().distance(target);
		double speed = 1.3 + (0.1 * distance);
		double upAmount = MathUtil.clamp(0.3 + (0.1 * distance), 0.3, 3);

		Vector velocity = target.add(new Vector(0, upAmount,
				0)).subtract(ourLocation).toVector().normalize()
				.multiply(speed);
		Arrow arrow = ourLocation.getWorld().spawnArrow(ourLocation, velocity, 0.1f, 0f);

		arrow.setShooter(thisPillager);
		arrow.setVelocity(velocity);

		ItemUtil.setValue(arrow.getPersistentDataContainer(), "sentry-pillager-id",
				thisPillager.getUniqueId().toString());
	}

	public void hurt() {
		((Pillager) this.npc.getEntity()).damage(0);
	}

	public void died() {
		this.npc.destroy();
	}
}
