package com.example.discplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class for the DiscPlugin.
 * Allows downloading MP3 from a URL and converting it to a custom music disc
 * that can be played in a jukebox.
 */
public class DiscPlugin extends JavaPlugin implements Listener {

    // Folder where downloaded MP3s will be stored
    private final File soundFolder = new File(getDataFolder(), "sounds");
    // Mapping from disc custom name to sound file name (without extension)
    private final Map<String, String> discSoundMap = new HashMap<>();
    // Set of jukebox locations currently playing our custom sound (for possible future use)
    private final Map<String, JukeboxInfo> playingJukeboxes = new HashMap<>();

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        // Ensure sound folder exists
        if (!soundFolder.exists()) {
            soundFolder.mkdirs();
        }
        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);
        // Register command executor
        getCommand("musicdisc").setExecutor(new DiscCommand(this));
        // Load existing sounds and register them
        loadSounds();
        getLogger().info("DiscPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DiscPlugin disabled!");
    }

    /**
     * Loads existing sound files from the sound folder, builds the map, and registers them.
     */
    private void loadSounds() {
        discSoundMap.clear();
        for (File file : soundFolder.listFiles()) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".mp3")) {
                String name = file.getName().substring(0, file.getName().length() - 4);
                discSoundMap.put(name, name);
                // Register the sound
                SoundUtils.registerSound(this, name, file);
            }
        }
    }

    /**
     * Returns the sound folder.
     */
    public File getSoundFolder() {
        return soundFolder;
    }

    /**
     * Adds a disc sound mapping and registers the sound.
     * @param discName The display name of the disc.
     * @param soundFileName The filename of the sound (without extension).
     */
    public void addDiscSound(String discName, String soundFileName) {
        discSoundMap.put(discName, soundFileName);
        File soundFile = new File(soundFolder, soundFileName + ".mp3");
        if (soundFile.exists()) {
            SoundUtils.registerSound(this, soundFileName, soundFile);
        } else {
            getLogger().warning("Sound file not found for disc: " + discName);
        }
    }

    /**
     * Retrieves the sound file name associated with a disc name.
     */
    public String getSoundFileForDisc(String discName) {
        return discSoundMap.get(discName);
    }

    /**
     * Creates a custom music disc item with the given name and optional lore.
     */
    public ItemStack createDiscItem(String displayName) {
        // Use RECORD_13 as base; you can change to any record material
        ItemStack disc = new ItemStack(Material.RECORD_13);
        ItemMeta meta = disc.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            // Note: setItemName does not exist in 1.12.2 – displayName is enough
            disc.setItemMeta(meta);
        }
        return disc;
    }

    /**
     * Handles player interaction with jukebox blocks.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only process right-click actions
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // Check if the block is a jukebox
        if (block.getType() != Material.JUKEBOX) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInHand();

        // Case 1: Player is holding our custom disc -> attempt to insert
        if (isOurDisc(itemInHand)) {
            // Cancel the event to prevent default jukebox behavior (inserting disc and playing default sound)
            event.setCancelled(true);

            // Remove one disc from player's hand
            ItemStack newHand = itemInHand.clone();
            if (newHand.getAmount() > 1) {
                newHand.setAmount(newHand.getAmount() - 1);
                player.getInventory().setItemInHand(newHand);
            } else {
                player.getInventory().setItemInHand(new ItemStack(Material.AIR));
            }

            // Get the jukebox block state
            Jukebox jukebox = (Jukebox) block.getState();

            // Set the jukebox's record to our disc
            jukebox.setRecord(itemInHand.clone()); // Use the disc we took from hand
            jukebox.update(true, false); // Apply changes

            // Play our custom sound at the jukebox location
            String soundFile = getSoundFileForDisc(itemInHand.getItemMeta().getDisplayName());
            if (soundFile != null) {
                SoundUtils.playCustomSound(block.getLocation(), soundFile, 1.0f, 1.0f);
                // Record that this jukebox is playing our sound (for potential stopping later)
                playingJukeboxes.put(getLocationKey(block.getLocation()), new JukeboxInfo(block.getLocation(), soundFile));
            }

            // Notify player
            player.sendMessage("Inserted disc into jukebox.");
            return;
        }

        // Case 2: Player is not holding our disc (could be empty or other item) and jukebox has our disc inside -> allow ejection
        Jukebox jukebox = (Jukebox) block.getState();
        ItemStack record = jukebox.getRecord();
        if (record != null && isOurDisc(record)) {
            // Let the event proceed normally (do not cancel) so the jukebox ejects the disc
            // We’ll not stop the sound here; the sound will continue until it finishes.
            // Since we played the sound once when inserted, it will stop automatically when done.
            return;
        }

        // If the jukebox has some other disc (not ours), we do nothing; let default behavior happen.
        // If the jukebox has no disc and player is holding something else, default behavior may do nothing.
        // We let event proceed.
    }

    /**
     * Checks if the given item stack is one of our custom music discs.
     */
    private boolean isOurDisc(ItemStack item) {
        if (item == null || item.getType() != Material.RECORD_13) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String discName = meta.getDisplayName();
        return discSoundMap.containsKey(discName);
    }

    /**
     * Generates a key for a location (world,x,y,z) for use in maps.
     */
    private String getLocationKey(Location location) {
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }

    /**
     * Simple class to hold info about a jukebox playing our sound.
     */
    private class JukeboxInfo {
        private final Location location;
        private final String soundFile;

        JukeboxInfo(Location location, String soundFile) {
            this.location = location;
            this.soundFile = soundFile;
        }
    }
}

Key changes
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }

    /**
     * Simple class to hold info about a jukebox playing our sound.
     */
    private class JukeboxInfo {
        private final Location location;
        private final String soundFile;

        JukeboxInfo(Location location, String soundFile) {
            this.location = location;
            this.soundFile = soundFile;
        }
    }
}
