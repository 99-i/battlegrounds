package trident.grimm.battlegrounds.game.classes.engineer;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;

import lombok.Getter;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;

public class SentryPigEntity {
	private @Getter NPC npc;
	private NPCRegistry registry;
	private @Getter Sentry sentry;
	private Location location;

	public SentryPigEntity(Location location, Sentry sentry, NPCRegistry registry) {
		this.registry = registry;
		this.sentry = sentry;
		this.location = location;
	}

	public void start() {
		this.npc = this.registry.createNPC(EntityType.PIG, "");
		this.npc.getNavigator().setPaused(false);
		this.npc.data().set(NPC.Metadata.RESET_PITCH_ON_TICK, false);
		this.npc.data().set(NPC.Metadata.AMBIENT_SOUND, "none");
		this.npc.data().set(NPC.Metadata.DEATH_SOUND, "none");
		this.npc.data().set(NPC.Metadata.HURT_SOUND, "none");
		this.npc.data().set(NPC.Metadata.USE_MINECRAFT_AI, false);
		this.npc.data().set(NPC.Metadata.COLLIDABLE, false);
		this.npc.data().set(NPC.Metadata.FLUID_PUSHABLE, false);
		this.npc.data().set(NPC.Metadata.FLYABLE, true);
		this.npc.spawn(location);
		this.npc.setProtected(false);

		this.npc.getEntity().setGravity(false);
	}

	public void hurt() {
		((Pig) this.npc.getEntity()).damage(0);
	}

	public void died() {
		this.npc.destroy();
	}

}
