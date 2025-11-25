package net.okakuh.pepelandshop;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PepelandShop implements ModInitializer {
    public static final String MOD_ID = "pepeland-shop";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Pepeland Shop mod initialized!");
        // Серверная логика (если нужна) здесь
    }
}