package neko.nekoBridge;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class NekoBridge extends JavaPlugin {

    @Override
    public void onEnable() {
        // 保存默认配置文件
        saveDefaultConfig();
        
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new BridgeListener(this), this);
        getCommand("setspawn").setExecutor(new SpawnCommand(this));
        
        // 锁定天气为晴天
        lockWeather();
        
        System.out.println("[NekoBridge] 插件加载成功");
    }

    @Override
    public void onDisable() {
        System.out.println("[NekoBridge] 插件卸载成功");
    }
    
    private void lockWeather() {
        // 设置所有世界为晴天并锁定
        getServer().getWorlds().forEach(world -> {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
        });
        
        // 每隔一段时间检查并重置天气
        new BukkitRunnable() {
            @Override
            public void run() {
                getServer().getWorlds().forEach(world -> {
                    if (world.hasStorm() || world.isThundering()) {
                        world.setStorm(false);
                        world.setThundering(false);
                    }
                });
            }
        }.runTaskTimer(this, 0L, 6000L); // 每5分钟检查一次
    }
}
