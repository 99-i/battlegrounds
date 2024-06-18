package trident.grimm.battlegrounds.util;

public class FastUtil {
	public static String doubleToString(double fl) {
		int s = (int) fl;
		int fractional = (int) ((fl % 1) * 10);

		return new StringBuilder().append(s).append(".").append(fractional).toString();
	}
}
