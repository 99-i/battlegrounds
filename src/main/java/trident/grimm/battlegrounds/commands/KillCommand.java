package trident.grimm.battlegrounds.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import trident.grimm.battlegrounds.game.players.BPlayer;

// kill command. works by calling BPlayer#slashKill()
public class KillCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandName, String[] args) {

		if (!(sender instanceof Player)) {
			return true;
		}

		BPlayer bPlayer = BPlayer.getBPlayer((Player) sender);
		bPlayer.slashKill();

		return true;
	}
}
