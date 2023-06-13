package ru.nern.alwaysloaded;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.io.FileUtils;
import ru.nern.alwaysloaded.mixin.ChunkTicketAccessor;
import ru.nern.alwaysloaded.mixin.ChunkTicketManagerAccessor;
import ru.nern.alwaysloaded.mixin.ServerChunkManagerAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public class AlwaysLoaded implements ModInitializer
{
	public static final Logger LOGGER = LoggerFactory.getLogger("Always Loaded");

	private File createSaveFile(MinecraftServer server)
	{
		File file = new File(server.getSavePath(WorldSavePath.ROOT).resolve("data").toFile(), "chunks.loaded");
		File fileOld = new File(server.getSavePath(WorldSavePath.ROOT).resolve("data").toFile(), "chunks.loaded.old");
		try {
			if (!file.exists()) {
				if (!file.createNewFile()) LOGGER.error("Unable to create a save file");
			} else {
				FileUtils.copyFile(file, fileOld);
			}

		}catch (IOException e) {
			throw new RuntimeException(e);
		}
		return file;
	}

	private File getSaveFile(MinecraftServer server)
	{
		return new File(server.getSavePath(WorldSavePath.ROOT).resolve("data").toFile(), "chunks.loaded");
	}

	public void saveData(MinecraftServer server) throws IOException
	{
		File saveFile = createSaveFile(server);

		FileOutputStream fos;

		fos = new FileOutputStream(saveFile);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

		int savedChunkCount = 0;
		for(World world : server.getWorlds())
		{
			ServerChunkManagerAccessor scm = (ServerChunkManagerAccessor) world.getChunkManager();
			Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> tickets = ((ChunkTicketManagerAccessor) scm.getTicketManager()).getTicketsByPosition();
			for (SortedArraySet<ChunkTicket<?>> chunkTickets : tickets.values())
			{
				for (ChunkTicket<?> ticket : chunkTickets)
				{
					if (ticket.getType() == ChunkTicketType.PORTAL)
					{
						ChunkTicketAccessor<?> ticketAccessor = (ChunkTicketAccessor<?>) (Object) ticket;
						BlockPos loadPos = (BlockPos) ticketAccessor.getArgument();

						//LOGGER.info("Saving chunk ticket: " +loadPos.toString() + " World: " +world.getRegistryKey().getValue().toString());
						bw.write(loadPos.asLong() + " " + world.getRegistryKey().getValue());
						bw.newLine();
						savedChunkCount++;
					}
				}
			}
		}
		LOGGER.info("Saved " + savedChunkCount + " chunk tickets");

		bw.close();
	}

	private void loadData(MinecraftServer server) throws IOException
	{
		File saveFile = getSaveFile(server);
		if(!saveFile.exists() || saveFile.length() == 0) return;

		List<String> strings = Files.readAllLines(saveFile.toPath());

		int loadedChunksCount = 0;

		for(String data : strings)
		{
			String[] splitedData = data.split("\\s+");

			BlockPos blockPos;

			if(splitedData.length != 2)
			{
				LOGGER.error("Saved data has invalid arguments");
				continue;
			}

			try {
				blockPos = BlockPos.fromLong(Long.parseLong(splitedData[0]));
			}catch (NumberFormatException exc)
			{
				LOGGER.error("Saved data has invalid position");
				continue;
			}

			if(!World.isValid(blockPos))
			{
				LOGGER.warn("Saved data has invalid position");
				continue;
			}


			World world = null;

			for (World tempWorld : server.getWorlds())
			{
				if (tempWorld.getRegistryKey().getValue().toString().equals(splitedData[1])) world = tempWorld;
			}

			if(world == null)
			{
				LOGGER.error("Saved data has invalid world id");
				continue;
			}

			//LOGGER.info("Loading chunk ticket: " + blockPos + " World: " +world.getRegistryKey().getValue());

			ServerChunkManager chunkManager = (ServerChunkManager) world.getChunkManager();
			chunkManager.addTicket(ChunkTicketType.PORTAL, new ChunkPos(blockPos), 3, blockPos);

			loadedChunksCount++;
		}
		LOGGER.info("Successfully loaded " + loadedChunksCount +"/" + strings.size() + " chunk tickets");
	}

	@Override
	public void onInitialize()
	{
		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) ->
		{
			try {
				loadData(server);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPING.register((MinecraftServer server) ->
		{
			try {
				saveData(server);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
