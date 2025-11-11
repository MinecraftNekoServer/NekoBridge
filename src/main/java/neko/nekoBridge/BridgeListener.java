package neko.nekoBridge;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BridgeListener implements Listener {
    
    private final Map<UUID, Long> lastBlockPlaceTime = new HashMap<>();
    private final Map<UUID, Double> blocksPerSecond = new HashMap<>();
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
        lastBlockPlaceTime.put(player.getUniqueId(), System.currentTimeMillis());
        blocksPerSecond.put(player.getUniqueId(), 0.0);
        
        // 给玩家提供无限沙石
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SURVIVAL) {
            ensureInfiniteSandstone(player);
        }
        
        // 开始显示搭路速度计时器
        startSpeedDisplay(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 取消退出消息显示
        event.setQuitMessage(null);
        
        // 清理玩家数据
        UUID playerId = event.getPlayer().getUniqueId();
        lastBlockPlaceTime.remove(playerId);
        blocksPerSecond.remove(playerId);
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 确保玩家始终有沙石
        ensureInfiniteSandstone(player);
        
        long currentTime = System.currentTimeMillis();
        long lastTime = lastBlockPlaceTime.getOrDefault(playerId, currentTime);
        
        // 计算放置方块的间隔时间
        double timeDiff = (currentTime - lastTime) / 1000.0; // 转换为秒
        
        if (timeDiff > 0) {
            // 计算每秒放置的方块数
            double bps = 1.0 / timeDiff;
            blocksPerSecond.put(playerId, bps);
        }
        
        lastBlockPlaceTime.put(playerId, currentTime);
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
    
    private void startSpeedDisplay(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                UUID playerId = player.getUniqueId();
                double bps = blocksPerSecond.getOrDefault(playerId, 0.0);
                
                // 显示搭路速度小标题
                String subtitle = "搭路速度: " + String.format("%.2f", bps) + " 方块/秒";
                player.sendTitle("", subtitle, 0, 20, 0); // 空标题，副标题显示速度
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L); // 每秒更新一次（20个tick）
    }
}