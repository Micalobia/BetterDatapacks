package dev.micalobia.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.micalobia.ExecuteRaycastSubcommand;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ExecuteCommand.class)
public class ExecuteCommandMixin {
    @Inject(method = "register", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void registerRaycast(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CallbackInfo ci, LiteralCommandNode<ServerCommandSource> root) {
        ExecuteRaycastSubcommand.register(dispatcher, commandRegistryAccess, root);
    }
}
