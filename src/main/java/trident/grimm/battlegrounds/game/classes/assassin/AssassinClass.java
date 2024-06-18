package trident.grimm.battlegrounds.game.classes.assassin;

import java.awt.Color;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.config.Parameters;
import trident.grimm.battlegrounds.game.CooldownManager;
import trident.grimm.battlegrounds.game.ability.Ability;
import trident.grimm.battlegrounds.game.ability.Ability.AbilityType;
import trident.grimm.battlegrounds.game.classes.BClass;
import trident.grimm.battlegrounds.game.classes.BClassUtil;
import trident.grimm.battlegrounds.game.classes.assassin.deceptor.DeceptorNPC;
import trident.grimm.battlegrounds.game.effects.BEffects;
import trident.grimm.battlegrounds.game.effects.stealth.StealthEffect.StealthEffectSettings;
import trident.grimm.battlegrounds.game.effects.subdued.SubduedEffect.SubduedEffectSettings;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.items.ItemUtil;
import trident.grimm.battlegrounds.util.Pair;

public class AssassinClass extends BClass {
	private static Parameters parameters = new Parameters(
			App.getInstance().getBConfig().getClassEntries().get("assassin").getParameters());

	private final static int STEALTH_DURATION_TICKS = parameters.getInt("stealth_duration_ticks");

	private final static TextComponent STEALTH_COOLDOWN_NAME = parameters.getString("stealth_cooldown_name");
	private final static int STEALTH_COOLDOWN_MS = parameters.getInt("stealth_cooldown_ms");

	private final static TextComponent SUBDUE_COOLDOWN_NAME = parameters.getString("subdue_cooldown_name");
	private final static int SUBDUE_COOLDOWN_MS = parameters.getInt("subdue_cooldown_ms");

	private final static TextComponent DECEPTOR_COOLDOWN_NAME = parameters.getString("deceptor_cooldown_name");

	private final static int DECEPTOR_COOLDOWN_MS = parameters.getInt("deceptor_cooldown_ms");

	private final static String DECEPTOR_ABILITY_ID = parameters.getRawString("deceptor_ability_id");
	private final static String CONFIG_SECTION = "assassin";

	private NPCRegistry npcRegistry;
	private HashMap<BPlayer, DeceptorNPC> deceptorNPCs;

	public AssassinClass() {
		super(Material.GOLDEN_SWORD, Component.text("Assassin", TextColor.color(127, 127, 127)), CONFIG_SECTION);
		this.registerFKeyAbility();
		this.registerItemAbility(DECEPTOR_ABILITY_ID);

		this.npcRegistry = CitizensAPI.createInMemoryNPCRegistry("ASSASSIN_NPCS");
		this.deceptorNPCs = new HashMap<>();
	}

	@Override
	public void abilityUsed(BPlayer bPlayer, Ability ability) {
		if (ability.getAbilityType() == AbilityType.F_KEY) {
			// player used subdue
			playerSubdue(bPlayer);
		} else if (ability.getAbilityType() == AbilityType.ITEM) {
			// player used deceptor.
			playerDeceptor(bPlayer);
		}
	}

	public void removeNpc(BPlayer owner) {
		this.deceptorNPCs.remove(owner);
	}

	public static BPlayer getOwnerOfNPC(Player npc) {
		String ownerUUID = ItemUtil.getValue(npc.getPersistentDataContainer(), "deceptor-owner-uuid");

		if (ownerUUID == null)
			return null;

		Entity owner = Bukkit.getEntity(UUID.fromString(ownerUUID));

		if (!(owner instanceof Player))
			return null;

		return BPlayer.getBPlayer((Player) owner);
	}

	public void npcWasDamaged(Player damager, Player npc) {
		BPlayer damagerBPlayer = BPlayer.getBPlayer(damager);

		String ownerUUID = ItemUtil.getValue(npc.getPersistentDataContainer(),
				"deceptor-owner-uuid");

		if (ownerUUID == null)
			return;

		Entity owner = Bukkit.getEntity(UUID.fromString(ownerUUID));

		if (!(owner instanceof Player))
			return;

		BPlayer ownerBPlayer = BPlayer.getBPlayer((Player) owner);

		if (ownerBPlayer.getTeam() == damagerBPlayer.getTeam())
			return;

		deceptorNPCs.get(ownerBPlayer).explode();
	}

	private void playerDeceptor(BPlayer bPlayer) {
		if (this.deceptorNPCs.containsKey(bPlayer)) {
			return;
		}
		if (!CooldownManager.getInstance().isCooldownOver(bPlayer, DECEPTOR_COOLDOWN_NAME)) {
			return;
		}

		DeceptorNPC npc = new DeceptorNPC(bPlayer, this.npcRegistry);
		this.deceptorNPCs.put(bPlayer, npc);
		npc.start();
		CooldownManager.getInstance().setCooldown(bPlayer, DECEPTOR_COOLDOWN_NAME, DECEPTOR_COOLDOWN_MS);
		removeStealthIfPlayerHasStealth(bPlayer);
	}

	private void playerSubdue(BPlayer bPlayer) {
		if (!CooldownManager.getInstance().isCooldownOver(bPlayer, SUBDUE_COOLDOWN_NAME)) {
			return;
		}

		// todo!: hardcoded numbers
		Pair<Entity, Vector> target = BClassUtil.getTargetEntity(bPlayer.getPlayer(), 4, 0.4);
		if (target == null)
			return;
		if (target.first() == null)
			return;
		if (target.first().getType() != EntityType.PLAYER)
			return;

		BPlayer targetPlayer = BPlayer.getBPlayer((Player) target.first());
		if (targetPlayer.getTeam() == bPlayer.getTeam())
			return;

		Location origin = ((Player) bPlayer.getPlayer()).getEyeLocation();
		Location end = target.second().toLocation(origin.getWorld());

		XParticle.line(origin, end, 0.5f, ParticleDisplay.colored(origin, new Color(255, 100, 100), 1));

		// todo!: hardcoded effect duration.
		targetPlayer.giveEffect(BEffects.SUBDUED, 100, new SubduedEffectSettings(), SubduedEffectSettings.class);
		CooldownManager.getInstance().setCooldown(bPlayer, SUBDUE_COOLDOWN_NAME, SUBDUE_COOLDOWN_MS);

		removeStealthIfPlayerHasStealth(bPlayer);
	}

	@Override
	public void killedPlayer(BPlayer killer, BPlayer victim) {
		if (!CooldownManager.getInstance().isCooldownOver(killer, STEALTH_COOLDOWN_NAME))
			return;

		playerStealh(killer);
	}

	@Override
	public void cooldownEnded(BPlayer bPlayer, TextComponent cooldownName) {
		if (cooldownName == BEffects.STEALTH.getDisplayName()) {
			CooldownManager.getInstance().setCooldown(bPlayer, STEALTH_COOLDOWN_NAME, STEALTH_COOLDOWN_MS);
		}
	}

	@Override
	public void handleHit(BPlayer damager, BPlayer victim) {
		if (damager.getBClass() == this) {
			removeStealthIfPlayerHasStealth(damager);
		} else if (victim.getBClass() == this) {
			removeStealthIfPlayerHasStealth(victim);
		}
	}

	private void playerStealh(BPlayer player) {
		if (player.hasEffect(BEffects.STEALTH))
			return;

		player.giveEffect(BEffects.STEALTH, STEALTH_DURATION_TICKS, new StealthEffectSettings(),
				StealthEffectSettings.class);
	}

	private void removeStealthIfPlayerHasStealth(BPlayer bPlayer) {
		if (bPlayer.hasEffect(BEffects.STEALTH)) {
			bPlayer.removeEffect(BEffects.STEALTH);
			CooldownManager.getInstance().setCooldown(bPlayer, STEALTH_COOLDOWN_NAME, STEALTH_COOLDOWN_MS);
		}
	}

	// disable using eye of ender.
	// todo: this might not be the right place to implement it. not sure.
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack item = event.getItem();
			if (item != null && item.getType() == Material.ENDER_EYE) {
				event.setCancelled(true);
			}
		}
	}
}
