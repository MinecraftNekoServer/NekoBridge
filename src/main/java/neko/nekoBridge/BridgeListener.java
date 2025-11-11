package neko.nekoBridge;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // 取消死亡消息显示
        event.setDeathMessage(null);
        
        // 清除死亡掉落物
        event.getDrops().clear();
        
        // 玩家死亡后立即复活
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.spigot().respawn();
            
            // 重新发放物品
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                ensureInfiniteSandstone(player);
            }, 5L); // 延迟1个tick确保复活完成
        }, 1L); // 延迟1个tick执行复活
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否使用钻石镐挖掘
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand != null && itemInHand.getType() == Material.DIAMOND_PICKAXE) {
            // 防止方块掉落物品
            event.setDropItems(false);
        }
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
        // 检查玩家是否还有沙石
        if (player.getInventory().contains(Material.SANDSTONE)) {
            // 确保沙石数量始终为64个
            int sandstoneSlot = -1;
            int sandstoneCount = 0;
            
            // 查找沙石在哪个槽位
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() == Material.SANDSTONE) {
                    sandstoneSlot = i;
                    sandstoneCount += item.getAmount();
                }
            }
            
            // 如果沙石数量不是64，则调整到64个
            if (sandstoneCount != 64) {
                if (sandstoneSlot != -1) {
                    // 更新现有槽位的沙石数量
                    player.getInventory().setItem(sandstoneSlot, new ItemStack(Material.SANDSTONE, 64));
                } else {
                    // 如果没有找到沙石槽位，则添加新的沙石
                    player.getInventory().addItem(new ItemStack(Material.SANDSTONE, 64));
                }
            }
        } else {
            // 如果完全没有沙石，则给一组
            player.getInventory().addItem(new ItemStack(Material.SANDSTONE, 64));
        }
        
        // 确保玩家有一个效率3的钻石镐
        ensureDiamondPickaxe(player);
    }
    
    private void ensureDiamondPickaxe(Player player) {
        // 检查玩家是否已经有钻石镐
        boolean hasPickaxe = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND_PICKAXE) {
                hasPickaxe = true;
                break;
            }
        }
        
        // 如果没有钻石镐，则给一个效率3且无限耐久的钻石镐
        if (!hasPickaxe) {
            ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE, 1);
            pickaxe.addUnsafeEnchantment(Enchantment.DIG_SPEED, 3); // 效率III
            pickaxe.addUnsafeEnchantment(Enchantment.DURABILITY, 3); // 无限耐久III (在1.12.2中，耐久度附魔等级3是最高等级)
            player.getInventory().addItem(pickaxe);
        }
    }
}