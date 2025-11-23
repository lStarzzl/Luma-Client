/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 * * Simplified DiscordPresence module focusing only on basic In-Game vs. In-Menu status.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;

public class SimpleDiscordPresence extends Module {
    // Discord Rich Presence object
    private final RichPresence rpc = new RichPresence();

    // Setting Groups
    private final SettingGroup sgGeneral = settings.createGroup("General");

    // Settings for user customization (simplified)
    private final Setting<String> inGameDetails = sgGeneral.add(new StringSetting.Builder()
        .name("in-game-details")
        .description("The main text line displayed when playing in a world/server.")
        .defaultValue("Cracking some blocks.")
        .build()
    );

    private final Setting<String> mainMenuState = sgGeneral.add(new StringSetting.Builder()
        .name("main-menu-state")
        .description("The secondary text line displayed when in the main menu or options.")
        .defaultValue("Chillin' in the main menu.")
        .build()
    );

    // Internal state tracking
    private boolean lastStateInGame = false;

    public SimpleDiscordPresence() {
        super(Categories.Misc, "simple-discord-presence", "A much simpler Discord Rich Presence, without Starscript or rotation.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        // 1. Initialize Discord IPC
        // Use a known client ID for Meteor Client
        DiscordIPC.start(835240968533049424L, null);

        // 2. Set static presence elements
        rpc.setStart(System.currentTimeMillis() / 1000L); // Set timer start

        // Set large image and text (static)
        String largeText = "%s %s".formatted(MeteorClient.NAME, MeteorClient.VERSION);
        if (!MeteorClient.BUILD_NUMBER.isEmpty()) largeText += " Build: " + MeteorClient.BUILD_NUMBER;
        rpc.setLargeImage("meteor_client", largeText);

        // Set small image and text (static - using one of the available assets)
        rpc.setSmallImage("seasnail", "seasnail8169"); 

        // 3. Set initial state and force an update
        lastStateInGame = !Utils.canUpdate(); // Ensures the first update runs
        updatePresence(true);
    }

    @Override
    public void onDeactivate() {
        // Stop the Discord IPC connection
        DiscordIPC.stop();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Update presence only when the game state changes (e.g., connect to server or disconnect)
        updatePresence(false);
    }

    private void updatePresence(boolean force) {
        boolean isInGame = Utils.canUpdate();

        // Only update RPC if the state changed, or if force is true (from onActivate)
        if (isInGame == lastStateInGame && !force) return;

        if (isInGame) {
            // State 1: Player is in a world or connected to a server
            rpc.setDetails(inGameDetails.get());
            // Since we removed Starscript, we can't get server name dynamically, so we just set a fixed state.
            rpc.setState("In a Minecraft session.");
        } else {
            // State 2: Player is in the main menu or a game screen
            rpc.setDetails(MeteorClient.NAME + " " + MeteorClient.VERSION); // Use a fixed client name for the top line
            rpc.setState(mainMenuState.get()); // Use the user's custom main menu state
        }

        DiscordIPC.setActivity(rpc);
        lastStateInGame = isInGame;
    }
}
