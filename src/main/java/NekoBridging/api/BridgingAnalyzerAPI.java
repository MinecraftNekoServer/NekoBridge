package NekoBridging.api;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import NekoBridging.Man;

public class BridgingAnalyzerAPI {
    public static void setBlockSkinProvider(BlockSkinProvider blockSkinProvider) {
        Man.setBlockSkinProvider(blockSkinProvider);
    }

    public static void clearEffect(Player player) {
        Man.clearEffect(player);
    }

    public static void clearInventory(Player player) {
        Man.clearInventory(player);
    }

    public static void respawnVillager() {
        Man.spawnVillager();
    }

    public static boolean isPlacedByPlayer(Block block) {
        return Man.isPlacedByPlayer(block);
    }

    public static void teleportCheckPoint(Player player) {
        Man.teleportCheckPoint(player);
    }

    public static void refreshItem(Player p) {
        Man.refreshItem(p);
    }

    public static void setPlayerHighlightEnabled(Player player, boolean enabled) {
        Man.getCounter(player).setHighlightEnabled(enabled);
    }

    public static void setPlayerPvPEnabled(Player player, boolean enabled) {
        Man.getCounter(player).setPvPEnabled(enabled);
    }

    public static void setPlayerSpeedDisplayEnabled(Player player, boolean enabled) {
        Man.getCounter(player).setSpeedCountEnabled(enabled);
    }

    public static void setPlayerStandBridgeMarkerEnabled(Player player, boolean enabled) {
        Man.getCounter(player).setStandBridgeMarkerEnabled(enabled);
    }

    public static boolean isPlayerHighlightEnabled(Player player) {
        return Man.getCounter(player).isHighlightEnabled();
    }

    public static boolean isPlayerPvPEnabled(Player player) {
        return Man.getCounter(player).isPvPEnabled();
    }

    public static boolean isPlayerSpeedDisplayEnabled(Player player) {
        return Man.getCounter(player).isSpeedCountEnabled();
    }

    public static boolean isPlayerStandBridgeMarkerEnabled(Player player) {
        return Man.getCounter(player).isStandBridgeMarkerEnabled();
    }
}
