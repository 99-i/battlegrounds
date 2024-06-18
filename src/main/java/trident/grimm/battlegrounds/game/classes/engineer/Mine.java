package trident.grimm.battlegrounds.game.classes.engineer;

import java.util.Collection;
import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.game.classes.BClasses;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.game.players.BPlayer.BDamageType;
import trident.grimm.battlegrounds.nms.Hologram;
import trident.grimm.battlegrounds.util.FastUtil;

public class Mine {

	// todo!: put this in config/parameters.
	private static final int READYING_TICKS = 20 * 4;

	private @Getter BPlayer owner;
	private @Getter Location position;
	private @Getter @Setter boolean enabled = false;
	private @Getter Hologram readyingHologram;
	private @Getter int readyingTicksLeft;

	public Mine(BPlayer owner, Location position) {
		if (owner.getBClass() != BClasses.ENGINEER) {
			return;
		}
		this.owner = owner;
		this.position = position;
	}

	// detonate the mine.
	public void detonate() {
		if (!this.enabled)
			return;

		position.getWorld().spawnParticle(Particle.EXPLOSION, this.position, 4);
		DustOptions dustOptions = new DustOptions(Color.fromRGB(128, 5, 0), 10);
		position.getWorld().spawnParticle(Particle.DUST, this.position, 40, 2, 2, 2, dustOptions);

		Collection<Entity> entities = this.position.getWorld().getNearbyEntities(this.position, 3, 3, 3);

		BTeam ownerTeam = owner.getTeam();
		for (Entity entity : entities) {
			if (!(entity instanceof Player))
				continue;
			BPlayer bPlayer = BPlayer.getBPlayer((Player) entity);
			if (ownerTeam != bPlayer.getTeam()) {
				Location location = ((Player) entity).getEyeLocation();
				Vector kbVector = location.toVector().subtract(this.position.toVector()).multiply(0.3);
				bPlayer.damage(50, owner, BDamageType.PHYSICAL);
				bPlayer.getPlayer().setVelocity(kbVector);
			}
		}
	}

	public void ready() {
		this.readyingTicksLeft = READYING_TICKS;
		Location newPosition = this.position.clone().add(new Vector(0, 1.3, 0));
		this.readyingHologram = new Hologram(newPosition, prettyPrintTicksLeft(), false);

		this.readyingHologram.addPlayer(this.owner.getPlayer());

		int interval = 1;

		Mine.particleEffect(this.position);

		new BukkitRunnable() {
			@Override
			public void run() {
				readyingHologram.setText(prettyPrintTicksLeft());

				readyingTicksLeft -= interval;
				if (readyingTicksLeft <= 0) {
					finishedReadying();
					this.cancel();
				}
			}

		}.runTaskTimer(App.getInstance(), 0, interval);
	}

	private void finishedReadying() {
		MineStatus.setPlayerMineStatus(owner, MineStatus.ENABLED);
		setEnabled(true);
		readyingHologram.destroy();
		readyingHologram = null;
	}

	private TextComponent prettyPrintTicksLeft() {
		return Component.text("READYING MINE...")
				.append(Component.text(FastUtil.doubleToString((double) this.readyingTicksLeft / 20d)))
				.append(Component.text("s"));
	}

	private static void particleEffect(Location location) {
		int interval = 1;
		new BukkitRunnable() {
			Random rand = new Random();
			ExponentialDistribution distribution = new ExponentialDistribution(0.3);
			int numTicks = 0;

			@Override
			public void run() {
				int numNewParticles = rand.nextInt(15);

				for (int i = 0; i < numNewParticles; i++) {
					double r = distribution.sample();
					double theta = rand.nextDouble() * Math.PI * 2;
					double upTheta = rand.nextDouble() * Math.PI / 4;
					double dx = Math.cos(theta) * r;
					double dz = Math.sin(theta) * r;
					double dy = Math.sin(upTheta) * r;
					DustOptions dustOptions = new DustOptions(Color.fromRGB(127, 127, 127), 1.0F);
					location.getWorld().spawnParticle(Particle.DUST, location.getX() + dx, location.getY() + dy,
							location.getZ() + dz, 20, dustOptions);
				}
				numTicks += interval;
				if (numTicks >= READYING_TICKS) {
					this.cancel();
				}
			}
		}.runTaskTimer(App.getInstance(), 0, interval);
	}

}
