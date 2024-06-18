package trident.grimm.battlegrounds.game.classes.engineer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import trident.grimm.battlegrounds.game.StatusManager;
import trident.grimm.battlegrounds.game.players.BPlayer;

public enum MineStatus {

	ENABLED, READYING, DISABLED;

	// todo!: put this in config.
	private static final TextComponent MINE_ENABLED_STATUS = Component.text("MINE: ", TextColor.color(0, 255, 0))
			.append(Component.text("ENABLED", TextColor.color(50, 255, 50)));
	private static final TextComponent MINE_DISABLED_STATUS = Component.text("MINE: ", TextColor.color(0, 255, 0))
			.append(Component.text("DISABLED", TextColor.color(139, 0, 0)));
	private static final TextComponent MINE_READYING_STATUS = Component.text("MINE: ", TextColor.color(0, 255, 0))
			.append(Component.text("READYING", TextColor.color(139, 139, 139)));

	public static void setPlayerMineStatus(BPlayer bPlayer, MineStatus status) {
		removePlayerMineStatus(bPlayer);
		switch (status) {
			case READYING:
				StatusManager.getInstance().addPlayerStatus(bPlayer, MINE_READYING_STATUS);
				break;
			case ENABLED:
				StatusManager.getInstance().addPlayerStatus(bPlayer, MINE_ENABLED_STATUS);
				break;
			case DISABLED:
				StatusManager.getInstance().addPlayerStatus(bPlayer, MINE_DISABLED_STATUS);
				break;
		}
	}

	public static void removePlayerMineStatus(BPlayer bPlayer) {
		StatusManager.getInstance().removePlayerStatus(bPlayer, MINE_ENABLED_STATUS);
		StatusManager.getInstance().removePlayerStatus(bPlayer, MINE_READYING_STATUS);
		StatusManager.getInstance().removePlayerStatus(bPlayer, MINE_DISABLED_STATUS);
	}
}
