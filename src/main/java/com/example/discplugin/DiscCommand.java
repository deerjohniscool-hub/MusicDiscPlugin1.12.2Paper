package com.example.discplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

/**
 * Command executor for the /musicdisc command.
 * Usage: /musicdisc <url> <songName>
 */
public class DiscCommand implements CommandExecutor {

    private final DiscPlugin plugin;

    public DiscCommand(DiscPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /musicdisc <url> <songName>");
            return true;
        }

        String urlString = args[0];
        String songName = args[1];

        // Validate URL
        try {
            new URL(urlString);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Invalid URL.");
            return true;
        }

        // Download the MP3
        File soundFile = downloadMp3(urlString, songName);
        if (soundFile == null) {
            player.sendMessage(ChatColor.RED + "Failed to download MP3.");
            return
        }

        // Register the sound (this will be loaded on next server restart or we can try to load it now)
        // For simplicity, we’ll just add the mapping and reload the sounds map.
        plugin.addDiscSound(songName, soundFile.getName().replace(".mp3", ""));
        // Also, we can try to register the sound instantly via SoundUtils (if implemented)
        // SoundUtils.registerSound(plugin, songName, soundFile); // We'll implement this later

        // Give the player the custom disc
        ItemStack disc = plugin.createDiscItem(songName);
        player.getInventory().addItem(disc);

        player.sendMessage(ChatColor.GREEN + "Downloaded and created disc for song: " + ChatColor.YELLOW + songName);
        return true;
    }
     * @param urlString The URL of the MP3.
     * @param songName The desired name for the sound file (without extension).
     * @return The saved File, or null if failed.
     */
    private File downloadMp3(String urlString, String songName) {
        try {
            URL url = new URL(urlString);
            InputStream in = url.openStream();
            File soundFolder = plugin.getSoundFolder();
            if (!soundFolder.exists()) {
                soundFolder.mkdirs();
            }
            File targetFile = new File(soundFolder, songName + ".mp3");
            Files.copy(in, targetFile.toPath());
            in.close();
            return targetFile;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to download MP3 from " + urlString + ": " + e.getMessage());
            return null;
        }
    }
}
