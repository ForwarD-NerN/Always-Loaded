package ru.nern.alwaysloaded.storage;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.io.FileUtils;
import ru.nern.alwaysloaded.mixin.ChunkTicketAccessor;
import ru.nern.alwaysloaded.mixin.ChunkTicketManagerAccessor;
import ru.nern.alwaysloaded.mixin.ServerChunkManagerAccessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static ru.nern.alwaysloaded.AlwaysLoaded.LOGGER;

public class DimensionTicketStorage {
    public final static String TICKETS_KEY = "tickets.saved";
    public final static String OLD_TICKETS_KEY = "tickets.saved.old";

    public static void loadChunkTickets(ServerWorld world) throws IOException, NumberFormatException {
        File saveFile = getSaveLocation(world).resolve(TICKETS_KEY).toFile();
        if(!saveFile.exists() || saveFile.length() == 0) return;

        int loadedTicketCount = 0;
        List<String> lines = Files.readAllLines(saveFile.toPath());
        ServerChunkManager chunkManager = world.getChunkManager();

        for(String line : lines) {
            if(line.isEmpty()) continue;

            String[] arguments = line.split(" ");
            BlockPos pos = new BlockPos(Integer.parseInt(arguments[0]), Integer.parseInt(arguments[1]), Integer.parseInt(arguments[2]));

            if(World.isValid(pos)) {
                chunkManager.addTicket(ChunkTicketType.PORTAL, new ChunkPos(pos), 3, pos);

                loadedTicketCount++;
                LOGGER.debug("Adding chunk ticket at {}", pos);
            }
        }

        LOGGER.info("Loaded {}/{} chunk ticket(s) in {}", loadedTicketCount, lines.size(), world.getRegistryKey().getValue());
    }

    public static void saveChunkTickets(ServerWorld world) throws IOException {
        List<BlockPos> toSave = getTicketsToSave(world);
        Path saveLocation = getSaveLocation(world);
        File saveFile = saveLocation.resolve(TICKETS_KEY).toFile();

        //Do not create a save file if the chunk ticket list is empty and there were no tickets saved before.
        if(toSave.isEmpty() && !saveFile.exists()) return;

        createSaveFileOrBackup(saveLocation);
        BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile));

        int savedTicketCount = 0;

        for(BlockPos pos : toSave) {
            writer.write(String.format("%s %s %s", pos.getX(), pos.getY(), pos.getZ()));
            writer.newLine();

            LOGGER.debug("Saving chunk ticket at {}", pos);
            savedTicketCount++;
        }

        writer.close();
        LOGGER.info("Saved {} ticket(s) in {}", savedTicketCount, world.getRegistryKey().getValue());
    }

    private static void createSaveFileOrBackup(Path saveLocation) throws IOException {
        File saveFile = saveLocation.resolve(TICKETS_KEY).toFile();

        if (!saveFile.exists()) {
            if (!saveFile.createNewFile()) LOGGER.error("Unable to create {}", TICKETS_KEY);
        } else {
            File oldSaveFile = saveLocation.resolve(OLD_TICKETS_KEY).toFile();
            FileUtils.copyFile(saveFile, oldSaveFile);
        }
    }

    private static List<BlockPos> getTicketsToSave(ServerWorld world) {
        List<BlockPos> toSave = new ArrayList<>();
        var tickets = getChunkTickets(world);

        for(SortedArraySet<ChunkTicket<?>> ticketSet : tickets) {
            for (ChunkTicket<?> ticket : ticketSet) {
                if(ticket.getType() == ChunkTicketType.PORTAL) {
                    ChunkTicketAccessor<BlockPos> ticketAccessor = (ChunkTicketAccessor<BlockPos>) (Object) ticket;
                    toSave.add(ticketAccessor.getArgument());
                }
            }
        }
        return toSave;
    }

    private static ObjectCollection<SortedArraySet<ChunkTicket<?>>> getChunkTickets(ServerWorld world) {
        ServerChunkManagerAccessor accessor = (ServerChunkManagerAccessor) world.getChunkManager();
        return ((ChunkTicketManagerAccessor) accessor.getTicketManager()).getTicketsByPosition().values();
    }

    private static Path getSaveLocation(ServerWorld world) {
        return getDimensionLocation(world.getRegistryKey(), world.getServer().getSavePath(WorldSavePath.ROOT)).resolve("data");
    }

    // Copied from Vanilla, can't use LevelStorage.Session.getWorldDirectory() because in 1.17 File is the return value and in the newer version it's Path.
    private static Path getDimensionLocation(RegistryKey<World> worldRef, Path worldDirectory) {
        if (worldRef == World.OVERWORLD) {
            return worldDirectory;
        }else if (worldRef == World.END) {
            return worldDirectory.resolve("DIM1");
        }else if (worldRef == World.NETHER) {
            return worldDirectory.resolve("DIM-1");
        }
        return worldDirectory.resolve("dimensions").resolve(worldRef.getValue().getNamespace()).resolve(worldRef.getValue().getPath());
    }
}
