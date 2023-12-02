package dev.micalobia.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.micalobia.BetterDatapacks;
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
import net.minecraft.util.Pair;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.EntityHitResult;
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
import java.util.function.Supplier;

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

    @Unique
    private static final DoubleArgumentType DISTANCE_ARG = DoubleArgumentType.doubleArg(0, 32);


    @Inject(method = "register", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void registerRaycast(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CallbackInfo ci, LiteralCommandNode<ServerCommandSource> root) {
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(argument("distance", DISTANCE_ARG).then(literal("at").then(literal("*").redirect(root, ctx -> raycastAt(ctx, false))).then(argument("predicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).redirect(root, ctx -> raycastAt(ctx, true)))))));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(argument("distance", DISTANCE_ARG).then(literal("as").then(literal("*").redirect(root, ctx -> raycastAs(ctx, false))).then(argument("predicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).redirect(root, ctx -> raycastAs(ctx, true)))))));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(argument("distance", DISTANCE_ARG).then(literal("positioned").then(literal("at").then(literal("*").then(literal("entity").redirect(root, ctx -> raycastPositionedAt(ctx, false, false)))).then(literal("*").then(literal("hit").redirect(root, ctx -> raycastPositionedAt(ctx, false, true)))).then(argument("hasPredicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).then(literal("entity").redirect(root, ctx -> raycastPositionedAt(ctx, true, false)))).then(argument("hasPredicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).then(literal("hit").redirect(root, ctx -> raycastPositionedAt(ctx, true, true)))))))));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(argument("distance", DISTANCE_ARG).then(literal("block").then(literal("*").then(generateLiteralEnumArgument("collision_mode", Raycast.CollisionMode::values).then(generateLiteralEnumArgument("fluid_mode", Raycast.FluidMode::values).redirect(root, ctx -> raycastBlock(ctx, false)))))).then(literal("block").then(argument("block", BlockPredicateArgumentType.blockPredicate(registryAccess)).then(generateLiteralEnumArgument("collision_mode", Raycast.CollisionMode::values).then(generateLiteralEnumArgument("fluid_mode", Raycast.FluidMode::values).redirect(root, ctx -> raycastBlock(ctx, true)))))))));
    }

    @Inject(method = "addConditionArguments", at = @At(value = "HEAD"))
    private static void registerRaycastConditions(CommandNode<ServerCommandSource> root, LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, boolean positive, CommandRegistryAccess commandRegistryAccess, CallbackInfoReturnable<ArgumentBuilder<ServerCommandSource, ?>> cir) {

//        ExecuteRaycastSubcommand.registerConditions(root, argumentBuilder, positive, commandRegistryAccess);
//        var t = argumentBuilder
//                .then(CommandManager.literal("block")
//                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
//                                .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("block", BlockPredicateArgumentType.blockPredicate(commandRegistryAccess)), positive, context -> BlockPredicateArgumentType.getBlockPredicate(context, "block").test(new CachedBlockPosition(context.getSource().getWorld(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), true))))))
//                .then(CommandManager.literal("biome")
//                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
//                                .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("biome", RegistryEntryPredicateArgumentType.registryEntryPredicate(commandRegistryAccess, RegistryKeys.BIOME)), positive, context -> RegistryEntryPredicateArgumentType.getRegistryEntryPredicate(context, "biome", RegistryKeys.BIOME).test(context.getSource().getWorld().getBiome(BlockPosArgumentType.getLoadedBlockPos(context, "pos")))))))
//                .then(CommandManager.literal("loaded")
//                        .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("pos", BlockPosArgumentType.blockPos()), positive, commandContext -> ExecuteCommand.isLoaded(commandContext.getSource().getWorld(), BlockPosArgumentType.getBlockPos(commandContext, "pos")))))
//                .then(CommandManager.literal("dimension")
//                        .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("dimension", DimensionArgumentType.dimension()), positive, context -> DimensionArgumentType.getDimensionArgument(context, "dimension") == context.getSource().getWorld())))
//                .then(CommandManager.literal("score")
//                        .then(CommandManager.argument("target", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
//                                .then((ArgumentBuilder<ServerCommandSource, ?>) ((RequiredArgumentBuilder) CommandManager.argument("targetObjective", ScoreboardObjectiveArgumentType.scoreboardObjective())
//                                        .then(CommandManager.literal("=")
//                                                .then(CommandManager.argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
//                                                        .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> ExecuteCommand.testScoreCondition(context, Integer::equals))))))
//                                        .then(CommandManager.literal("<")
//                                                .then(CommandManager.argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
//                                                        .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> ExecuteCommand.testScoreCondition(context, (a, b) -> a < b)))))
//                                        .then(CommandManager.literal("<=")
//                                                .then(CommandManager.argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
//                                                        .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> ExecuteCommand.testScoreCondition(context, (a, b) -> a <= b)))))
//                                        .then(CommandManager.literal(">")
//                                                .then(CommandManager.argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
//                                                        .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> ExecuteCommand.testScoreCondition(context, (a, b) -> a > b)))))
//                                        .then(CommandManager.literal(">=")
//                                                .then(CommandManager.argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
//                                                        .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> ExecuteCommand.testScoreCondition(context, (a, b) -> a >= b)))))
//                                        .then(CommandManager.literal("matches")
//                                                .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("range", NumberRangeArgumentType.intRange()), positive, context -> ExecuteCommand.testScoreMatch(context, NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(context, "range"))))))))
//                .then(CommandManager.literal("blocks")
//                        .then(CommandManager.argument("start", BlockPosArgumentType.blockPos())
//                                .then(CommandManager.argument("end", BlockPosArgumentType.blockPos())
//                                        .then((ArgumentBuilder<ServerCommandSource, ?>) ((RequiredArgumentBuilder) CommandManager.argument("destination", BlockPosArgumentType.blockPos())
//                                                .then(ExecuteCommand.addBlocksConditionLogic(root, CommandManager.literal("all"), positive, false)))
//                                                .then(ExecuteCommand.addBlocksConditionLogic(root, CommandManager.literal("masked"), positive, true))))))
//                .then(CommandManager.literal("entity")
//                        .then((ArgumentBuilder<ServerCommandSource, ?>) ((RequiredArgumentBuilder) CommandManager.argument("entities", EntityArgumentType.entities()).fork(root, context -> ExecuteCommand.getSourceOrEmptyForConditionFork(context, positive, !EntityArgumentType.getOptionalEntities(context, "entities").isEmpty()))).executes(ExecuteCommand.getExistsConditionExecute(positive, context -> EntityArgumentType.getOptionalEntities(context, "entities").size()))))
//                .then(CommandManager.literal("predicate")
//                        .then(ExecuteCommand.addConditionLogic(root, CommandManager.argument("predicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS), positive, context -> ExecuteCommand.testLootCondition((ServerCommandSource) context.getSource(), IdentifierArgumentType.getPredicateArgument(context, "predicate")))));
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
    private static EntityHitResult entityCast(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var distance = ctx.getArgument("distance", Double.class);
        Predicate<Entity> predicate;
        if (hasPredicate) {
            var condition = IdentifierArgumentType.getPredicateArgument(ctx, "hasPredicate");
            predicate = testRaycastEntity(source, condition);
        } else {
            predicate = ExecuteCommandMixin::pass;
        }
        var entity = source.getEntityOrThrow();
        var endPoints = getEndPoints(source, distance);
        var cast = ProjectileUtil.raycast(entity, endPoints.getLeft(), endPoints.getRight(), entity.getBoundingBox().expand(distance), predicate, 0);
        if (cast == null)
            throw new CommandException(Text.translatable("better_datapacks.raycast.error.no_target_found"));
        return cast;
    }

    @Unique
    private static ServerCommandSource raycastBlock(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        try {
            var rayCtx = blockCastContext(ctx, hasPredicate);
            var source = ctx.getSource();
            var world = source.getWorld();
            var cast = world.raycast(rayCtx);
            return source.withPosition(cast.getPos());
        } catch (RuntimeException err) {
            BetterDatapacks.LOGGER.info(err.getMessage());
            throw err;
        }
    }

    @Unique
    private static <T> boolean pass(T arg) {
        return true;
    }

    @Unique
    private static RaycastContext blockCastContext(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var distance = ctx.getArgument("distance", Double.class);
        var entity = source.getEntityOrThrow();
        var endPoints = getEndPoints(source, distance);
        var type = getLiteralEnumArgument(ctx, "collision_mode", Raycast.CollisionMode::values).getType();
        var fluid = getLiteralEnumArgument(ctx, "fluid_mode", Raycast.FluidMode::values).getFluidHandling();
        if (hasPredicate) {
            var blockPredicate = BlockPredicateArgumentType.getBlockPredicate(ctx, "block");
            return new Raycast.CustomShapeTypeRaycastContext(endPoints.getLeft(), endPoints.getRight(), type, fluid, entity, blockPredicate);
        }
        return new RaycastContext(endPoints.getLeft(), endPoints.getRight(), type, fluid, entity);
    }

    @Unique
    private static Pair<Vec3d, Vec3d> getEndPoints(ServerCommandSource source, double distance) {
        var anchor = source.getEntityAnchor();
        var start = anchor.positionAt(source);
        var rotation = source.getRotation();
        var direction = vectorFromRotation(rotation);
        var end = direction.multiply(distance).add(start);
        return new Pair<>(start, end);
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

    @Unique
    private static <E extends Enum<E> & StringIdentifiable> RequiredArgumentBuilder<ServerCommandSource, String> generateLiteralEnumArgument(String name, Supplier<E[]> valuesSupplier) {
        return argument(name, StringArgumentType.word()).suggests(literalEnumSuggestions(valuesSupplier));
    }

    @Unique
    private static <E extends Enum<E> & StringIdentifiable> SuggestionProvider<ServerCommandSource> literalEnumSuggestions(Supplier<E[]> valuesSupplier) {
        return ((context, builder) -> {
            for (var value : valuesSupplier.get()) {
                builder.suggest(value.asString());
            }
            return builder.buildFuture();
        });
    }

    @Unique
    private static <S, E extends Enum<E> & StringIdentifiable> E getLiteralEnumArgument(CommandContext<S> context, String name, Supplier<E[]> valuesSupplier) throws CommandSyntaxException {
        var arg = context.getArgument(name, String.class);
        for (var value : valuesSupplier.get()) {
            if (value.asString().equals(arg)) return value;
        }
        var text = Text.translatable("better_datapacks.raycast.error.invalid_mode");
        throw new CommandSyntaxException(new SimpleCommandExceptionType(text), text);
    }

}
