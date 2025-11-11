package neko.nekoBridge;

import org.bukkit.plugin.java.JavaPlugin;

public final class NekoBridge extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new BridgeListener(this), this);
        System.out.println("[NekoBridge] 插件加载成功");
    }

    @Override
    public void onDisable() {
        System.out.println("[NekoBridge] 插件卸载成功");
    }
}
