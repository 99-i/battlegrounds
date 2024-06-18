package trident.grimm.battlegrounds.game;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.game.players.BPlayer;
import trident.grimm.battlegrounds.sidebar.SidebarManager;
import trident.grimm.battlegrounds.util.FastUtil;

public class CooldownManager {

	private static @Getter CooldownManager instance = new CooldownManager();
	private ExecutorService stopThreadPool;

	private CooldownManager() {
		stopThreadPool = Executors.newCachedThreadPool();
	}

	protected HashMap<BPlayer, ArrayList<CooldownEntry>> cooldowns = new HashMap<>();

	public static class CooldownEntry {
		private TextComponent _name;
		private LocalTime _endTime;
		private LocalTime _startTime;
		private Future<?> stopThread;
		private BPlayer bPlayer;
		private boolean ended = false;

		public CooldownEntry(TextComponent name, int durationMilliseconds, BPlayer bPlayer) {
			this._name = name;
			this._startTime = LocalTime.now();
			this._endTime = this._startTime.plus(durationMilliseconds, ChronoUnit.MILLIS);
			this.bPlayer = bPlayer;
			this.initiateStopThread();
		}

		private void initiateStopThread() {
			if (this.stopThread != null) {
				this.stopThread.cancel(true);
			}

			Duration duration = Duration.between(LocalTime.now(), _endTime);
			final long milliseconds = duration.toMillis();
			this.stopThread = CooldownManager.getInstance().stopThreadPool.submit(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(milliseconds + 10);
						CooldownEntry.this.ended = true;
						CooldownManager.getInstance().cooldownEnded(CooldownEntry.this);
						SidebarManager.getInstance().updatePlayer(bPlayer.getPlayer());
						CooldownEntry.this.bPlayer.cooldownEnded(_name);
					} catch (InterruptedException e) {
						// cancelled.
					}
				}

			});
		}

		public TextComponent name() {
			return this._name;
		}

		public LocalTime _startTime() {
			return this._startTime;
		}

		public double secondsLeft() {
			if (this.over())
				return 0;

			Duration duration = Duration.between(this._endTime, LocalTime.now());

			return -(duration.getSeconds() + duration.getNano() / 1e9);
		}

		public boolean over() {
			return LocalTime.now().isAfter(this._endTime);
		}

		public void setCooldown(int durationMilliseconds) {
			if (!this.ended) {
				this._endTime = LocalTime.now().plus(durationMilliseconds, ChronoUnit.MILLIS);
				this.initiateStopThread();
			}
		}
	}

	public List<Component> getPlayerCooldownsList(BPlayer bPlayer) {
		List<Component> components = new ArrayList<>();

		ArrayList<CooldownEntry> entries = getEntries(bPlayer);

		for (CooldownEntry entry : entries) {
			components.add(entry.name().appendSpace()
					.append(Component.text(FastUtil.doubleToString(entry.secondsLeft()), TextColor.color(0, 255, 255)))
					.append(Component.text('s', TextColor.color(0, 255, 255))));
		}

		return components;
	}

	// register a new cooldown.
	private void registerCooldown(BPlayer bPlayer, TextComponent name, int durationMilliseconds) {
		ArrayList<CooldownEntry> entries = getEntries(bPlayer);
		for (CooldownEntry entry : entries) {
			if (entry.name().equals(name)) {
				return;
			}
		}
		final CooldownEntry entry = new CooldownEntry(name, durationMilliseconds, bPlayer);

		entries.add(entry);
		SidebarManager.getInstance().updatePlayer(bPlayer.getPlayer());

	}

	private ArrayList<CooldownEntry> getEntries(BPlayer bPlayer) {
		if (this.cooldowns.containsKey(bPlayer)) {
			return this.cooldowns.get(bPlayer);
		} else {
			this.cooldowns.put(bPlayer, new ArrayList<CooldownEntry>());
			return this.cooldowns.get(bPlayer);
		}
	}

	// get the cooldown duration, in seconds. returns 0 if the cooldown doesn't
	// exist on the
	// player (e.g. its over or never existed)
	public double getCooldown(BPlayer bPlayer, TextComponent name) {
		ArrayList<CooldownEntry> entries = getEntries(bPlayer);
		double cooldown = 0;

		for (CooldownEntry entry : entries) {
			if (entry.name().equals(name)) {
				cooldown = entry.secondsLeft();
				break;
			}
		}

		return cooldown;
	}

	public boolean isCooldownOver(BPlayer bPlayer, TextComponent name) {
		ArrayList<CooldownEntry> entries = getEntries(bPlayer);

		for (CooldownEntry entry : entries) {
			if (entry.name().equals(name)) {
				if (entry.over()) {
					return true;
				} else {
					return false;
				}
			}
		}

		return true;
	}

	// set the cooldown duration to something else
	public void setCooldown(BPlayer bPlayer, TextComponent name, int durationMilliseconds) {
		ArrayList<CooldownEntry> entries = getEntries(bPlayer);
		boolean foundEntry = false;
		for (CooldownEntry entry : entries) {
			if (entry.name().equals(name)) {
				entry.setCooldown(durationMilliseconds);
				foundEntry = true;
				break;
			}
		}
		if (!foundEntry) {
			registerCooldown(bPlayer, name, durationMilliseconds);
		}
		SidebarManager.getInstance().updatePlayer(bPlayer.getPlayer());
	}

	protected void cooldownEnded(CooldownEntry entry) {
		ArrayList<CooldownEntry> entries = getEntries(entry.bPlayer);
		entries.remove(entry);
	}

	public void shutdownCooldowns() {
		this.stopThreadPool.shutdownNow();
	}
}
