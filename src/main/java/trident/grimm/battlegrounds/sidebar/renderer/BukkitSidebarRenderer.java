package trident.grimm.battlegrounds.sidebar.renderer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.Component;
import trident.grimm.battlegrounds.sidebar.Sidebar;

public class BukkitSidebarRenderer implements SidebarRenderer {

	@SuppressWarnings("deprecation")
	private static final String[] COLOR_CODES = Arrays
			.stream(org.bukkit.ChatColor.values())
			.map(Object::toString)
			.toArray(String[]::new);

	private final String rendererId = Integer.toHexString(ThreadLocalRandom.current().nextInt());
	private final Pattern pattern = Pattern.compile(rendererId + "_(\\d\\d?)");
	private final boolean zeroScores;

	public BukkitSidebarRenderer() {
		this(false);
	}

	public BukkitSidebarRenderer(boolean zeroScores) {
		this.zeroScores = zeroScores;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void add(Player player, Sidebar sidebar) {
		Scoreboard scoreboard = player.getScoreboard();
		Objective objective = scoreboard.registerNewObjective("sidebar", "", sidebar.getTitle().apply(player));
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}

	@Override
	public void remove(Player player, Sidebar sidebar) {
		Scoreboard scoreboard = player.getScoreboard();
		Objective objective = scoreboard.getObjective("sidebar");
		if (objective == null)
			return;
		objective.unregister();
	}

	@Override
	public void render(Sidebar sidebar) {
		if (sidebar.getTitle() == null || sidebar.getLineFunction() == null)
			return;

		// I don't want this!
		for (Player viewer : sidebar.getViewers()) {
			renderForPlayer(viewer, sidebar);
		}
	}

	@Override
	public void renderForPlayer(Player player, Sidebar sidebar) {
		if (!sidebar.getViewers().contains(player))
			return;

		List<Component> lines = List.copyOf(sidebar.getLineFunction().apply(player));

		renderTitle(player, sidebar.getTitle().apply(player));
		cleanup(player, lines.size());
		for (int i = 0; i < lines.size(); i++) {
			renderLine(player, i, lines.get(i));
		}
	}

	private void renderLine(Player player, int index, Component line) {
		Scoreboard scoreboard = player.getScoreboard();

		Objective objective = player.getScoreboard().getObjective("sidebar");
		if (objective == null)
			return;

		Team team = scoreboard.getTeam(rendererId + "_" + index);

		if (team == null) {
			team = scoreboard.registerNewTeam(rendererId + "_" + index);
		}

		String entry = COLOR_CODES[index];
		if (!team.hasEntry(entry)) {
			team.addEntry(entry);
		}

		team.prefix(line);

		objective.getScore(entry).setScore(zeroScores ? 0 : (16 - index));
	}

	private void renderTitle(Player player, Component title) {
		Objective objective = player.getScoreboard().getObjective("sidebar");
		if (objective == null)
			return;
		objective.displayName(title);
	}

	private void cleanup(Player player, int size) {
		Scoreboard scoreboard = player.getScoreboard();

		for (Team team : scoreboard.getTeams()) {
			String name = team.getName();
			Matcher matcher = pattern.matcher(name);
			if (!matcher.matches())
				continue;
			if (Integer.parseInt(matcher.group(1)) < size)
				continue;
			for (String entry : team.getEntries()) {
				scoreboard.resetScores(entry);
			}
			team.unregister();
		}
	}
}
