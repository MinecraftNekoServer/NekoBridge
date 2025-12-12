package BridgingAnalyzer.utils;

import org.bukkit.Sound;

public class SoundMachine {
    public static Sound get(String v18, String v19) {
        try {
            return Sound.valueOf(v18);
        } catch (IllegalArgumentException ex) {
            try {
                return Sound.valueOf(v19);
            } catch (IllegalArgumentException ex2) {
                // 如果两个版本的sound都不存在，返回一个默认值
                // 对于Minecraft 1.12.2，使用旧的命名约定
                return Sound.valueOf(v18.replace("ENTITY_", "ENTITY_").replace("ITEM_", "ITEM_").replace("BLOCK_", "BLOCK_"));
            }
        }
    }

    // 为Minecraft 1.12.2特别添加LEVEL_UP声音的处理
    public static Sound getLevelUpSound() {
        try {
            return Sound.valueOf("LEVEL_UP");
        } catch (IllegalArgumentException ex) {
            try {
                return Sound.valueOf("ENTITY_PLAYER_LEVELUP");
            } catch (IllegalArgumentException ex2) {
                return Sound.valueOf("ENTITY_PLAYER_LEVEL_UP");
            }
        }
    }

    // 为Minecraft 1.12.2特别添加ORB_PICKUP声音的处理
    public static Sound getOrbPickupSound() {
        try {
            return Sound.valueOf("ORB_PICKUP");
        } catch (IllegalArgumentException ex) {
            try {
                return Sound.valueOf("ENTITY_EXPERIENCE_ORB_PICKUP");
            } catch (IllegalArgumentException ex2) {
                return Sound.valueOf("ENTITY_EXPERIENCE_ORB_PICKUP");
            }
        }
    }

    // 为Minecraft 1.12.2特别添加ENDERMAN_TELEPORT声音的处理
    public static Sound getEndermanTeleportSound() {
        try {
            return Sound.valueOf("ENDERMAN_TELEPORT");
        } catch (IllegalArgumentException ex) {
            try {
                return Sound.valueOf("ENTITY_ENDERMEN_TELEPORT");
            } catch (IllegalArgumentException ex2) {
                return Sound.valueOf("ENTITY_ENDERMAN_TELEPORT");
            }
        }
    }
}
