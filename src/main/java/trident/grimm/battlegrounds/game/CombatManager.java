package trident.grimm.battlegrounds.game;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.BConfig;
import trident.grimm.battlegrounds.game.effects.BEffects;
import trident.grimm.battlegrounds.game.effects.strength.StrengthEffect.StrengthEffectSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.BPlayer.BDamageType;
import trident.grimm.battlegrounds.util.WorldManager;

// manages PVP comb
public class CombatManager implements Listener {

	private static @Getter CombatManager instance = new CombatManager();

	private App app;

	public CombatManager() {
		app = App.getInstance();
		disableSweepParticles();
	}

	// disables the sweep particles from sweep enchantments.
	private void disableSweepParticles() {
		app.getProtocolManager().addPacketListener(
				new PacketAdapter(app, ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_PARTICLES) {
					@Override
					public void onPacketSending(PacketEvent event) {
						Particle particle = event.getPacket().getNewParticles().read(0).getParticle();
						if (particle.equals(Particle.SWEEP_ATTACK) || particle.equals(Particle.DAMAGE_INDICATOR)) {
							event.setCancelled(true);
						}

					}
				});
	}

	// disables attack speed and also check if the player equipped armor with right
	// click, to refresh defenses
	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event) {
		event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(100);

		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getItem() == null)
				return;
			if (isArmor(event.getItem().getType())) {
				new BukkitRunnable() {

					@Override
					public void run() {
						BPlayer.getBPlayer(event.getPlayer()).refreshDefenseChanges();
					}

				}.runTask(app);

			}

		}

	}

	// disable ender pearl cooldown
	@EventHandler
	public void onProjectileLaunch(ProjectileLaunchEvent event) {
		Projectile projectile = event.getEntity();
		if (!(projectile instanceof EnderPearl))
			return;
		ProjectileSource projectileSource = projectile.getShooter();
		if (!(projectileSource instanceof Player))
			return;
		final Player player = (Player) projectileSource;

		new BukkitRunnable() {

			@Override
			public void run() {
				player.setCooldown(Material.ENDER_PEARL, 0);
			}

		}.runTask(app);

	}

	// fire damage is handled in BEffect.FIRE
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			if (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

		if (event.getEntity() instanceof Player) {
			event.setDamage(0);
		}
		if (!GameManager.getInstance().isStarted()) {
			event.setCancelled(true);
			return;
		}

		if (!(event.getDamager() instanceof Player))
			return;
		if (!(event.getEntity() instanceof Player))
			return;

		if (event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
			event.setCancelled(true);
			return;
		}
		Player damager = (Player) event.getDamager();
		Player victim = (Player) event.getEntity();

		WorldManager worldManager = WorldManager.getInstance();
		if (event.getDamager().getWorld() != worldManager.getMapWorld()
				|| event.getEntity().getWorld() != worldManager.getMapWorld()) {
			event.setCancelled(true);
			return;
		}

		handleHit(BPlayer.getBPlayer(damager), BPlayer.getBPlayer(victim), damager.getInventory().getItemInMainHand());
	}

	public static double getPlayerDamage(BPlayer damager, ItemStack itemInHand) {
		BConfig config = App.getInstance().getBConfig();
		Integer ench = itemInHand.getEnchantments().get(Enchantment.SHARPNESS);
		int sharpnessEnchantment = ench == null ? 0 : ench;
		StrengthEffectSettings strengthSettings = (StrengthEffectSettings) damager.getEffectSettings(BEffects.STRENGTH,
				StrengthEffectSettings.class);
		int strengthLevel = strengthSettings == null ? 0 : strengthSettings.mult();

		return getDamage(itemInHand.getType(),
				config.getSharpnessMultipliers().get(sharpnessEnchantment),
				config.getStrengthMultipliers().get(strengthLevel));
	}

	public static double getDamage(Material itemInHand, double sharpnessMultiplier, double strengthLevel) {
		BConfig config = App.getInstance().getBConfig();
		double damage = config.getDefaultDamage();
		if (config.getSwordDamages().containsKey(itemInHand)) {
			damage = config.getSwordDamages().get(itemInHand);
		}

		return damage * sharpnessMultiplier * strengthLevel;
	}

	// called when a player hits another player in melee combat.
	// TODO: bow combat
	private void handleHit(BPlayer damager, BPlayer victim, ItemStack item) {
		damager.getBClass().handleHit(damager, victim);
		victim.getBClass().handleHit(damager, victim);

		victim.damage(getPlayerDamage(damager, item), damager, BDamageType.PHYSICAL);
	}

	// watches for armor equips and refresh defense changes.
	@EventHandler
	public void onInventoryClick(final InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player))
			return;

		if (event.getCurrentItem() == null)
			return;

		if (isArmor(event.getCurrentItem().getType()) || isArmor(event.getCursor().getType())) {
			new BukkitRunnable() {
				@Override
				public void run() {
					BPlayer.getBPlayer((Player) event.getWhoClicked()).refreshDefenseChanges();
				}
			}.runTask(app);
		}
	}

	private boolean isArmor(Material material) {
		return EnchantmentTarget.ARMOR.includes(material);
	}

}
