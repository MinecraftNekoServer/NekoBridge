package neko.nekoBridge;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {
    
    private final NekoBridge plugin;
    
    public SpawnCommand(NekoBridge plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个指令！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("setspawn")) {
            // 检查玩家是否有权限
            if (!player.hasPermission("neko.bridge.setspawn")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个指令！");
                return true;
            }
            
            // 获取玩家当前位置
            Location location = player.getLocation();
            
            // 保存到配置文件
            plugin.getConfig().set("spawn.world", location.getWorld().getName());
            plugin.getConfig().set("spawn.x", location.getX());
            plugin.getConfig().set("spawn.y", location.getY());
            plugin.getConfig().set("spawn.z", location.getZ());
            plugin.getConfig().set("spawn.yaw", location.getYaw());
            plugin.getConfig().set("spawn.pitch", location.getPitch());
            plugin.saveConfig();
            
            player.sendMessage(ChatColor.GREEN + "全局出生点已设置在: " + 
                             ChatColor.YELLOW + location.getWorld().getName() + 
                             ChatColor.GOLD + " X:" + String.format("%.2f", location.getX()) + 
                             " Y:" + String.format("%.2f", location.getY()) + 
                             " Z:" + String.format("%.2f", location.getZ()));
            
            return true;
        }
        
        return false;
    }
}