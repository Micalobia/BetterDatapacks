package dev.micalobia.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.micalobia.BetterDatapacks;
import dev.micalobia.command.CommandUtility;
import dev.micalobia.Raycast;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;
import java.util.function.Predicate;

import static dev.micalobia.command.CommandUtility.generateLiteralEnumArgument;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

@Mixin(ExecuteCommand.class)
public abstract class ExecuteCommandMixin {

    @Shadow
    @Final
    private static SuggestionProvider<ServerCommandSource> LOOT_CONDITIONS;

    @Shadow
    private static ArgumentBuilder<ServerCommandSource, ?> addConditionLogic(CommandNode<ServerCommandSource> root, ArgumentBuilder<ServerCommandSource, ?> builder, boolean positive, ExecuteCommand.Condition condition) {
        return null;
    }


    @Inject(method = "register", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void registerRaycast(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CallbackInfo ci, LiteralCommandNode<ServerCommandSource> root) {
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(distanceArgument().then(literal("at").then(literal("*").redirect(root, ctx -> raycastAt(ctx, false))).then(entityArgument().redirect(root, ctx -> raycastAt(ctx, true)))))));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(distanceArgument().then(literal("as").then(literal("*").redirect(root, ctx -> raycastAs(ctx, false))).then(entityArgument().redirect(root, ctx -> raycastAs(ctx, true)))))));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(distanceArgument().then(literal("positioned").then(literal("at").then(literal("*").then(literal("entity").redirect(root, ctx -> raycastPositionedAt(ctx, false, false)))).then(literal("*").then(literal("hit").redirect(root, ctx -> raycastPositionedAt(ctx, false, true)))).then(argument("hasPredicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).then(literal("entity").redirect(root, ctx -> raycastPositionedAt(ctx, true, false)))).then(argument("hasPredicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).then(literal("hit").redirect(root, ctx -> raycastPositionedAt(ctx, true, true)))))))));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(distanceArgument().then(literal("block").then(literal("*").then(generateLiteralEnumArgument("collision_mode", Raycast.CollisionMode::values).then(generateLiteralEnumArgument("fluid_mode", Raycast.FluidMode::values).then(generateLiteralEnumArgument("hit_mode", Raycast.BlockHitMode::values).redirect(root, ctx -> raycastBlock(ctx, false))))))).then(literal("block").then(blockArgument(registryAccess).then(generateLiteralEnumArgument("collision_mode", Raycast.CollisionMode::values).then(generateLiteralEnumArgument("fluid_mode", Raycast.FluidMode::values).then(generateLiteralEnumArgument("hit_mode", Raycast.BlockHitMode::values).redirect(root, ctx -> raycastBlock(ctx, true))))))))));
    }

    @Inject(method = "addConditionArguments", at = @At(value = "HEAD"))
    private static void registerRaycastConditions(CommandNode<ServerCommandSource> root, LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, boolean positive, CommandRegistryAccess registryAccess, CallbackInfoReturnable<ArgumentBuilder<ServerCommandSource, ?>> cir) {
        argumentBuilder.then(literal("raycast").then(distanceArgument().then(literal("block").then(literal("exists").then(generateLiteralEnumArgument("collision_mode", Raycast.CollisionMode::values).then(addConditionLogic(root, generateLiteralEnumArgument("fluid_mode", Raycast.FluidMode::values), positive, context -> raycastIfBlock(context, false))))).then(blockArgument(registryAccess).then(generateLiteralEnumArgument("collision_mode", Raycast.CollisionMode::values).then(addConditionLogic(root, generateLiteralEnumArgument("fluid_mode", Raycast.FluidMode::values), positive, context -> raycastIfBlock(context, true)))))).then(literal("entity").then(addConditionLogic(root, entityArgument(), positive, context -> raycastIfEntity(context, true))).then(addConditionLogic(root, literal("exists"), positive, context -> raycastIfEntity(context, false))))));
    }

    @Unique
    private static Vec3d vectorFromRotation(Vec2f rotation) {
        final float pi180 = (float) (Math.PI / 180d);
        final float f = MathHelper.cos((rotation.y + 90.0F) * pi180);
        final float g = MathHelper.sin((rotation.y + 90.0F) * pi180);
        final float h = MathHelper.cos(-rotation.x * pi180);
        final float i = MathHelper.sin(-rotation.x * pi180);
        Vec3d vec3d2 = new Vec3d(f * h, i, g * h);
        return new Vec3d(vec3d2.x, vec3d2.y, vec3d2.z);
    }

    @Unique
    private static ServerCommandSource raycastAt(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var cast = entityCast(ctx, hasPredicate);
        var entity = cast.getEntity();
        return source.withPosition(entity.getPos()).withRotation(entity.getRotationClient());
    }

    @Unique
    private static ServerCommandSource raycastAs(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var cast = entityCast(ctx, hasPredicate);
        var entity = cast.getEntity();
        return source.withEntity(entity);
    }

    @Unique
    private static ServerCommandSource raycastPositionedAt(CommandContext<ServerCommandSource> ctx, boolean hasPredicate, boolean hit) throws CommandSyntaxException {
        var source = ctx.getSource();
        var cast = entityCast(ctx, hasPredicate);
        if (hit) return source.withPosition(cast.getPos());
        else return source.withPosition(cast.getEntity().getPos());
    }

    @Unique
    private static ServerCommandSource raycastBlock(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var rayCtx = blockCastContext(ctx, hasPredicate);
        var source = ctx.getSource();
        var world = source.getWorld();
        var cast = world.raycast(rayCtx);
        var hitMode = CommandUtility.getLiteralEnumArgument(ctx, "hit_mode", Raycast.BlockHitMode::values);
        return switch (hitMode) {
            case BLOCK -> source.withPosition(cast.getBlockPos().toCenterPos());
            case HIT -> source.withPosition(cast.getPos());
        };
    }

    @Unique
    private static boolean raycastIfBlock(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var rayCtx = blockCastContext(ctx, hasPredicate);
        var source = ctx.getSource();
        var world = source.getWorld();
        var cast = world.raycast(rayCtx);
        return cast.getType() != HitResult.Type.MISS;
    }

    @Unique
    private static boolean raycastIfEntity(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        try {
            entityCast(ctx, hasPredicate);
            return true;
        } catch (CommandException err) {
            return false;
        } catch (Exception err) {
            BetterDatapacks.LOGGER.info(err.getMessage());
            throw err;
        }
    }

    @Unique
    private static EntityHitResult entityCast(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var distance = ctx.getArgument("distance", Double.class);
        Predicate<Entity> predicate;
        if (hasPredicate) {
            var condition = IdentifierArgumentType.getPredicateArgument(ctx, "predicate");
            predicate = testRaycastEntity(source, condition);
        } else {
            predicate = ExecuteCommandMixin::pass;
        }
        var entity = source.getEntityOrThrow();
        var vecs = getCastVecs(source, distance);
        var cast = ProjectileUtil.raycast(entity, vecs.start(), vecs.end(), entity.getBoundingBox().stretch(vecs.multipliedDirection()), predicate, 0);
        if (cast == null)
            throw new CommandException(Text.translatable("argument.entity.notfound.entity"));
        return cast;
    }

    @Unique
    private static <T> boolean pass(T arg) {
        return true;
    }

    @Unique
    private static RequiredArgumentBuilder<ServerCommandSource, Double> distanceArgument() {
        return argument("distance", DoubleArgumentType.doubleArg(0, 32));
    }

    @Unique
    private static RequiredArgumentBuilder<ServerCommandSource, Identifier> entityArgument() {
        return argument("predicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS);
    }

    @Unique
    private static RequiredArgumentBuilder<ServerCommandSource, BlockPredicateArgumentType.BlockPredicate> blockArgument(CommandRegistryAccess registryAccess) {
        return argument("block", BlockPredicateArgumentType.blockPredicate(registryAccess));
    }

    @Unique
    private static RaycastContext blockCastContext(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var distance = ctx.getArgument("distance", Double.class);
        var entity = source.getEntityOrThrow();
        var vecs = getCastVecs(source, distance);
        var type = CommandUtility.getLiteralEnumArgument(ctx, "collision_mode", Raycast.CollisionMode::values).getType();
        var fluid = CommandUtility.getLiteralEnumArgument(ctx, "fluid_mode", Raycast.FluidMode::values).getFluidHandling();
        if (hasPredicate) {
            var blockPredicate = BlockPredicateArgumentType.getBlockPredicate(ctx, "block");
            return new Raycast.CustomShapeTypeRaycastContext(vecs.start(), vecs.end(), type, fluid, entity, blockPredicate);
        }
        return new RaycastContext(vecs.start(), vecs.end(), type, fluid, entity);
    }

    @Unique
    private static Raycast.VecSet getCastVecs(ServerCommandSource source, double distance) {
        var anchor = source.getEntityAnchor();
        var start = anchor.positionAt(source);
        var rotation = source.getRotation();
        var direction = vectorFromRotation(rotation);
        var multipliedDirection = direction.multiply(distance);
        var end = start.add(multipliedDirection);
        return new Raycast.VecSet(start, end, multipliedDirection);
    }

    @Unique
    private static Predicate<Entity> testRaycastEntity(ServerCommandSource source, LootCondition condition) {
        var builder = new LootContextParameterSet.Builder(source.getWorld());
        return entity -> {
            var set = builder.add(LootContextParameters.ORIGIN, entity.getPos()).add(LootContextParameters.THIS_ENTITY, entity).build(LootContextTypes.COMMAND);
            var ctx = new LootContext.Builder(set).build(Optional.empty());
            ctx.markActive(LootContext.predicate(condition));
            return condition.test(ctx);
        };
    }
}
