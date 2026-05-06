package de.juststoragepanel.network;

import de.juststoragepanel.block.AbstractPanelBlock;
import de.juststoragepanel.block.LogicCableBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public final class NetworkConnectionHelper {
    private static final String SOPHISTICATED_STORAGE_NAMESPACE = "sophisticatedstorage";
    private static final List<String> SOPHISTICATED_STORAGE_ACCESS_MARKERS = List.of(
            "controller",
            "storage_io",
            "storage_input",
            "storage_output");

    private NetworkConnectionHelper() {
    }

    public static boolean isNetworkNode(BlockState state) {
        return state.getBlock() instanceof LogicCableBlock || state.getBlock() instanceof AbstractPanelBlock;
    }

    public static boolean canCableConnect(LevelReader level, BlockPos cablePos, Direction direction) {
        BlockPos targetPos = cablePos.relative(direction);
        BlockState targetState = level.getBlockState(targetPos);
        return isNetworkNode(targetState)
                || isSophisticatedStorageAccessBlock(targetState)
                || hasAccessibleItemHandler(level, targetPos, direction.getOpposite());
    }

    public static boolean hasAccessibleItemHandler(LevelReader level, BlockPos pos, @Nullable Direction side) {
        return level instanceof Level actualLevel && resolveHandler(actualLevel, pos, side) != null;
    }

    @Nullable
    public static ResourceHandler<ItemResource> resolveHandler(Level level, BlockPos pos, @Nullable Direction side) {
        BlockState state = level.getBlockState(pos);
        if (isSophisticatedStorageAccessBlock(state)) {
            ResourceHandler<ItemResource> prioritized = resolveSophisticatedStorageHandler(level, pos, side);
            if (prioritized != null) {
                return prioritized;
            }
        }

        return resolveDefaultHandler(level, pos, side);
    }

    public static boolean isSophisticatedStorageAccessBlock(BlockState state) {
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!SOPHISTICATED_STORAGE_NAMESPACE.equals(key.getNamespace())) {
            return false;
        }

        String path = key.getPath();
        return SOPHISTICATED_STORAGE_ACCESS_MARKERS.stream().anyMatch(path::contains);
    }

    @Nullable
    private static ResourceHandler<ItemResource> resolveSophisticatedStorageHandler(Level level, BlockPos pos, @Nullable Direction side) {
        if (side != null) {
            ResourceHandler<ItemResource> sided = level.getCapability(Capabilities.Item.BLOCK, pos, side);
            if (sided != null) {
                return sided;
            }
        }

        ResourceHandler<ItemResource> unsided = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        if (unsided != null) {
            return unsided;
        }

        for (Direction direction : Direction.values()) {
            if (direction == side) {
                continue;
            }

            ResourceHandler<ItemResource> directional = level.getCapability(Capabilities.Item.BLOCK, pos, direction);
            if (directional != null) {
                return directional;
            }
        }

        return null;
    }

    @Nullable
    private static ResourceHandler<ItemResource> resolveDefaultHandler(Level level, BlockPos pos, @Nullable Direction side) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null && side != null) {
            handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        }
        return handler;
    }
}