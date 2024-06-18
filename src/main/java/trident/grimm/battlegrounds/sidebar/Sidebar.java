package trident.grimm.battlegrounds.sidebar;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableSet;

import net.kyori.adventure.text.Component;
import trident.grimm.battlegrounds.sidebar.renderer.SidebarRenderer;

public class Sidebar {

	private final Set<Player> viewers = new HashSet<>();
	private final SidebarRenderer sidebarRenderer;
	private Function<Player, Component> title;
	private Function<Player, Collection<Component>> lineFunction;

	public Sidebar(SidebarRenderer sidebarRenderer) {
		this.sidebarRenderer = sidebarRenderer;
	}

	public void add(Player player) {
		viewers.add(player);
		sidebarRenderer.add(player, this);
	}

	public void add(Player... players) {
		for (Player player : players) {
			add(player);
		}
	}

	public Set<Player> getViewers() {
		return ImmutableSet.copyOf(viewers);
	}

	public Function<Player, Component> getTitle() {
		return title;
	}

	public Function<Player, Collection<Component>> getLineFunction() {
		return lineFunction;
	}

	public void remove(Player player) {
		viewers.remove(player);
		sidebarRenderer.remove(player, this);
	}

	public void remove(Player... players) {
		for (Player player : players) {
			remove(player);
		}
	}

	public void title(Function<Player, Component> title) {
		this.title = title;
	}

	public void lines(Function<Player, Collection<Component>> lineFunction) {
		this.lineFunction = lineFunction;
	}

	public void render() {
		this.sidebarRenderer.render(this);
	}

	public void renderForPlayer(Player player) {
		this.sidebarRenderer.renderForPlayer(player, this);
	}
}
