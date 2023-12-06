package dev.micalobia;

import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.WorldView;

import java.util.function.Predicate;

public class Raycast {
    public enum BlockHitMode implements StringIdentifiable {
        BLOCK("block"), HIT("hit");

        private final String name;

        BlockHitMode(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
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

    public static class CustomShapeTypeRaycastContext extends RaycastContext {
        private final Predicate<CachedBlockPosition> predicate;

        public CustomShapeTypeRaycastContext(Vec3d start, Vec3d end, ShapeType provider, FluidHandling fluidHandling, Entity entity, Predicate<CachedBlockPosition> predicate) {
            super(start, end, provider, fluidHandling, entity);
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

    public record VecSet(Vec3d start, Vec3d end, Vec3d multipliedDirection) {
    }
}
