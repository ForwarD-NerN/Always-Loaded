package ru.nern.alwaysloaded;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.world.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nern.alwaysloaded.storage.DimensionTicketStorage;
import ru.nern.alwaysloaded.storage.LegacyTicketStorage;

import java.io.IOException;

public class AlwaysLoaded implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("Always Loaded");

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				LegacyTicketStorage.loadChunkTickets(server);

				for(ServerWorld world : server.getWorlds()) {
					DimensionTicketStorage.loadChunkTickets(world);
				}
			} catch (IOException | NumberFormatException e) {
				AlwaysLoaded.LOGGER.error("Exception occurred during loading of chunk tickets ", e);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			try {
				for (ServerWorld world : server.getWorlds()) {
					DimensionTicketStorage.saveChunkTickets(world);
				}
			} catch (IOException e) {
				AlwaysLoaded.LOGGER.error("Exception occurred during saving of chunk tickets ", e);
			}
		});
	}
}
