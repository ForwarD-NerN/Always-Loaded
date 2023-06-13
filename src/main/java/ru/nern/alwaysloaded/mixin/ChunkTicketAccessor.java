package ru.nern.alwaysloaded.mixin;

import net.minecraft.server.world.ChunkTicket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkTicket.class)
public interface ChunkTicketAccessor<T>
{
    @Accessor
    T getArgument();
}
