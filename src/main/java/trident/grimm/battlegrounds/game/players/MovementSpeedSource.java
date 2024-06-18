package trident.grimm.battlegrounds.game.players;

public record MovementSpeedSource(MovementSpeedSourceType type, double value) {

	public static enum MovementSpeedSourceType {
		ADDITIVE, // additive movement speed source.
		MULTIPLICATIVE, // multiplicative movement speed source
		STOP // stop = if this shows up anywhere in a player's internal list of
				// movementspeedsources, set the player's movement speed to 0.
	}

}
