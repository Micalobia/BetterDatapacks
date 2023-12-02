package dev.micalobia;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.loot.LootDataType;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.WorldView;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ExecuteRaycastSubcommand {
    private static final SuggestionProvider<ServerCommandSource> LOOT_CONDITIONS = (context, builder) -> {
        LootManager lootManager = context.getSource().getServer().getLootManager();
        return CommandSource.suggestIdentifiers(lootManager.getIds(LootDataType.PREDICATES), builder);
    };

    private static final double maxDistance = 32d;

    private static final DoubleArgumentType distanceArg = DoubleArgumentType.doubleArg(0, maxDistance);


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, LiteralCommandNode<ServerCommandSource> root) {
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(argument("distance", distanceArg).then(literal("at").then(literal("*").redirect(root, ctx -> raycastAt(ctx, false))).then(argument("predicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).redirect(root, ctx -> raycastAt(ctx, true)))))));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(argument("distance", distanceArg).then(literal("as").then(literal("*").redirect(root, ctx -> raycastAs(ctx, false))).then(argument("predicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).redirect(root, ctx -> raycastAs(ctx, true)))))));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(argument("distance", distanceArg).then(literal("positioned").then(literal("at").then(literal("*").then(literal("entity").redirect(root, ctx -> raycastPositionedAt(ctx, false, false)))).then(literal("*").then(literal("hit").redirect(root, ctx -> raycastPositionedAt(ctx, false, true)))).then(argument("hasPredicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).then(literal("entity").redirect(root, ctx -> raycastPositionedAt(ctx, true, false)))).then(argument("hasPredicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS).then(literal("hit").redirect(root, ctx -> raycastPositionedAt(ctx, true, true)))))))));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)).then(literal("raycast").then(argument("distance", distanceArg).then(literal("block").then(literal("*").then(generateLiteralEnumArgument("collision_mode", CollisionMode::values).then(generateLiteralEnumArgument("fluid_mode", FluidMode::values).redirect(root, ctx -> raycastBlock(ctx, false)))))).then(literal("block").then(argument("block", BlockPredicateArgumentType.blockPredicate(registryAccess)).then(generateLiteralEnumArgument("collision_mode", CollisionMode::values).then(generateLiteralEnumArgument("fluid_mode", FluidMode::values).redirect(root, ctx -> raycastBlock(ctx, true)))))))));
    }


    private static Vec3d vectorFromRotation(Vec2f rotation) {
        final float pi180 = (float) (Math.PI / 180d);
        final float f = MathHelper.cos((rotation.y + 90.0F) * pi180);
        final float g = MathHelper.sin((rotation.y + 90.0F) * pi180);
        final float h = MathHelper.cos(-rotation.x * pi180);
        final float i = MathHelper.sin(-rotation.x * pi180);
        Vec3d vec3d2 = new Vec3d(f * h, i, g * h);
        return new Vec3d(vec3d2.x, vec3d2.y, vec3d2.z);
    }


    private static ServerCommandSource raycastAt(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var cast = entityCast(ctx, hasPredicate);
        var entity = cast.getEntity();
        return source.withPosition(entity.getPos()).withRotation(entity.getRotationClient());
    }


    private static ServerCommandSource raycastAs(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var cast = entityCast(ctx, hasPredicate);
        var entity = cast.getEntity();
        return source.withEntity(entity);
    }


    private static ServerCommandSource raycastPositionedAt(CommandContext<ServerCommandSource> ctx, boolean hasPredicate, boolean hit) throws CommandSyntaxException {
        var source = ctx.getSource();
        var cast = entityCast(ctx, hasPredicate);
        if (hit) return source.withPosition(cast.getPos());
        else return source.withPosition(cast.getEntity().getPos());
    }


    private static EntityHitResult entityCast(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var distance = ctx.getArgument("distance", Double.class);
        Predicate<Entity> predicate;
        if (hasPredicate) {
            var condition = IdentifierArgumentType.getPredicateArgument(ctx, "hasPredicate");
            predicate = testRaycastEntity(source, condition);
        } else {
            predicate = ExecuteRaycastSubcommand::pass;
        }
        var entity = source.getEntityOrThrow();
        var endPoints = getEndPoints(source, distance);
        var cast = ProjectileUtil.raycast(entity, endPoints.getLeft(), endPoints.getRight(), entity.getBoundingBox().expand(distance), predicate, 0);
        if (cast == null)
            throw new CommandException(Text.translatable("better_datapacks.raycast.error.no_target_found"));
        return cast;
    }


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


    private static <T> boolean pass(T arg) {
        return true;
    }


    private static RaycastContext blockCastContext(CommandContext<ServerCommandSource> ctx, boolean hasPredicate) throws CommandSyntaxException {
        var source = ctx.getSource();
        var distance = ctx.getArgument("distance", Double.class);
        var entity = source.getEntityOrThrow();
        var endPoints = getEndPoints(source, distance);
        var type = getLiteralEnumArgument(ctx, "collision_mode", CollisionMode::values).getType();
        var fluid = getLiteralEnumArgument(ctx, "fluid_mode", FluidMode::values).getFluidHandling();
        if (hasPredicate) {
            var blockPredicate = BlockPredicateArgumentType.getBlockPredicate(ctx, "block");
            return new CustomShapeTypeRaycastContext(endPoints.getLeft(), endPoints.getRight(), type, fluid, entity, blockPredicate);
        }
        return new RaycastContext(endPoints.getLeft(), endPoints.getRight(), type, fluid, entity);
    }

    private static Pair<Vec3d, Vec3d> getEndPoints(ServerCommandSource source, double distance) {
        var anchor = source.getEntityAnchor();
        var start = anchor.positionAt(source);
        var rotation = source.getRotation();
        var direction = vectorFromRotation(rotation);
        var end = direction.multiply(distance).add(start);
        return new Pair<>(start, end);
    }


    private static Predicate<Entity> testRaycastEntity(ServerCommandSource source, LootCondition condition) {
        var builder = new LootContextParameterSet.Builder(source.getWorld());
        return entity -> {
            var set = builder.add(LootContextParameters.ORIGIN, entity.getPos()).add(LootContextParameters.THIS_ENTITY, entity).build(LootContextTypes.COMMAND);
            var ctx = new LootContext.Builder(set).build(Optional.empty());
            ctx.markActive(LootContext.predicate(condition));
            return condition.test(ctx);
        };
    }

    public static <E extends Enum<E> & StringIdentifiable> RequiredArgumentBuilder<ServerCommandSource, String> generateLiteralEnumArgument(String name, Supplier<E[]> valuesSupplier) {
        return argument(name, StringArgumentType.word()).suggests(literalEnumSuggestions(valuesSupplier));
    }

    public static <E extends Enum<E> & StringIdentifiable> SuggestionProvider<ServerCommandSource> literalEnumSuggestions(Supplier<E[]> valuesSupplier) {
        return ((context, builder) -> {
            for (var value : valuesSupplier.get()) {
                builder.suggest(value.asString());
            }
            return builder.buildFuture();
        });
    }

    public static <S, E extends Enum<E> & StringIdentifiable> E getLiteralEnumArgument(CommandContext<S> context, String name, Supplier<E[]> valuesSupplier) throws CommandSyntaxException {
        var arg = context.getArgument(name, String.class);
        for (var value : valuesSupplier.get()) {
            if (value.asString().equals(arg)) return value;
        }
        var text = Text.translatable("better_datapacks.raycast.error.invalid_mode");
        throw new CommandSyntaxException(new SimpleCommandExceptionType(text), text);
    }

    public static class CustomShapeTypeRaycastContext extends RaycastContext {
        private final Predicate<CachedBlockPosition> predicate;

        public CustomShapeTypeRaycastContext(Vec3d start, Vec3d end, ShapeType provider, FluidHandling fluidHandling, Entity entity, Predicate<CachedBlockPosition> predicate) {
            super(start, end, ShapeType.COLLIDER, fluidHandling, entity);
            this.predicate = predicate;
        }

        @Override
        public VoxelShape getBlockShape(BlockState state, BlockView world, BlockPos pos) {
            if (!(world instanceof WorldView view)) throw new RuntimeException(world.getClass().getSimpleName());
            var cached = new CachedBlockPosition(view, pos, false);
            if (predicate.test(cached)) return super.getBlockShape(state, world, pos);
            return VoxelShapes.empty();
        }
    }

    public enum CollisionMode implements StringIdentifiable {
        COLLIDER(RaycastContext.ShapeType.COLLIDER, "collider"), OUTLINE(RaycastContext.ShapeType.OUTLINE, "outline"), VISUAL(RaycastContext.ShapeType.VISUAL, "visual");

        private final String name;
        private final RaycastContext.ShapeType type;

        CollisionMode(RaycastContext.ShapeType type, String name) {
            this.type = type;
            this.name = name;
        }

        public RaycastContext.ShapeType getType() {
            return type;
        }

        @Override
        public String asString() {
            return name;
        }
    }

    public enum FluidMode implements StringIdentifiable {
        NONE(RaycastContext.FluidHandling.NONE, "none"), SOURCE_ONLY(RaycastContext.FluidHandling.SOURCE_ONLY, "source"), ANY(RaycastContext.FluidHandling.ANY, "any");


        private final RaycastContext.FluidHandling handling;

        private final String name;

        FluidMode(RaycastContext.FluidHandling handling, String name) {
            this.handling = handling;
            this.name = name;
        }

        public RaycastContext.FluidHandling getFluidHandling() {
            return handling;
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
