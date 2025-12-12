package NekoBridging;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

public class ResourcePackLoader implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(Man.getInstance(),
                () -> e.getPlayer().setResourcePack("https://raw.githubusercontent.com/SakuraKoi/FileCloud/texture/BridgingHelper.zip"), 10);
        // raw.githubusercontent.com在部分地区及网络环境下无法访问, 并不保证正常加载
    }

    @EventHandler
    public void onRefuse(PlayerResourcePackStatusEvent e) {
        if (e.getStatus() == Status.ACCEPTED)
            e.getPlayer().sendMessage("§b§l梦幻次元 §7>> §e正在下载资源包, 请稍候...");
        else if (e.getStatus() == Status.FAILED_DOWNLOAD)
            e.getPlayer().sendMessage(new String[]{"§a§l梦幻次元 §7>> §c资源包下载失败, 请重进游戏再试一次",
                    "§a§l梦幻次元 §7>> §c如果此情况反复出现, 请联系管理员"});
        else if (e.getStatus() == Status.SUCCESSFULLY_LOADED)
            e.getPlayer().sendMessage(new String[]{"", "§a§l梦幻次元 §7>> §b此资源包对沙石添加了标记线以辅助您练习",
                    "§a§l梦幻次元 §7>> §e顶端的红蓝线是走搭路线", "§a§l梦幻次元 §7>> §e侧面的指示线是推荐准心位置",
                    "§a§l梦幻次元 §7>> §a输入 /bridge 可以开关一些有用的功能", ""});
    }
}
