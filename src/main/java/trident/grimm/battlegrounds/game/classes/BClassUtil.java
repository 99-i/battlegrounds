package trident.grimm.battlegrounds.game.classes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftEnderDragon;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.BPlayer.BDamageType;
import trident.grimm.battlegrounds.util.Pair;

public class BClassUtil {

	// helper method to calculate damage and stuff.
	public static void playerDamagedEntity(BPlayer bPlayer, LivingEntity entity, BDamageType damageType,
			double damage) {

		if (entity instanceof Player) {
			Player damagedEntity = (Player) entity;
			BPlayer.getBPlayer(damagedEntity).damage(damage, bPlayer, damageType);
		} else {
			if (entity instanceof LivingEntity) {
				LivingEntity livingEntity = (LivingEntity) entity;
				if (((CraftLivingEntity) livingEntity) instanceof CraftEnderDragon) {
					CraftEnderDragon dragon = ((CraftEnderDragon) livingEntity);
					double newHealth = Math.max(0, dragon.getHealth() - damage);
					dragon.setHealth(newHealth);
					dragon.damage(1);
					// TODO: dragon lastDamager.
					dragon.playHurtAnimation(0);
					dragon.getWorld().playSound(dragon.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 100, 1);
				}
				livingEntity.damage(damage);
			}
		}
	}

	// get the target entity of a player's facing direction.
	// player: the player.
	// range: the range for which to target
	// hitboxMultipler: the multiplier to scale enemy hitboxes (used to make
	// abilities easier to hit.)
	public static Pair<Entity, Vector> getTargetEntity(Player player, double range, double hitboxMultiplier) {
		Location start = player.getEyeLocation();
		Predicate<Entity> predicate = entity -> (entity != player);
		RayTraceResult result = player.getWorld().rayTraceEntities(start, player.getLocation().getDirection(), range,
				hitboxMultiplier, predicate);

		if (result == null)
			return null;
		return new Pair<Entity, Vector>(result.getHitEntity(), result.getHitPosition());
	}

	public static ArrayList<BPlayer> getNearbyBPlayers(Player player, double range) {
		ArrayList<BPlayer> players = new ArrayList<>();

		List<Entity> entities = player.getNearbyEntities(range, range, range);

		for (Entity entity : entities) {
			if (entity instanceof Player) {
				players.add(BPlayer.getBPlayer((Player) entity));
			}
		}

		return players;
	}

	public static List<BPlayer> getBPlayersOfClass(BClass bClass) {
		Collection<BPlayer> allBPlayers = BPlayer.getAllBPlayers();

		List<BPlayer> players = allBPlayers.stream()
				.filter(bPlayer -> !bPlayer.isNPC() && bPlayer.getBClass() == bClass)
				.collect(Collectors.toList());

		return players;
	}
}
