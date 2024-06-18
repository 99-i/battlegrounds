package trident.grimm.battlegrounds.game.classes.widow;

import java.util.Random;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.TickCounter;
import trident.grimm.battlegrounds.game.classes.BClasses;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.BPlayer.BDamageType;
import trident.grimm.battlegrounds.util.HealthbarUtil;
import trident.grimm.battlegrounds.util.MathUtil;
import net.citizensnpcs.api.ai.AttackStrategy;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.util.NMS;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class WidowSpider {
	private @Getter BPlayer widow;
	private @Getter NPC npc;
	private NPCRegistry registry;

	private @Getter BPlayer target;

	private boolean targetting = false;

	private Location wanderLocation;

	private final static double MAX_HEALTH = 100d; // todo: hardcoded.
	private final static int ATTACK_TICKS = 25; // todo: hardcoded.

	private static class WidowSpiderAttackStrategy implements AttackStrategy {
		private int attackTicksRemaining = 0;
		private long lastAttackTickNumber = 0;
		private WidowSpider spider;

		public WidowSpiderAttackStrategy(WidowSpider spider) {
			this.spider = spider;
		}

		private void calculateAttackTicksRemaining() {
			long currentTickNumber = TickCounter.getInstance().getTickCount().longValue();

			attackTicksRemaining -= currentTickNumber - lastAttackTickNumber;
			lastAttackTickNumber = currentTickNumber;

		}

		private void attack(LivingEntity target) {
			Entity spiderEntity = spider.getNpc().getEntity();
			LivingEntity thisEntity = (LivingEntity) spiderEntity;

			NMS.attack(thisEntity, target);

			BPlayer targetBPlayer = BPlayer.getBPlayer((Player) target);

			// todo: hardcoded number for damage.
			targetBPlayer.damage(15, this.spider.widow, BDamageType.MAGIC);
		}

		@Override
		public boolean handle(LivingEntity attacker, LivingEntity target) {
			this.calculateAttackTicksRemaining();
			if (attackTicksRemaining <= 0) {
				attackTicksRemaining = ATTACK_TICKS;
				attack(target);
			}
			return true;
		}

	}

	public WidowSpider(BPlayer widow, NPCRegistry registry) {
		this.widow = widow;
		this.registry = registry;
		this.wanderLocation = widow.getPlayer().getLocation();
	}

	public void spawn(Location location) {
		Player player = this.widow.getPlayer();
		if (player == null)
			return;

		this.npc = registry.createNPC(EntityType.SPIDER, "", location);

		this.npc.getNavigator().getDefaultParameters().attackRange(3);
		this.npc.getNavigator().getDefaultParameters().attackStrategy(new WidowSpiderAttackStrategy(this));

		Bukkit.broadcastMessage("" + this.npc.getNavigator().getDefaultParameters().speedModifier());

		this.npc.data().set(NPC.Metadata.COLLIDABLE, false);
		this.npc.data().set(NPC.Metadata.USE_MINECRAFT_AI, false);
		((Spider) this.npc.getEntity()).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(MAX_HEALTH);
		((Spider) this.npc.getEntity()).setHealth(MAX_HEALTH);

		this.npc.setProtected(false);

		this.start();
	}

	public void setTarget(BPlayer target) {
		this.target = target;

		Player targetEntityPlayer = target.getPlayer();

		if (targetEntityPlayer == null)
			return;

		npc.getNavigator().setTarget(targetEntityPlayer, true);
	}

	private void recalculateWanderLocation() {
		Player widowPlayer = this.widow.getPlayer();
		if (widowPlayer == null)
			return;
		Random random = new Random();
		this.wanderLocation = widowPlayer.getLocation().clone()
				.add(new Vector(random.nextDouble(2), random.nextDouble(1), random.nextDouble(2)));
	}

	public void speedup() {
		this.npc.getNavigator().getDefaultParameters().speedModifier(1.4f); // todo: hardcoded
	}

	public void resetSpeed() {
		this.npc.getNavigator().getDefaultParameters().speedModifier(1.0f); // todo: hardcoded
	}

	public void damage(double d) {
		Entity entity = npc.getEntity();
		if (entity == null) {
			return;
		}

		Spider spiderEntity = (Spider) entity;

		double health = MathUtil.clamp(spiderEntity.getHealth() - d, 0, 1000);
		spiderEntity.setHealth(health);
		setCustomName();
		if (health <= 0) {
			died();
			return;
		}

	}

	private void died() {
		BClasses.WIDOW.spiderDied(this);
	}

	private void start() {
		new BukkitRunnable() {

			@Override
			public void run() {
				Entity entity = npc.getEntity();
				if (entity == null) {
					this.cancel();
					return;
				}

				Spider spiderEntity = (Spider) entity;

				double health = MathUtil.clamp(spiderEntity.getHealth() - 4, 0, 1000);
				spiderEntity.setHealth(health);
				if (health <= 0) {
					died();
					this.cancel();
					return;
				}

				setCustomName();

				recalculateWanderLocation();

				if (target != null) {
					Entity targetEntity = target.getPlayer();
					if (targetEntity != null) {
						Location thisLocation = spiderEntity.getLocation();
						if (thisLocation.distanceSquared(target.getPlayer().getLocation()) > 100) {
							target = null;
							targetting = false;
						} else {
							if (npc.isSpawned()) {
								if (!targetting) {
									npc.getNavigator().cancelNavigation();
									targetting = true;
								}

								if (npc.isSpawned())
									NMS.cancelMoveDestination(npc.getEntity());

								npc.getNavigator().setTarget(targetEntity, true);
								targetting = true;
							} else {
								this.cancel();
								return;
							}
						}

					}
				} else {
					if (npc.isSpawned()) {
						if (!targetting) {
							npc.getNavigator().setTarget(wanderLocation);
						}
					} else {
						this.cancel();
						return;
					}
				}

			}

		}.runTaskTimer(App.getInstance(), 0, 20);
	}

	private void setCustomName() {
		Entity entity = npc.getEntity();
		if (entity == null)
			return;
		Spider spiderEntity = (Spider) entity;
		double currentHealth = spiderEntity.getHealth();

		TextComponent name = HealthbarUtil.getBarredHealthbar(20, currentHealth / MAX_HEALTH);
		String serializedName = LegacyComponentSerializer.legacyAmpersand().serialize(name);
		this.npc.setName(serializedName);
	}

}
