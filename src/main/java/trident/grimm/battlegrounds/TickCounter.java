package trident.grimm.battlegrounds;

import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;

import lombok.Getter;

public class TickCounter implements Listener {

	private @Getter AtomicInteger tickCount = new AtomicInteger(0);

	private static @Getter TickCounter instance = new TickCounter();

	private TickCounter() {
	}

	@EventHandler
	public void onServerTickEnd(ServerTickEndEvent event) {
		tickCount.set(event.getTickNumber());
	}

}
