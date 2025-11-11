package neko.nekoBridge;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class BridgeListener implements Listener {
    
    // 存储每个玩家最近放置方块的时间戳队列
    private final Map<UUID, Queue<Long>> blockPlaceTimes = new HashMap<>();
    private final NekoBridge plugin;
    
    public BridgeListener(NekoBridge plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 取消加入消息显示
        event.setJoinMessage(null);
        
        // 初始化玩家数据
        blockPlaceTimes.put(player.getUniqueId(), new LinkedList<>());
        
        // 给玩家提供无限沙石
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SURVIVAL) {
            ensureInfiniteSandstone(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 取消退出消息显示
        event.setQuitMessage(null);
        
        // 清理玩家数据
        UUID playerId = event.getPlayer().getUniqueId();
        blockPlaceTimes.remove(playerId);
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 确保玩家始终有沙石
        ensureInfiniteSandstone(player);
        
        long currentTime = System.currentTimeMillis();
        
        // 获取该玩家的放置时间队列
        Queue<Long> times = blockPlaceTimes.computeIfAbsent(playerId, k -> new LinkedList<>());
        
        // 添加当前时间戳
        times.offer(currentTime);
        
        // 移除超过5秒的旧时间戳
        while (!times.isEmpty() && currentTime - times.peek() > 5000) {
            times.poll();
        }
        
        // 计算5秒内的方块/秒
        double bps = times.size() / 5.0;
        
        // 显示搭路速度小标题，仅在放置方块时显示
        String subtitle = ChatColor.GOLD + "搭路速度: " + ChatColor.AQUA + String.format("%.2f", bps) + ChatColor.GOLD + " 方块/秒";
        player.sendTitle("", subtitle, 0, 40, 10); // 空标题，副标题显示速度，显示1.5秒 (0-40-10 ticks)
    }
    
    private void ensureInfiniteSandstone(Player player) {
        // 检查玩家是否还有沙石，如果没有则补充
        if (player.getInventory().contains(Material.SANDSTONE)) {
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.SANDSTONE) {
                    count += item.getAmount();
                }
            }
            
            // 如果数量少于16个，则补充到64个
            if (count < 16) {
                ItemStack sandstone = new ItemStack(Material.SANDSTONE, 64);
                player.getInventory().addItem(sandstone);
            }
        } else {
            // 如果完全没有沙石，则给一组
            ItemStack sandstone = new ItemStack(Material.SANDSTONE, 64);
            player.getInventory().addItem(sandstone);
        }
    }
}