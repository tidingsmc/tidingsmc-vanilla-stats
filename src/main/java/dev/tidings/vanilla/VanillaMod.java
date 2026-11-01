package dev.tidings.vanilla;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("tidings_vanilla")
public class VanillaMod {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaMod.class);

    public VanillaMod(FMLJavaModLoadingContext context) {
        if (ModList.get().isLoaded("tidings")) {
            VanillaAddon.register(context);
        } else {
            LOGGER.warn("Tidings isn't installed: Tidings Vanilla does nothing without it");
        }
    }
}
