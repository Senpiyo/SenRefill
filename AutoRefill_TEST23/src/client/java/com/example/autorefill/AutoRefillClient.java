package com.example.autorefill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class AutoRefillClient implements ClientModInitializer {
    public static final String MOD_ID = "autorefill";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[AutoRefill TEST23] client initialized");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                InputDetectionController.onPlayerTick(client);
            }
        });
    }
}
