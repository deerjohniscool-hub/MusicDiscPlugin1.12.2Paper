# DiscPlugin

A Spigot/Paper plugin for Minecraft 1.12.2 that allows players to download MP3 files from URLs and convert them into custom music discs that can be played in a jukebox.

## Features

- Download MP3 from any URL using `/musicdisc <url> <songName>`
- The downloaded MP3 is saved as a custom sound and associated with a music disc item.
- Insert the disc into a jukebox to play the custom sound (like a regular music disc).
- The disc item is named after the song.

## Usage

1. Install the plugin on your Paper 1.12.2 server.
2. Use the command: `/musicdisc <url> <songName>`
   - Example: `/musicdisc https://example.com/song.mp3 MySong`
3. The plugin will download the MP3, save it, and give you a music disc named "MySong".
4. Place the disc into a jukebox (right-click the jukebox with the disc in hand) to play the song.
5. The jukebox will eject the disc after the sound finishes (or you can eject it early by using the jukebox again).

## Permissions

- `discplugin.use` - Allows using the `/musicdisc` command (default: true)

## Compilation

To compile this plugin, you need to include the Spigot 1.12.2 API in your classpath.

If using Maven, add the following dependency:

```xml
<dependency>
    <groupId>org.spigotmc</groupId>
    <artifactId>spigot-api</artifactId>
    <version>1.12.2-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Then run `mvn clean package` to generate the JAR file in the `target` directory.

## Notes

- The plugin uses reflection to access NMS (net.minecraft.server) code for registering and playing custom sounds.
  This is specific to Minecraft 1.12.2 and may not work on other versions.
- The downloaded MP3 files are stored in the plugin's `sounds` folder.
- The plugin does not convert the MP3 to OGG or any other format; it uses the MP3 as-is.
  However, Minecraft's sound system typically expects OGG files. You may need to convert your MP3 to OGG for best results.
  For simplicity, this plugin attempts to register the MP3 file directly. If it doesn't work, consider converting to OGG.
- Auto-stopping of sound when ejecting the disc early is not currently implemented due to technical limitations.
  The sound will play to completion once started, even if the disc is ejected early.
  This is a known limitation.

## Disclaimer

This plugin is provided as-is. Use at your own risk.