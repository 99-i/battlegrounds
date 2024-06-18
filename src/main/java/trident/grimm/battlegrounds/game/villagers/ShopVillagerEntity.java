package trident.grimm.battlegrounds.game.villagers;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.util.NMS;
import trident.grimm.battlegrounds.App;

// custom shop villager nms entity
public class ShopVillagerEntity {

	private NPC npc;
	private Location location;

	public ShopVillagerEntity(Location location, NPCRegistry npcRegistry) {
		this.location = location;
		this.npc = npcRegistry.createNPC(EntityType.WANDERING_TRADER, "Shop");
	}

	public void spawn() {
		this.npc.getNavigator().setPaused(true);
		this.npc.data().set(NPC.Metadata.USE_MINECRAFT_AI, false);
		this.npc.data().set(NPC.Metadata.AMBIENT_SOUND, "none");
		this.npc.data().set(NPC.Metadata.DEATH_SOUND, "none");
		this.npc.data().set(NPC.Metadata.HURT_SOUND, "none");
		this.npc.data().set(NPC.Metadata.COLLIDABLE, false);
		this.npc.data().set(NPC.Metadata.FLUID_PUSHABLE, false);
		this.npc.data().set(NPC.Metadata.FLYABLE, true);

		this.npc.spawn(this.location);
		this.npc.getEntity().setGravity(false);

		this.start();
	}

	private void start() {
		new BukkitRunnable() {

			@Override
			public void run() {
				ShopVillagerEntity.this.npc.addTrait(LookClose.class);
			}

		}.runTask(App.getInstance());
	}
}
