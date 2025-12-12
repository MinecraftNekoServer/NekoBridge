package NekoBridging.utils;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActionBarUtils {
    public static boolean works;
    public static String nmsver;
    private static Class<?> classCraftPlayer;
    private static Class<?> classPacketChat;
    private static Class<?> classChatSerializer;
    private static Class<?> classIChatComponent;
    private static Method methodSeralizeString;
    private static Class<?> classChatComponentText;
    private static Method methodGetHandle;
    private static Field fieldConnection;
    private static Method methodSendPacket;

    static {
        try {
            nmsver = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            classCraftPlayer = Class.forName("org.bukkit.craftbukkit." + nmsver + ".entity.CraftPlayer");
            classPacketChat = Class.forName("net.minecraft.server." + nmsver + ".PacketPlayOutChat");
            Class<?> classPacket = Class.forName("net.minecraft.server." + nmsver + ".Packet");
            if (nmsver.equalsIgnoreCase("v1_8_R1") || nmsver.equalsIgnoreCase("v1_7_")) {
                classChatSerializer = Class.forName("net.minecraft.server." + nmsver + ".ChatSerializer");
                classIChatComponent = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
                methodSeralizeString = classChatSerializer.getDeclaredMethod("a", String.class);
            } else {
                classChatComponentText = Class.forName("net.minecraft.server." + nmsver + ".ChatComponentText");
                classIChatComponent = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
            }
            methodGetHandle = classCraftPlayer.getDeclaredMethod("getHandle");
            Class<?> classEntityPlayer = Class.forName("net.minecraft.server." + nmsver + ".EntityPlayer");
            fieldConnection = classEntityPlayer.getDeclaredField("playerConnection");
            Class<?> classPlayerConnection = Class.forName("net.minecraft.server." + nmsver + ".PlayerConnection");
            methodSendPacket = classPlayerConnection.getDeclaredMethod("sendPacket", classPacket);
            works = true;
        } catch (Exception e) {
            works = false;
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (!works) return;
        try {
            Object p = classCraftPlayer.cast(player);
            Object ppoc;
            if (nmsver.equalsIgnoreCase("v1_8_R1") || nmsver.equalsIgnoreCase("v1_7_")) {
                Object cbc = classIChatComponent.cast(methodSeralizeString.invoke(classChatSerializer, "{\"text\": \"" + message + "\"}"));
                ppoc = classPacketChat.getConstructor(new Class<?>[]{classIChatComponent, byte.class}).newInstance(cbc, (byte) 2);
            } else {
                Object o = classChatComponentText.getConstructor(new Class<?>[]{String.class}).newInstance(message);
                // 尝试不同的构造函数，因为1.12.2可能有不同的参数
                try {
                    ppoc = classPacketChat.getConstructor(new Class<?>[]{classIChatComponent, byte.class}).newInstance(o, (byte) 2);
                } catch (NoSuchMethodException e) {
                    // 如果byte参数的构造函数不存在，则尝试只使用IChatBaseComponent的构造函数
                    ppoc = classPacketChat.getConstructor(new Class<?>[]{classIChatComponent}).newInstance(o);
                    // 然后通过反射设置消息类型为2（Action Bar）
                    // 注意：在1.12.2中，字段类型可能是ChatMessageType而不是byte
                    try {
                        Field typeField = ppoc.getClass().getDeclaredField("b");
                        typeField.setAccessible(true);
                        // 尝试获取ChatMessageType枚举
                        Class<?> chatMsgTypeClass = Class.forName("net.minecraft.server." + nmsver + ".ChatMessageType");
                        Object[] chatMsgTypes = chatMsgTypeClass.getEnumConstants();
                        if (chatMsgTypes.length > 2) {
                            typeField.set(ppoc, chatMsgTypes[2]); // 2对应Action Bar
                        } else {
                            // 如果枚举常量不够，使用第一个
                            typeField.set(ppoc, chatMsgTypes[0]);
                        }
                    } catch (Exception ex) {
                        // 如果无法设置类型，使用旧方法尝试
                        try {
                            Field typeField = ppoc.getClass().getDeclaredField("b");
                            typeField.setAccessible(true);
                            typeField.set(ppoc, (byte) 2);
                        } catch (Exception ex2) {
                            // 如果还是失败，则忽略，消息仍会发送但可能不是action bar
                            System.out.println("Warning: Could not set chat message type");
                        }
                    }
                }
            }
            Object h = methodGetHandle.invoke(p);
            Object pc = fieldConnection.get(h);
            methodSendPacket.invoke(pc, ppoc);
        } catch (Exception ex) {
            ex.printStackTrace();
            works = false;
        }
    }

}