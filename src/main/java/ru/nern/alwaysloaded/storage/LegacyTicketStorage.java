package ru.nern.alwaysloaded.storage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static ru.nern.alwaysloaded.AlwaysLoaded.LOGGER;

//Left for backwards compatibility
public class LegacyTicketStorage {
    public static void loadChunkTickets(MinecraftServer server) throws IOException {
        File saveFile = getSaveLocation(server).resolve("chunks.loaded").toFile();
        if(!saveFile.exists() || saveFile.length() == 0) return;

        int loadedTicketCount = 0;
        List<String> lines = Files.readAllLines(saveFile.toPath());

        for(String line : lines) {
            String[] arguments = line.split(" ");

            BlockPos blockPos;

            if(arguments.length != 2) {
                LOGGER.error("'{}' has invalid argument count. Expected: 2", line);
                continue;
            }

            try {
                blockPos = BlockPos.fromLong(Long.parseLong(arguments[0]));
            }catch (NumberFormatException exc) {
                LOGGER.error("'{}' is invalid position", arguments[0]);
                continue;
            }

            if(!World.isValid(blockPos)) {
                LOGGER.error("'{}' -> {} is outside of the world", arguments[0], blockPos);
                continue;
            }


            World world = null;
            for (World tempWorld : server.getWorlds()) {
                if(tempWorld.getRegistryKey().getValue().toString().equals(arguments[1])) world = tempWorld;
            }

            if(world == null) {
                LOGGER.error("Saved data has invalid world id");
                continue;
            }

            LOGGER.debug("Loading chunk ticket: {} World: {}", blockPos, world.getRegistryKey().getValue());

            ServerChunkManager chunkManager = (ServerChunkManager) world.getChunkManager();
            chunkManager.addTicket(ChunkTicketType.PORTAL, new ChunkPos(blockPos), 3, blockPos);

            loadedTicketCount++;
        }
        LOGGER.info("Loaded {}/{} chunk tickets", loadedTicketCount, lines.size());

        LOGGER.info("Deleting the legacy chunks.loaded file");
        saveFile.delete();
    }


    public static Path getSaveLocation(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("data");
    }

    /*
    public static File createSaveFile(MinecraftServer server) throws IOException {
		Path saveLocation = getSaveLocation(server);

		File ticketStorageFile = saveLocation.resolve("chunks.loaded").toFile();

		if (!ticketStorageFile.exists()) {
			if (!ticketStorageFile.createNewFile()) LOGGER.error("Unable to create chunks.loaded");
		} else {
			File backupFile = saveLocation.resolve("chunks.loaded.old").toFile();
			FileUtils.copyFile(ticketStorageFile, backupFile);
		}

		return ticketStorageFile;
	}
     */
    /*
	public static void saveChunkTickets(BufferedWriter writer, ServerWorld world) throws IOException {
		int savedTicketCount = 0;
		ServerChunkManagerAccessor accessor = (ServerChunkManagerAccessor) world.getChunkManager();
		Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> tickets = ((ChunkTicketManagerAccessor) accessor.getTicketManager()).getTicketsByPosition();
		for (SortedArraySet<ChunkTicket<?>> chunkTickets : tickets.values()) {
			for (ChunkTicket<?> ticket : chunkTickets) {
				if (ticket.getType() == ChunkTicketType.PORTAL) {
					ChunkTicketAccessor<BlockPos> ticketAccessor = (ChunkTicketAccessor<BlockPos>) (Object) ticket;
					BlockPos ticketPosition = ticketAccessor.getArgument();

					writer.write(ticketPosition.asLong() + " " + world.getRegistryKey().getValue());
					writer.newLine();
					savedTicketCount++;
                    LOGGER.debug("Saving chunk ticket: {}", ticketPosition);
				}
			}
		}

		if(savedTicketCount > 0) {
			LOGGER.info("Saved {} ticket(s) in {}", savedTicketCount, world.getRegistryKey().getValue());
		}
	}

	 */
}
