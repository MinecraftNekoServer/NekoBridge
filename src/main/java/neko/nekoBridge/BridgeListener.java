package neko.nekoBridge;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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
import org.bukkit.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class BridgeListener implements Listener {
    
    // 存储每个玩家最近放置方块的时间戳队列
    private final Map<UUID, Queue<Long>> blockPlaceTimes = new HashMap<>();
    // 存储玩家放置的方块位置
    private final Map<String, UUID> placedBlocks = new HashMap<>();
    // 存储玩家放置的方块按时间顺序
    private final Map<UUID, Queue<String>> playerPlacedBlocksOrder = new HashMap<>();
    // 存储每个玩家的个人出生点
    private final Map<UUID, Location> playerSpawnPoints = new HashMap<>();
    // 存储玩家当前站立的方块位置，用于检测是否移动到了新的方块
    private final Map<UUID, String> playerCurrentBlock = new HashMap<>();
    private final NekoBridge plugin;
    
    public BridgeListener(NekoBridge plugin) {
        this.plugin = plugin;
        
        // 每秒检查一次玩家脚下的方块
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                checkPlayerSpawnPoint(player);
            }
        }, 20L, 20L); // 每秒执行一次（20个tick）
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
        
        // 传送玩家到全局出生点
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            teleportToSpawn(player);
        }, 1L); // 延迟1个tick以确保玩家完全加入
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 取消退出消息显示
        event.setQuitMessage(null);
        
        // 立即破坏该玩家放置的所有方块（不按顺序，因为玩家退出了）
        destroyPlayerBlocksImmediately(playerId, player.getWorld());
        
        // 清理玩家数据
        blockPlaceTimes.remove(playerId);
        playerSpawnPoints.remove(playerId);
        playerCurrentBlock.remove(playerId);
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        
        // 取消死亡消息显示
        event.setDeathMessage(null);
        
        // 清除死亡掉落物
        event.getDrops().clear();
        
        // 破坏该玩家放置的所有方块
        destroyPlayerBlocks(playerId, player.getWorld());
        
        // 玩家死亡后立即复活
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.spigot().respawn();
            
            // 立即传送玩家到个人出生点（如果存在）并重新发放物品
            teleportToPlayerSpawn(player);
            ensureInfiniteSandstone(player);
        });
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // 创造模式玩家可以破坏任何方块
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        Block block = event.getBlock();
        String blockKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
        
        // 检查方块是否是玩家放置的
        if (!placedBlocks.containsKey(blockKey)) {
            // 不是玩家放置的方块，阻止破坏
            event.setCancelled(true);
            return;
        }
        
        // 检查是否是放置该方块的玩家本人
        UUID placerId = placedBlocks.get(blockKey);
        if (!placerId.equals(player.getUniqueId())) {
            // 不是放置者本人，阻止破坏
            event.setCancelled(true);
            return;
        }
        
        // 是玩家自己放置的方块，允许破坏
        // 移除记录
        placedBlocks.remove(blockKey);
        
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
        
        // 记录玩家放置的方块
        Block block = event.getBlock();
        String blockKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
        placedBlocks.put(blockKey, playerId);
        
        // 记录方块放置顺序
        Queue<String> blockOrder = playerPlacedBlocksOrder.computeIfAbsent(playerId, k -> new LinkedList<>());
        blockOrder.offer(blockKey);
        
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
        String subtitle = ChatColor.AQUA + String.format("%.2f", bps) + ChatColor.GOLD + " 方块/秒";
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
    
    private void destroyPlayerBlocks(UUID playerId, World world) {
        // 获取该玩家放置的方块按顺序
        Queue<String> blockOrder = playerPlacedBlocksOrder.get(playerId);
        if (blockOrder == null) {
            return;
        }
        
        // 按顺序逐步破坏方块
        int delay = 0;
        while (!blockOrder.isEmpty()) {
            String blockKey = blockOrder.poll();
            
            // 确保方块仍然存在且属于该玩家
            if (placedBlocks.containsKey(blockKey) && placedBlocks.get(blockKey).equals(playerId)) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    String[] parts = blockKey.split(",");
                    if (parts.length == 4) {
                        try {
                            String worldName = parts[0];
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            int z = Integer.parseInt(parts[3]);
                            
                            // 确保是同一个世界
                            if (world.getName().equals(worldName)) {
                                Block block = world.getBlockAt(x, y, z);
                                // 播放破坏音效
                                world.playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 0.3F, 1.0F);
                                // 设置为空气方块来"破坏"它
                                block.setType(Material.AIR);
                            }
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    }
                    
                    // 从记录中移除这个方块
                    placedBlocks.remove(blockKey);
                }, delay);
                
                // 每次破坏间隔2个tick（0.1秒）
                delay += 2;
            }
        }
        
        // 清空该玩家的方块顺序记录
        playerPlacedBlocksOrder.remove(playerId);
    }
    
    private void destroyPlayerBlocksImmediately(UUID playerId, World world) {
        // 创建一个列表来存储需要移除的方块键
        List<String> blocksToRemove = new ArrayList<>();
        
        // 遍历所有放置的方块，找到属于该玩家的
        for (Map.Entry<String, UUID> entry : placedBlocks.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                blocksToRemove.add(entry.getKey());
            }
        }
        
        // 破坏这些方块
        for (String blockKey : blocksToRemove) {
            String[] parts = blockKey.split(",");
            if (parts.length == 4) {
                try {
                    String worldName = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    
                    // 确保是同一个世界
                    if (world.getName().equals(worldName)) {
                        Block block = world.getBlockAt(x, y, z);
                        // 播放破坏音效
                        world.playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 0.3F, 1.0F);
                        // 设置为空气方块来"破坏"它
                        block.setType(Material.AIR);
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        
        // 从记录中移除这些方块
        for (String blockKey : blocksToRemove) {
            placedBlocks.remove(blockKey);
        }
        
        // 清空该玩家的方块顺序记录
        playerPlacedBlocksOrder.remove(playerId);
    }
    
    private void teleportToSpawn(Player player) {
        // 检查是否有设置出生点
        if (plugin.getConfig().contains("spawn.world")) {
            String worldName = plugin.getConfig().getString("spawn.world");
            double x = plugin.getConfig().getDouble("spawn.x");
            double y = plugin.getConfig().getDouble("spawn.y");
            double z = plugin.getConfig().getDouble("spawn.z");
            float yaw = (float) plugin.getConfig().getDouble("spawn.yaw");
            float pitch = (float) plugin.getConfig().getDouble("spawn.pitch");
            
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                Location spawnLocation = new Location(world, x, y, z, yaw, pitch);
                player.teleport(spawnLocation);
            }
        }
    }
    
    private void checkPlayerSpawnPoint(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 获取玩家脚下的方块
        Location playerLocation = player.getLocation();
        Location blockLocation = playerLocation.clone();
        blockLocation.setY(blockLocation.getY() - 1); // 脚下的方块
        
        // 创建方块位置的唯一标识
        String blockKey = blockLocation.getWorld().getName() + "," + 
                         blockLocation.getBlockX() + "," + 
                         blockLocation.getBlockY() + "," + 
                         blockLocation.getBlockZ();
        
        // 获取玩家之前站立的方块
        String previousBlock = playerCurrentBlock.get(playerId);
        
        // 更新玩家当前站立的方块
        playerCurrentBlock.put(playerId, blockKey);
        
        Block block = blockLocation.getBlock();
        
        // 检查是否是红石块（完成点）并且玩家是刚移动到这个方块上
        if (block.getType() == Material.REDSTONE_BLOCK && 
            (previousBlock == null || !previousBlock.equals(blockKey))) {
            
            // 传送玩家回全局出生点（如果有绿宝石出生点则传送到绿宝石出生点）
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                teleportToPlayerSpawn(player);
                
                // 规律破坏玩家方块
                destroyPlayerBlocks(playerId, player.getWorld());
            }, 1L);
            
            // 播放完成音效
            player.playSound(playerLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
            
            // 在标题中显示完成信息
            player.sendTitle(ChatColor.GOLD + "搭路完成！", 
                            ChatColor.GREEN + "已传送回出生点", 
                            10, 60, 20);
        }
        // 检查是否是绿宝石块，并且玩家是刚移动到这个方块上
        else if (block.getType() == Material.EMERALD_BLOCK && 
            (previousBlock == null || !previousBlock.equals(blockKey))) {
            
            // 只记录坐标，不记录朝向等信息
            Location spawnPoint = new Location(
                blockLocation.getWorld(),
                blockLocation.getX() + 0.5, // 方块中心
                blockLocation.getY() + 1,   // 方块上方
                blockLocation.getZ() + 0.5  // 方块中心
            );
            
            // 设置为玩家的个人出生点
            playerSpawnPoints.put(playerId, spawnPoint);
            
            // 播放音效
            player.playSound(playerLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            
            // 在标题中显示出生点已设置
            player.sendTitle(ChatColor.GOLD + "出生点已设置", 
                            ChatColor.GREEN + "当前位置已设为你的个人出生点", 
                            10, 40, 10);
        }
    }
    
    private void teleportToPlayerSpawn(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 检查玩家是否有个人出生点
        if (playerSpawnPoints.containsKey(playerId)) {
            Location spawnPoint = playerSpawnPoints.get(playerId);
            if (spawnPoint != null && spawnPoint.getWorld() != null) {
                // 确保传送位置安全
                spawnPoint.setY(spawnPoint.getY() + 1); // 稍微提高一点避免卡在方块里
                player.teleport(spawnPoint);
                return;
            }
        }
        
        // 如果没有个人出生点，传送回全局出生点
        teleportToSpawn(player);
    }
}