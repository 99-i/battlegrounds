package trident.grimm.battlegrounds.game.phases;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import trident.grimm.battlegrounds.App;
import trident.grimm.battlegrounds.game.BTeam;
import trident.grimm.battlegrounds.game.GameManager;
import trident.grimm.battlegrounds.sidebar.SidebarManager;

public class PhaseManager implements Listener {

	private static @Getter PhaseManager instance = new PhaseManager();
	final int SECONDS_PER_PHASE = 4;

	private BossBar phaseBar;
	private App app;

	private int secondsTowardsNextPhase = 0;
	private int currentPhase = 0;

	private boolean metVotingRequirements = false;

	public PhaseManager() {
		app = App.getInstance();
	}

	public void start() {
		this.setupPhaseBar();
		this.startVoteRequirementsRunnable();
	}

	private String getBossBarName() {
		switch (currentPhase) {
			case 0:
				return "Voting";
			default:
				return "Phase " + currentPhase;
		}
	}

	private void setBarName() {
		this.phaseBar.setTitle(getBossBarName());
	}

	// create BossBar for everyone.
	private void setupPhaseBar() {
		this.phaseBar = Bukkit.createBossBar(getBossBarName(), BarColor.RED, BarStyle.SOLID);
		this.setBarProgress();
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.phaseBar.addPlayer(player);
		}
	}

	// set the bar's progress depending on phase progression
	private void setBarProgress() {
		double progress = ((double) secondsTowardsNextPhase) / ((double) SECONDS_PER_PHASE);
		if (currentPhase == 0) {
			progress = (1 - progress);
		}
		if (currentPhase == 5) {
			progress = 1;
		}
		this.phaseBar.setProgress(progress);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		this.phaseBar.addPlayer(event.getPlayer());
	}

	public void disable() {
		if (this.phaseBar != null) {
			this.phaseBar.removeAll();
		}
	}

	// watches to see if voting requirements are met.
	private void startVoteRequirementsRunnable() {
		new BukkitRunnable() {

			@Override
			public void run() {
				if (!metVotingRequirements) {
					checkVotingRequirements();
				} else {
					startPhaseRunnable();
					this.cancel();
				}
			}

		}.runTaskTimer(app, 0, 2L);
	}

	// called by the vote requirements runnable after voting requirements have been
	// met.
	private void startPhaseRunnable() {
		new BukkitRunnable() {

			@Override
			public void run() {
				if (currentPhase == 0) {
					if (!metVotingRequirements) {
						checkVotingRequirements();
					} else {
						secondsTowardsNextPhase++;
					}
				} else if (currentPhase < 5) {
					secondsTowardsNextPhase++;
				}
				if (secondsTowardsNextPhase >= SECONDS_PER_PHASE) {
					facilitatePhaseChange();
				}
				setBarProgress();
			}

		}.runTaskTimer(app, 0, 20L);
	}

	// check if the voting requirements are met. only has to meet voting
	// requirements once to start the countdown.
	private void checkVotingRequirements() {
		int numTotal = 0;
		for (BTeam team : BTeam.values()) {
			numTotal += team.getNumPlayers();
		}
		if (numTotal > 0) {
			metVotingRequirements = true;
		}
	}

	private void facilitatePhaseChange() {
		if (secondsTowardsNextPhase < SECONDS_PER_PHASE)
			return;
		secondsTowardsNextPhase = 0;
		currentPhase++;

		for (PhaseUnlock unlock : PhaseUnlock.values()) {
			if (unlock.getUnlockPhase() <= currentPhase) {
				unlock.setUnlocked(true);
			} else {
				unlock.setUnlocked(false);
			}
		}
		if (currentPhase == 1) {
			GameManager.getInstance().startGame();
		}

		SidebarManager.getInstance().update();
		setBarName();
	}

	public int getCurrentPhase() {
		return this.currentPhase;
	}
}
