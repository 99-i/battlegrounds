package trident.grimm.battlegrounds.config;

import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import net.kyori.adventure.text.TextComponent;
import trident.grimm.battlegrounds.BChat;

public class Parameters {
	private HashMap<String, Object> internalValues;

	public Parameters(HashMap<String, Object> internalValues) {
		this.internalValues = internalValues;
	}

	public int getInt(String str) {
		return (Integer) internalValues.get(str);
	}

	public double getDouble(String str) {
		Object obj = internalValues.get(str);
		if (obj instanceof Integer) {
			return ((Integer) obj).doubleValue();
		}
		return (double) (internalValues.get(str));
	}

	public TextComponent getString(String str) {
		if (!internalValues.containsKey(str)) {
			Bukkit.getLogger().log(Level.SEVERE, "TRIED TO GET TEXTCOMPONENT OF NULL (NONEXISTENT) STRING");
		}
		return (TextComponent) BChat.getMiniMessage().deserialize((String) internalValues.get(str));
	}

	public String getRawString(String str) {
		return (String) internalValues.get(str);
	}
}
