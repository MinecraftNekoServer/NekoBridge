package NekoBridging.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import NekoBridging.Man;

public class SetSpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§b§l搭路练习 §7>> §c此命令用于设置全局默认出生点, 仅玩家可以执行.");
            return true;
        }

        if (!sender.hasPermission("bridginganalyzer.setspawn")) {
            sender.sendMessage("§b§l搭路练习 §7>> §c你没有权限执行此命令.");
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();

        // 保存全局出生点到配置或静态变量
        Man.setGlobalSpawnPoint(location);

        sender.sendMessage("§b§l搭路练习 §7>> §a全局默认出生点已设置为当前位置: §f" + 
            String.format("%.2f, %.2f, %.2f", location.getX(), location.getY(), location.getZ()));

        // 提醒所有在线玩家
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("bridginganalyzer.setspawn.notify")) {
                onlinePlayer.sendMessage("§b§l搭路练习 §7>> §e全局默认出生点已被 §6" + player.getName() + " §e设置为: §f" + 
                    String.format("%.2f, %.2f, %.2f", location.getX(), location.getY(), location.getZ()));
            }
        }

        return true;
    }
}