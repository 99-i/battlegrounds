package trident.grimm.battlegrounds.game.effects;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.CooldownManager;
import trident.grimm.battlegrounds.game.HealthManager;
import trident.grimm.battlegrounds.game.effects.regeneration.RegenerationEffect.RegenerationEffectSettings;
import trident.grimm.battlegrounds.game.effects.slowness.SlownessEffect.SlownessEffectSettings;
import trident.grimm.battlegrounds.game.effects.speed.SpeedEffect.SpeedEffectSettings;
import trident.grimm.battlegrounds.game.effects.strength.StrengthEffect.StrengthEffectSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;

// used so that players drinking potions get BEffects instead of normal spigot
// effects.
public class PotionsManager implements Listener {

	private static @Getter PotionsManager instance = new PotionsManager();

	private App app;

	static final TextComponent GAPPLE_COOLDOWN_NAME = Component.text().color(TextColor.color(222, 200, 4))
			.append(Component.text("GAPPLE")).build();

	private PotionsManager() {
		this.app = App.getInstance();

		// this.app.getProtocolManager()
		// .addPacketListener(new PacketAdapter(this.app,
		// PacketType.Play.Server.ENTITY_EFFECT) {
		// private List<Byte> includeBytes = Arrays.asList((byte) 24, (byte) 8, (byte)
		// 10);

		// @Override
		// public void onPacketSending(PacketEvent event) {
		// byte effectId = event.getPacket().getBytes().read(0);
		// if (!includeBytes.contains(effectId)) {
		// event.setCancelled(true);
		// }
		// }
		// });

	}

	@EventHandler
	public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
		BPlayer bPlayer = BPlayer.getBPlayer(event.getPlayer());
		Player player = event.getPlayer();
		if (event.getItem().getType() == Material.POTION) {

			PotionMeta potionMeta = (PotionMeta) event.getItem().getItemMeta();
			PotionType type = potionMeta.getBasePotionType();

			switch (type) {
				case LONG_REGENERATION: {
					RegenerationEffectSettings settings = new RegenerationEffectSettings(1);
					bPlayer.giveEffect(BEffects.REGENERATION, 90 * 20, settings, RegenerationEffectSettings.class);
				}
					break;
				case STRONG_REGENERATION: {
					RegenerationEffectSettings settings = new RegenerationEffectSettings(2);
					bPlayer.giveEffect(BEffects.REGENERATION, 22 * 20, settings, RegenerationEffectSettings.class);
				}
					break;
				case REGENERATION: {
					RegenerationEffectSettings settings = new RegenerationEffectSettings(1);
					bPlayer.giveEffect(BEffects.REGENERATION, 45 * 20, settings, RegenerationEffectSettings.class);
				}
					break;
				case LONG_SLOWNESS: {
					SlownessEffectSettings settings = new SlownessEffectSettings(null, 20);
					bPlayer.giveEffect(BEffects.SLOWNESS, 240 * 20, settings, SlownessEffectSettings.class);
				}
					break;
				case STRONG_SLOWNESS: {
					SlownessEffectSettings settings = new SlownessEffectSettings(null, 40);
					bPlayer.giveEffect(BEffects.SLOWNESS, 10 * 20, settings, SlownessEffectSettings.class);
				}
					break;
				case SLOWNESS: {
					SlownessEffectSettings settings = new SlownessEffectSettings(null, 20);
					bPlayer.giveEffect(BEffects.SLOWNESS, 90 * 20, settings, SlownessEffectSettings.class);
				}
					break;
				case LONG_SWIFTNESS: {
					SpeedEffectSettings settings = new SpeedEffectSettings(20);
					bPlayer.giveEffect(BEffects.SPEED, 480 * 20, settings, SpeedEffectSettings.class);
				}
					break;
				case STRONG_SWIFTNESS: {
					SpeedEffectSettings settings = new SpeedEffectSettings(40);
					bPlayer.giveEffect(BEffects.SPEED, 90 * 20, settings, SpeedEffectSettings.class);
				}
					break;
				case SWIFTNESS: {
					SpeedEffectSettings settings = new SpeedEffectSettings(20);
					bPlayer.giveEffect(BEffects.SPEED, 180 * 20, settings, SpeedEffectSettings.class);
				}
					break;
				case LONG_STRENGTH: {
					StrengthEffectSettings settings = new StrengthEffectSettings(1);
					bPlayer.giveEffect(BEffects.STRENGTH, 480 * 20, settings, StrengthEffectSettings.class);
				}
					break;
				case STRONG_STRENGTH: {
					StrengthEffectSettings settings = new StrengthEffectSettings(2);
					bPlayer.giveEffect(BEffects.STRENGTH, 90 * 20, settings, StrengthEffectSettings.class);
				}
					break;
				case STRENGTH: {
					StrengthEffectSettings settings = new StrengthEffectSettings(1);
					bPlayer.giveEffect(BEffects.STRENGTH, 180 * 20, settings, StrengthEffectSettings.class);
				}
					break;
				default:
					bPlayer.getPlayer().sendMessage(
							Component.text("This potion is not supported yet! Contact devs.", NamedTextColor.DARK_RED));
					break;
			}

			new BukkitRunnable() {
				@Override
				public void run() {
					for (PotionEffect effect : player.getActivePotionEffects()) {
						player.removePotionEffect(effect.getType());
					}
				}
			}.runTask(app);

		} else if (event.getItem().getType() == Material.ENCHANTED_GOLDEN_APPLE) {
			ItemStack eaten = player.getInventory().getItemInMainHand();
			int amount = eaten.getAmount();
			eaten.setType(Material.AIR);
			player.getInventory().setItemInMainHand(eaten);
			eaten.setType(Material.ENCHANTED_GOLDEN_APPLE);
			eaten.setAmount(amount);
			player.getInventory().setItemInMainHand(eaten);
			new BukkitRunnable() {
				@Override
				public void run() {
					for (PotionEffect effect : player.getActivePotionEffects()) {
						player.removePotionEffect(effect.getType());
					}
				}
			}.runTask(app);
			playerGappled(player);
		}
	}

	private void playerGappled(Player player) {
		BPlayer bPlayer = BPlayer.getBPlayer(player);
		CooldownManager cooldownManager = CooldownManager.getInstance();

		if (!cooldownManager.isCooldownOver(bPlayer, GAPPLE_COOLDOWN_NAME)) {
			player.sendMessage(Component.text("You are still on gapple cooldown!", TextColor.color(255, 0, 0)));
			return;
		}

		ItemStack hand = player.getInventory().getItemInMainHand();
		hand.setAmount(hand.getAmount() - 1);
		player.getInventory().setItemInMainHand(hand);

		double healthAdd = 200;
		bPlayer.setMaxHealth(bPlayer.getMaxHealth() + healthAdd);
		bPlayer.setHealth(bPlayer.getMaxHealth());
		HealthManager.getInstance().updatePlayerHealth(player);
		new BukkitRunnable() {

			@Override
			public void run() {
				double percent = bPlayer.getHealth() / bPlayer.getMaxHealth();
				bPlayer.setMaxHealth(bPlayer.getMaxHealth() - healthAdd);
				bPlayer.setHealth(bPlayer.getMaxHealth() * percent);
				HealthManager.getInstance().updatePlayerHealth(player);
			}

		}.runTaskLater(app, 100);

		cooldownManager.setCooldown(bPlayer, GAPPLE_COOLDOWN_NAME, 100);
	}

	// @EventHandler
	// public void onPotionSplash(PotionSplashEvent event) {
	// Collection<LivingEntity> affectedEntities = event.getAffectedEntities();
	// new BukkitRunnable() {
	// @Override
	// public void run() {
	// for (LivingEntity livingEntity : affectedEntities) {
	// if (livingEntity instanceof Player) {
	// Player player = (Player) livingEntity;
	// BPlayer.getBPlayer(player).convertMinecraftEffectsToBEffects();
	// }
	// }
	// }
	// }.runTask(app);
	// }

}
