package trident.grimm.battlegrounds.game.classes.engineer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import trident.grimm.inventory.ElibGUI;
import trident.grimm.inventory.ElibView;
import trident.grimm.inventory.ElibButton.ElibButtonPressType;
import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.classes.BClasses;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.nms.Hologram;
import trident.grimm.battlegrounds.util.HealthbarUtil;
import trident.grimm.battlegrounds.util.MathUtil;

public class Sentry {

	public static enum UpgradeType {
		MAX_HEALTH("Max Health", "HP"), DAMAGE("Damage", "damage/arrow"),
		ATTACK_SPEED("Attack Speed", "attacks/second"), RANGE("Range", "blocks");

		private @Getter String prettyPrinted;
		private @Getter String units;

		private UpgradeType(String prettyPrinted, String units) {
			this.prettyPrinted = prettyPrinted;
			this.units = units;
		}
	}

	private static class SentryUpgrade {
		private @Getter UpgradeType upgradeType;

		private @Getter double startingValue;
		private @Getter double increment;

		private @Getter int upgrade = 0; // 0 - 5. 5 is max.

		private @Getter Material displayMaterial;

		public SentryUpgrade(UpgradeType upgradeType, Material displayMaterial, double startingValue,
				double increment) {
			this.upgradeType = upgradeType;
			this.displayMaterial = displayMaterial;
			this.startingValue = startingValue;
			this.increment = increment;
		}

		public int getCost() {
			return this.upgrade + 1;
		}

		public double getValue() {
			return this.startingValue + this.increment * this.upgrade;
		}

		public void upgrade() {
			this.upgrade += 1;
		}

		public boolean maxed() {
			return this.upgrade == 5;
		}

		public ItemStack getDisplayItem() {
			ItemStack itemStack = new ItemStack(this.displayMaterial);
			ItemUtil.setDisplayName(itemStack,
					Component.text("Upgrade " + this.upgradeType.getPrettyPrinted(), TextColor.color(255, 255, 255)));
			if (!this.maxed()) {
				ItemUtil.addLoreData(itemStack, Component.text("Current " + this.upgradeType.getPrettyPrinted()),
						Component.text(getValue() + " " + this.upgradeType.getUnits()));

				ItemUtil.addLoreData(itemStack,
						Component.text().color(TextColor.color(255, 255, 255))
								.append(Component.text("UPGRADED ").decorate(TextDecoration.BOLD))
								.append(Component.text(this.upgradeType.getPrettyPrinted()))
								.build(),
						Component.text((getValue() + this.increment) + " " + this.upgradeType.getUnits(),
								TextColor.color(255, 255, 255)));

				ItemUtil.addLore(itemStack,
						Component.text("Cost: " + (this.upgrade + 1) + " Emerald" + (this.upgrade != 0 ? "s" : ""))
								.color(TextColor.color(255, 255, 0)).decorate(TextDecoration.BOLD));

			} else {
				ItemUtil.addLore(itemStack,
						Component.text().decorate(TextDecoration.BOLD)
								.append(Component.text("MAXED! ", TextColor.color(0, 255, 0)))
								.color(TextColor.color(255, 255, 255))
								.resetStyle()
								.color(TextColor.color(255, 255, 255))
								.append(Component.text(this.upgradeType.getPrettyPrinted() + ": "))
								.append(Component.text(getValue() + " " + this.upgradeType.getUnits())).build());
			}

			return itemStack;
		}

	}

	private @Getter SentryPigEntity pigEntity;
	private @Getter SentryPillagerEntity pillagerEntity;
	private @Getter Location location;
	private @Getter boolean dead = false;

	private @Getter BPlayer placer;

	private ArrayList<SentryUpgrade> upgrades;

	private BukkitTask shootTask;
	private ElibGUI upgradeGUI;

	private @Getter @Setter double health;
	private Hologram healthHologram;

	public Sentry(Location location, BPlayer placer, NPCRegistry registry) {
		this.upgrades = new ArrayList<>();
		// todo: hardcoded
		this.upgrades.add(new SentryUpgrade(UpgradeType.MAX_HEALTH, Material.RED_DYE, 300d, 100d));
		this.upgrades.add(new SentryUpgrade(UpgradeType.DAMAGE, Material.FIREWORK_ROCKET, 10d, 10d));
		this.upgrades.add(new SentryUpgrade(UpgradeType.ATTACK_SPEED, Material.CROSSBOW, 0.5d, 0.25d));
		this.upgrades.add(new SentryUpgrade(UpgradeType.RANGE, Material.ENDER_EYE, 300d, 100d));

		this.location = location;
		this.pigEntity = new SentryPigEntity(location, this, registry);
		this.pillagerEntity = new SentryPillagerEntity(location, this, registry);
		this.placer = placer;
		this.health = this.getUpgradeValue(UpgradeType.MAX_HEALTH); // todo: hardcoded
	}

	public void spawn() {
		this.healthHologram = new Hologram(location.clone().add(new Vector(0, 2.1, 0)),
				HealthbarUtil.getBarredHealthbar(20, 1.0),
				true);
		this.pigEntity.start();
		this.pillagerEntity.start();
		this.start();

		this.pillagerEntity.ride(this.pigEntity);
		this.updateHologram();
		this.upgradeGUI = new ElibGUI();
		this.upgradeGUI.addView("main", 9, Component.text("Upgrade Sentry"));
	}

	private void start() {
		generateNewShootingTask();
	}

	private SentryUpgrade getUpgrade(UpgradeType upgradeType) {
		for (SentryUpgrade upgrade : this.upgrades) {
			if (upgrade.getUpgradeType() == upgradeType) {
				return upgrade;
			}
		}
		return null;
	}

	// todo: config
	private boolean isUpgradeMaxed(UpgradeType upgradeType) {
		SentryUpgrade upgrade = getUpgrade(upgradeType);
		return upgrade != null && upgrade.maxed();
	}

	private boolean shouldTargetPlayer(Player player) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		return bPlayer.getTeam() != placer.getTeam();
	}

	private void updateGuiForPlayer(Player player) {
		ElibView view = this.upgradeGUI.getView("main");
		int emeraldsNumber = BPlayer.getBPlayer(player).getBackpack().getEmeralds();
		ItemStack emeralds = new ItemStack(Material.EMERALD, (int) MathUtil.clamp((double) emeraldsNumber, 1, 64));
		ItemUtil.setDisplayName(emeralds, Component.text("Emeralds").color(TextColor.color(0, 255, 0)));
		ItemUtil.addLore(emeralds, Component.text("Emeralds: " + emeraldsNumber));

		int i = 0;
		for (SentryUpgrade upgrade : this.upgrades) {
			view.addButton(i, upgrade.getDisplayItem(), (event) -> {
				if (event.type() == ElibButtonPressType.LEFT) {
					upgrade(upgrade.getUpgradeType());
					event.button().setItem(upgrade.getDisplayItem());
					updateGuiForPlayer(player);
				}
			});
			i++;
		}

		view.setItem(8, emeralds);
	}

	public void openUpgradeGUIForPlayer(Player player) {
		updateGuiForPlayer(player);
		this.upgradeGUI.setPlayerView(player, "main");
	}

	private void updateHologram() {
		double maxHealth = getUpgradeValue(UpgradeType.MAX_HEALTH);
		double ratio = this.health / maxHealth;

		TextComponent name = HealthbarUtil.getBarredHealthbar(20, ratio);

		this.healthHologram.setText(name);
	}

	private void upgrade(UpgradeType type) {
		if (isUpgradeMaxed(type))
			return;
		SentryUpgrade upgrade = this.getUpgrade(type);
		int cost = upgrade.getCost();
		if (this.placer.getBackpack().getEmeralds() < cost) {
			Player placerPlayer = this.placer.getPlayer();
			if (placerPlayer == null)
				return;

			placerPlayer.sendMessage(Component.text("You do not have enough emeralds to afford this upgrade!")
					.color(TextColor.color(255, 100, 100)));

			return;
		}

		this.placer.getBackpack().setEmeralds(this.placer.getBackpack().getEmeralds() - cost);

		if (upgrade.getUpgradeType() == type) {
			if (type == UpgradeType.MAX_HEALTH) {
				double ratio = this.health / upgrade.getValue();
				upgrade.upgrade();
				this.health = upgrade.getValue() * ratio;
				this.updateHologram();
			} else {
				upgrade.upgrade();
			}
		}
	}

	public double getUpgradeValue(UpgradeType type) {
		for (SentryUpgrade upgrade : this.upgrades) {
			if (upgrade.getUpgradeType() == type) {
				return upgrade.getValue();
			}
		}
		return 0;
	}

	private Player getTargetPlayer() {
		double range = getUpgradeValue(UpgradeType.RANGE);
		List<Entity> entities = location.getWorld()
				.getNearbyEntities(location, range, range, range, (entity) -> entity.getType() == EntityType.PLAYER)
				.stream().sorted(new Comparator<Entity>() {

					@Override
					public int compare(Entity o1, Entity o2) {
						return (int) (o1.getLocation().distanceSquared(location)
								- o2.getLocation().distanceSquared(location));
					}

				}).toList();
		for (Entity entity : entities) {
			if (shouldTargetPlayer((Player) entity)) {
				return (Player) entity;
			}
		}
		return null;
	}

	// start() first calls this, and it creates a bukkitrunnable that runs when the
	// next shooting time happens.
	// when it shoots, it starts this runnable again with the next shooting time.
	private void generateNewShootingTask() {
		shootTask = new BukkitRunnable() {

			@Override
			public void run() {
				shoot();
				generateNewShootingTask();
			}

		}.runTaskLater(App.getInstance(), (long) (20 / getUpgradeValue(UpgradeType.ATTACK_SPEED)));
	}

	private void shoot() {
		Player targetPlayer = getTargetPlayer();
		if (targetPlayer == null)
			return;
		this.pillagerEntity.shoot(targetPlayer);
	}

	public void wasDamaged(double damage) {
		this.setHealth(this.getHealth() - damage);
		if (this.getHealth() <= 0) {
			this.died();
			return;
		}

		this.pigEntity.hurt();
		this.pillagerEntity.hurt();

		this.pigEntity.getNpc().getEntity().setVelocity(new Vector(0, 0, 0));
		this.pillagerEntity.getNpc().getEntity().setVelocity(new Vector(0, 0, 0));

		this.pigEntity.getNpc().getEntity().teleport(this.location);

		this.updateHologram();
	}

	public void died() {
		this.healthHologram.destroy();
		shootTask.cancel();
		dead = true;
		this.pigEntity.died();
		this.pillagerEntity.died();
		BClasses.ENGINEER.sentryDied(this);
	}
}
