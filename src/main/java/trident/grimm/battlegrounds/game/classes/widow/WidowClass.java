package trident.grimm.battlegrounds.game.classes.widow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.Parameters;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.game.CombatManager;
import trident.grimm.battlegrounds.game.CooldownManager;
import trident.grimm.battlegrounds.game.ability.Ability;
import trident.grimm.battlegrounds.game.classes.BClass;
import trident.grimm.battlegrounds.game.classes.BClasses;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.BPlayer.BDamageType;
import trident.grimm.battlegrounds.items.ItemUtil;

public class WidowClass extends BClass {

	private final static String CONFIG_SECTION = "widow";
	private static Parameters parameters = new Parameters(
			App.getInstance().getBConfig().getClassEntries().get("widow").getParameters());
	private final static String ARACHNE_ABILITY_ID = parameters.getRawString("arachne_ability_id");
	private final static TextComponent ARACHNE_COOLDOWN_NAME = parameters.getString("arachne_cooldown_name");
	private final static int ARACHNE_COOLDOWN_MS = parameters.getInt("arachne_cooldown_ms");

	private final static TextComponent POISON_SPIT_COOLDOWN_NAME = parameters.getString("poison_spit_cooldown_name");
	private final static int POISON_SPIT_COOLDOWN_TICKS = parameters.getInt("poison_spit_cooldown_ms");
	private final static double POISON_SPIT_DAMAGE = parameters.getDouble("poison_spit_damage");

	private NPCRegistry npcRegistry;

	private HashMap<BPlayer, ArrayList<WidowSpider>> playerSpiders;
	private HashMap<Spider, WidowSpider> spidersWidowSpiders;

	public WidowClass() {
		super(Material.SPIDER_EYE, Component.text("Widow", TextColor.color(88, 44, 94)), CONFIG_SECTION);
		this.playerSpiders = new HashMap<>();
		this.spidersWidowSpiders = new HashMap<>();
		this.registerFKeyAbility();
		this.registerItemAbility(ARACHNE_ABILITY_ID);
		this.npcRegistry = CitizensAPI.createInMemoryNPCRegistry("WIDOW_SPIDERS");
	}

	@Override
	public void abilityUsed(BPlayer bPlayer, Ability ability) {
		switch (ability.getAbilityType()) {
			case F_KEY:
				playerPoisonSpit(bPlayer);
				break;
			case ITEM:
				if (ability.getAbilityId().equals(ARACHNE_ABILITY_ID)) {
					playerArachne(bPlayer);
				}
				break;
			default:
				break;
		}
	}

	private void playerPoisonSpit(BPlayer bPlayer) {
		CooldownManager cooldownManager = CooldownManager.getInstance();

		if (!cooldownManager.isCooldownOver(bPlayer, POISON_SPIT_COOLDOWN_NAME))
			return;

		Player player = bPlayer.getPlayer();
		if (player == null)
			return;

		ThrownPotion thrownPotion = player.launchProjectile(ThrownPotion.class);
		ItemStack potionItem = new ItemStack(Material.SPLASH_POTION, 1);

		PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();

		potionMeta.setColor(Color.fromRGB(1, 50, 31));

		potionItem.setItemMeta(potionMeta);

		thrownPotion.setItem(potionItem);

		ItemUtil.setValue(thrownPotion.getPersistentDataContainer(), "widow-poison-spit-thrower",
				player.getUniqueId().toString());

		cooldownManager.setCooldown(bPlayer, POISON_SPIT_COOLDOWN_NAME, POISON_SPIT_COOLDOWN_TICKS);
	}

	@EventHandler
	public void onPotionSplash(PotionSplashEvent event) {
		ThrownPotion thrown = event.getEntity();

		String value = ItemUtil.getValue(thrown.getPersistentDataContainer(), "widow-poison-spit-thrower");

		if (value == null)
			return;

		event.setCancelled(true);

		Player player = Bukkit.getPlayer(UUID.fromString(value));

		if (player == null)
			return;

		BPlayer bPlayer = BPlayer.getBPlayer(player);

		for (Entity entity : event.getAffectedEntities()) {
			if (entity instanceof Player) {
				BPlayer affectedBPlayer = BPlayer.getBPlayer((Player) entity);
				if (affectedBPlayer.getTeam() != bPlayer.getTeam()) {
					affectedBPlayer.damage(POISON_SPIT_DAMAGE, bPlayer, BDamageType.MAGIC);
				}
			} else if (entity instanceof LivingEntity) {
				// todo: hardcoded widow damage number 5.
				((LivingEntity) entity).damage(5);
			}
		}

	}

	public void spiderDied(WidowSpider spider) {
		this.playerSpiders.get(spider.getWidow()).remove(spider);
		this.spidersWidowSpiders.remove(spider.getNpc().getEntity());
	}

	private void playerArachne(BPlayer bPlayer) {
		CooldownManager cooldownManager = CooldownManager.getInstance();

		if (!cooldownManager.isCooldownOver(bPlayer, ARACHNE_COOLDOWN_NAME))
			return;

		Player player = bPlayer.getPlayer();
		if (player == null)
			return;

		WidowSpider spider = new WidowSpider(bPlayer, this.npcRegistry);
		spider.spawn(player.getLocation());

		spidersWidowSpiders.put((Spider) spider.getNpc().getEntity(), spider);

		this.playerSpiders.get(bPlayer).add(spider);

		speedUpAllSpiders(bPlayer);

		new BukkitRunnable() {

			@Override
			public void run() {
				setNormalSpeedForSpiders(bPlayer);
			}

		}.runTaskLater(App.getInstance(), 100); // todo: hardcoded.

		cooldownManager.setCooldown(bPlayer, ARACHNE_COOLDOWN_NAME, ARACHNE_COOLDOWN_MS);
	}

	private void speedUpAllSpiders(BPlayer bPlayer) {
		for (WidowSpider spider : this.playerSpiders.get(bPlayer)) {
			spider.speedup();
		}
	}

	private void setNormalSpeedForSpiders(BPlayer bPlayer) {
		for (WidowSpider spider : this.playerSpiders.get(bPlayer)) {
			spider.resetSpeed();
		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!event.getEntity().hasMetadata("NPC"))
			return;
		Entity entity = event.getEntity();
		Entity damager = event.getDamager();

		if (!(damager instanceof Player)) {
			return;
		}

		if (!spidersWidowSpiders.containsKey(entity))
			return;
		event.setDamage(0);

		double damage = CombatManager.getPlayerDamage(BPlayer.getBPlayer((Player) damager),
				((Player) event.getDamager()).getEquipment().getItem(EquipmentSlot.HAND));

		WidowSpider widowSpider = spidersWidowSpiders.get(entity);

		BTeam widowTeam = widowSpider.getWidow().getTeam();
		BTeam damagerTeam = BPlayer.getBPlayer((Player) damager).getTeam();

		if (widowTeam == damagerTeam) {
			event.setCancelled(true);
			return;
		}

		widowSpider.damage(damage);
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (!event.getEntity().hasMetadata("NPC"))
			return;
		if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
				|| event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
				|| event.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
			return;
		} else if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {

			Entity entity = event.getEntity();

			if (!spidersWidowSpiders.containsKey(entity))
				return;

			Bukkit.broadcast(Component.text("TODO: Widow Spider received Projectile Hit!")
					.color(TextColor.color(255, 0, 0)));
			return;
		}
		Entity entity = event.getEntity();

		if (!spidersWidowSpiders.containsKey(entity))
			return;

		// todo: damage.

		// Sentry sentry = entitySentries.get(entity);
		// sentry.wasDamaged(event.getDamage());

		Bukkit.broadcast(Component
				.text("Widow Spider received EntityDamageEvent (" + event.getCause().name() + ")! Mark this edge case!")
				.color(TextColor.color(255, 0, 0)));
		Bukkit.broadcast(Component.text(event.getCause().name()));
		event.setDamage(0);
	}

	@Override
	public void killedPlayer(BPlayer killer, BPlayer victim) {
		Player player = victim.getPlayer();
		if (player == null)
			return;
		WidowSpider spider = new WidowSpider(killer, this.npcRegistry);
		spider.spawn(player.getLocation());
		this.playerSpiders.get(killer).add(spider);
		this.spidersWidowSpiders.put((Spider) spider.getNpc().getEntity(), spider);
	}

	@Override
	public void handleHit(BPlayer damager, BPlayer victim) {
		if (damager.getBClass() == BClasses.WIDOW) {
			this.playerSpiders.get(damager).forEach(spider -> spider.setTarget(victim));
		}
	}

	@Override
	public void playerBecameThisClass(BPlayer bPlayer) {
		this.playerSpiders.put(bPlayer, new ArrayList<>());
	}

	@Override
	public void playerLeftThisClass(BPlayer bPlayer) {
		this.playerSpiders.remove(bPlayer);
	}

}
