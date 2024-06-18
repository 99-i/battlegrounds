package trident.grimm.battlegrounds.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

public class HealthbarUtil {

	// return a TextComponent with bars representing a health.
	// e.g., used in the sentry's pillager custom name
	public static TextComponent getBarredHealthbar(int numColumns, double health,
			TextColor redColor,
			TextColor grayColor) {
		final int numRed = (int) Math.ceil(((double) numColumns * health));
		final int numGray = numColumns - numRed;
		return Component.text()
				.append(Component.text("|".repeat(numRed), redColor))
				.append(Component.text("|".repeat(numGray), grayColor))
				.build();
	}

	public static TextComponent getBarredHealthbar(int numColumns, double health) {
		return getBarredHealthbar(numColumns, health, TextColor.color(255, 0, 0), TextColor.color(127, 127, 127));
	}

	public static String getBarredHealthbarLegacy(int numColumns, double health) {
		final int numRed = (int) Math.ceil(((double) numColumns * health));
		final int numGray = numColumns - numRed;

		return "&#ff0000" + "|".repeat(numRed) + "&#7f7f7f" + "|".repeat(numGray);
	}
}
