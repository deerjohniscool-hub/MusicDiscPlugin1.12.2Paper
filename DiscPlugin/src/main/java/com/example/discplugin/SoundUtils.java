package com.example.discplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Utility class for registering and playing custom sounds via reflection.
 * This is a best-effort implementation for Minecraft 1.12.2 (Paper/Spigot).
 */
public class SoundUtils {

    private static Object soundManager;
    private static Method registerSoundMethod;
    private static Method playSoundMethod;

    static {
        try {
            // Get the SoundManager class (net.minecraft.server.v1_12_R1.SoundManager)
            Class<?> soundManagerClass = Class.forName("net.minecraft.server.v1_12_R1.SoundManager");
            // Get the instance from the MinecraftServer
            Class<?> minecraftServerClass = Class.forName("net.minecraft.server.v1_12_R1.MinecraftServer");
            Method getServerMethod = minecraftServerClass.getMethod("getServer");
            Object minecraftServer = getServerMethod.invoke(null);
            Field soundManagerField = minecraftServerClass.getField("soundManager");
            soundManager = soundManagerField.get(minecraftServer);

            // Get the registerSound method (void registerSound(String name, File sound))
            registerSoundMethod = soundManagerClass.getMethod("registerSound", String.class, java.io.File.class);

            // Get the playSound method (void playSound(EntityHuman entity, String name, float volume, float pitch))
            // Actually, we want to play for a player. We can use the Player's getHandle() and then playSound on the EntityPlayer.
            // We'll leave playSoundMethod as null and use a different approach for playing.
        } catch (Exception e) {
            Bukkit.getLogger().warning("[DiscPlugin] Failed to initialize SoundUtils: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers a custom sound with the given name and file.
     * @param plugin The plugin instance (to get the data folder).
     * @param soundName The name of the sound (without extension), will be prefixed with plugin's name.
     * @param soundFile The MP3 file.
     */
    public static void registerSound(Plugin plugin, String soundName, java.io.File soundFile) {
        if (soundManager == null || registerSoundMethod == null) {
            Bukkit.getLogger().warning("[DiscPlugin] SoundManager not available, cannot register custom sound.");
            return;
        }
        try {
            // The sound name in the sound manager is usually "discplugin:soundName"
            String fullName = plugin.getName().toLowerCase() + ":" + soundName;
            registerSoundMethod.invoke(soundManager, fullName, soundFile);
            Bukkit.getLogger().info("[DiscPlugin] Registered custom sound: " + fullName);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[DiscPlugin] Failed to register sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Plays a custom sound at the given location.
     * @param location Where to play the sound.
     * @param soundName The name of the sound (without extension), must match the registered name.
     * @param volume The volume (0.0 - 1.0+).
     * @param pitch The pitch (0.0 - 2.0).
     */
    public static void playCustomSound(Location location, String soundName, float volume, float pitch) {
        if (soundManager == null) {
            Bukkit.getLogger().warning("[DiscPlugin] SoundManager not available, cannot play custom sound.");
            return;
        }
        // We don't have a direct method to play a sound at a location via the SoundManager.
        // Instead, we can use the Player's playSound method if we have a player nearby, but we want to play at a location for all nearby players.
        // Alternatively, we can use the SoundManager's playSound method if we can find it.
        // Let's try to get the playSound method that takes a position.
        try {
            Class<?> soundManagerClass = soundManager.getClass();
            // Look for a method like "playSound" that takes a double x, double y, double z, String name, float volume, float pitch
            Method playSoundAtMethod = soundManagerClass.getMethod("playSound", double.class, double.class, double.class, String.class, float.class, float.class);
            playSoundAtMethod.invoke(soundManager, location.getX(), location.getY(), location.getZ(),
                    soundName, volume, pitch);
        } catch (NoSuchMethodException e) {
            // Fallback: try to play for all online players within range? Not ideal.
            Bukkit.getLogger().warning("[DiscPlugin] Could not find playSound method for location: " + e.getMessage());
            // As a last resort, we can play the sound for all online players (if they are within hearing distance, but we ignore distance)
            for (Player player : Bukkit.getOnlinePlayers()) {
                playCustomSoundForPlayer(player, soundName, volume, pitch);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[DiscPlugin] Failed to play sound at location: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Plays a custom sound for a specific player.
     * @param player The player to hear the sound.
     * @param soundName The name of the sound (without extension).
     * @param volume The volume.
     * @param pitch The pitch.
     */
    public static void playCustomSoundForPlayer(Player player, String soundName, float volume, float pitch) {
        try {
            // Get the player's EntityPlayer handle (net.minecraft.server.v1_12_R1.EntityPlayer)
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            // Get the world (net.minecraft.server.v1_12_R1.World)
            Object world = entityPlayer.getClass().getField("world").get(entityPlayer);
            // Get the playSound method from World: void playSound(double x, double y, double z, String sound, float volume, float pitch)
            Method playSoundMethod = world.getClass().getMethod("playSound", double.class, double.class, double.class, String.class, float.class, float.class);
            playSoundMethod.invoke(world,
                    entityPlayer.getClass().getField("locX").getDouble(entityPlayer),
                    entityPlayer.getClass().getField("locY").getDouble(entityPlayer),
                    entityPlayer.getClass().getField("locZ").getDouble(entityPlayer),
                    soundName, volume, pitch);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[DiscPlugin] Failed to play sound for player: " + e.getMessage());
            e.printStackTrace();
        }
    }
}