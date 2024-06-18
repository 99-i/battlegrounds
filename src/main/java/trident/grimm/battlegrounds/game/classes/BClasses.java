package trident.grimm.battlegrounds.game.classes;

import trident.grimm.battlegrounds.game.classes.acrobat.AcrobatClass;
import trident.grimm.battlegrounds.game.classes.assassin.AssassinClass;
import trident.grimm.battlegrounds.game.classes.engineer.EngineerClass;
import trident.grimm.battlegrounds.game.classes.guard.GuardClass;
import trident.grimm.battlegrounds.game.classes.vampire.VampireClass;
import trident.grimm.battlegrounds.game.classes.widow.WidowClass;

public class BClasses {
	public static final AcrobatClass ACROBAT = new AcrobatClass();
	public static final VampireClass VAMPIRE = new VampireClass();
	public static final WidowClass WIDOW = new WidowClass();
	public static final EngineerClass ENGINEER = new EngineerClass();
	public static final AssassinClass ASSASSIN = new AssassinClass();
	public static final GuardClass GUARD = new GuardClass();
	public static final BClass[] values = { ACROBAT, VAMPIRE, WIDOW, ENGINEER, ASSASSIN, GUARD };

	public static String stringValueOf(BClass bClass) {
		if (bClass == ACROBAT) {
			return "ACROBAT";
		} else if (bClass == VAMPIRE) {
			return "VAMPIRE";
		} else if (bClass == WIDOW) {
			return "WIDOW";
		} else if (bClass == ENGINEER) {
			return "ENGINEER";
		} else if (bClass == ASSASSIN) {
			return "ASSASSIN";
		} else if (bClass == GUARD) {
			return "GUARD";
		}
		return null;
	}

	public static BClass valueOf(String value) {
		switch (value) {
			case "ACROBAT":
				return ACROBAT;
			case "VAMPIRE":
				return VAMPIRE;
			case "WIDOW":
				return WIDOW;
			case "ENGINEER":
				return ENGINEER;
			case "ASSASSIN":
				return ASSASSIN;
			case "GUARD":
				return GUARD;
		}
		return null;
	}
}
