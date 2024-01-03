package dev.micalobia.event;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

public abstract class EventContext<D, X extends EventContext<D, X, C>, C extends EventCondition<X>> {
    private final MinecraftServer server;


    protected EventContext(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public NbtCompound getMacroArguments() {
        return new NbtCompound();
    }

    public abstract ServerCommandSource toSource(D data);
}
