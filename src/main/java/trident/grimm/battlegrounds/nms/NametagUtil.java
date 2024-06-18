package trident.grimm.battlegrounds.nms;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.TextComponent;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import trident.grimm.battlegrounds.App;

public class NametagUtil {

	public static void spawnNametag(List<Player> visibleToPlayers, Location position, TextComponent text,
			int durationTicks) {
		Location newLocation = position.clone().add(new Vector(0, 1.3, 0));
		ArmorStand armorStand = (ArmorStand) new net.minecraft.world.entity.decoration.ArmorStand(
				EntityType.ARMOR_STAND,
				((CraftWorld) position.getWorld()).getHandle()).getBukkitEntity();
		armorStand.setMarker(true);
		armorStand.setInvisible(true);
		armorStand.customName(text);
		armorStand.setCustomNameVisible(true);
		armorStand.teleport(newLocation);

		ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
				((CraftArmorStand) armorStand).getHandle());

		final int entityId = armorStand.getEntityId();
		SynchedEntityData entityData = ((CraftArmorStand) armorStand).getHandle().getEntityData();

		ClientboundSetEntityDataPacket setEntityDataPacket = new ClientboundSetEntityDataPacket(entityId,
				entityData.getNonDefaultValues());

		ClientboundRemoveEntitiesPacket removeEntitiesPacket = new ClientboundRemoveEntitiesPacket(
				new int[] { entityId });

		for (Player player : visibleToPlayers) {
			((CraftPlayer) player).getHandle().connection.send(addEntityPacket);
			((CraftPlayer) player).getHandle().connection.send(setEntityDataPacket);
		}

		Bukkit.getScheduler().runTaskLaterAsynchronously(App.getInstance(), () -> {
			for (Player player : visibleToPlayers) {
				((CraftPlayer) player).getHandle().connection.send(removeEntitiesPacket);
			}
		}, durationTicks);

	}

}
