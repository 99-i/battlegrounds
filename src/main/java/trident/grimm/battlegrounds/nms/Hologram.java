package trident.grimm.battlegrounds.nms;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;

public class Hologram {

	private @Getter Location location;
	private @Getter TextComponent text;
	private ArrayList<Player> playersVisibleTo;
	private ArmorStand armorStand;
	private boolean destroyed;
	private boolean shownToAllPlayers;

	public Hologram(Location location, TextComponent text, boolean shownToAllPlayers) {
		this.location = location;
		this.text = text;
		this.playersVisibleTo = new ArrayList<>();
		this.destroyed = false;
		this.shownToAllPlayers = shownToAllPlayers;

		this.armorStand = (ArmorStand) new net.minecraft.world.entity.decoration.ArmorStand(EntityType.ARMOR_STAND,
				((CraftWorld) location.getWorld()).getHandle()).getBukkitEntity();

		armorStand.setMarker(true);
		armorStand.setInvisible(true);
		armorStand.customName(this.text);
		armorStand.setCustomNameVisible(true);
		armorStand.teleport(location);

		if (shownToAllPlayers)
			this.init();

	}

	public void setLocation(Location newLocation) {
		if (destroyed)
			return;
		double distance = this.location.distanceSquared(newLocation);
		Location oldLocation = this.location;
		this.location = newLocation;
		armorStand.teleport(newLocation);

		if (distance > 64) {
			ClientboundTeleportEntityPacket teleportEntityPacket = new ClientboundTeleportEntityPacket(
					((CraftEntity) this.armorStand).getHandle());

			this.sendToAudience(teleportEntityPacket);
		} else {
			final int entityId = armorStand.getEntityId();
			short dx = (short) ((newLocation.getX() * 32 - oldLocation.getX() * 32) *
					128);
			short dy = (short) ((newLocation.getY() * 32 - oldLocation.getY() * 32) *
					128);
			short dz = (short) ((newLocation.getZ() * 32 - oldLocation.getZ() * 32) *
					128);

			ClientboundMoveEntityPacket.Pos moveEntityPosPacket = new ClientboundMoveEntityPacket.Pos(entityId, dx, dy,
					dz,
					this.armorStand.isOnGround());

			this.sendToAudience(moveEntityPosPacket);
		}
	}

	public void setText(TextComponent text) {
		if (destroyed)
			return;
		this.text = text;

		this.armorStand.customName(text);

		final int entityId = armorStand.getEntityId();
		SynchedEntityData entityData = ((CraftArmorStand) armorStand).getHandle().getEntityData();

		ClientboundSetEntityDataPacket setEntityDataPacket = new ClientboundSetEntityDataPacket(entityId,
				entityData.getNonDefaultValues());

		this.sendToAudience(setEntityDataPacket);
	}

	private void sendToAudience(Packet<?> packet) {
		if (this.shownToAllPlayers) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				((CraftPlayer) player).getHandle().connection.send(packet);
			}
		} else {
			for (Player player : this.playersVisibleTo) {
				((CraftPlayer) player).getHandle().connection.send(packet);
			}

		}
	}

	private void init() {

		ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
				((CraftArmorStand) this.armorStand).getHandle());

		final int entityId = armorStand.getEntityId();
		SynchedEntityData entityData = ((CraftArmorStand) armorStand).getHandle().getEntityData();

		ClientboundSetEntityDataPacket setEntityDataPacket = new ClientboundSetEntityDataPacket(entityId,
				entityData.getNonDefaultValues());

		this.sendToAudience(addEntityPacket);
		this.sendToAudience(setEntityDataPacket);

	}

	public void addPlayer(Player player) {
		if (destroyed)
			return;

		if (this.playersVisibleTo.contains(player))
			return;

		this.playersVisibleTo.add(player);

		ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
				((CraftArmorStand) this.armorStand).getHandle());

		final int entityId = armorStand.getEntityId();
		SynchedEntityData entityData = ((CraftArmorStand) armorStand).getHandle().getEntityData();

		ClientboundSetEntityDataPacket setEntityDataPacket = new ClientboundSetEntityDataPacket(entityId,
				entityData.getNonDefaultValues());

		((CraftPlayer) player).getHandle().connection.send(addEntityPacket);
		((CraftPlayer) player).getHandle().connection.send(setEntityDataPacket);

	}

	public void destroy() {
		this.destroyed = true;
		final int entityId = this.armorStand.getEntityId();
		ClientboundRemoveEntitiesPacket removeEntitiesPacket = new ClientboundRemoveEntitiesPacket(
				new int[] { entityId });

		this.sendToAudience(removeEntitiesPacket);
	}

}
