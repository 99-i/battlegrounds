package trident.grimm.battlegrounds.game.phases;

//enums that determine what is unlocked.
public enum PhaseUnlock {
	MINE_NEXUS(2),
	SPAWN_DIAMONDS(3);

	private boolean unlocked = false;
	private int unlockPhase;

	PhaseUnlock(int unlockPhase) {
		this.unlockPhase = unlockPhase;
	}

	public boolean isUnlocked() {
		return this.unlocked;
	}

	public void setUnlocked(boolean unlocked) {
		this.unlocked = unlocked;
	}

	public int getUnlockPhase() {
		return this.unlockPhase;
	}

}
