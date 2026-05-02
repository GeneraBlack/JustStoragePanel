package de.juststoragepanel.network;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.ItemStackMap;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class StorageNetwork {
    private static final int MAX_NETWORK_NODES = 2048;
    private static final Map<Level, LevelCache> LEVEL_CACHES = new WeakHashMap<>();

    private final Level level;
    private final List<Endpoint> endpoints;

    private StorageNetwork(Level level, List<Endpoint> endpoints) {
        this.level = level;
        this.endpoints = endpoints;
    }

    public static StorageNetwork discover(Level level, BlockPos origin) {
        BlockPos cacheKey = origin.immutable();
        if (level.isClientSide) {
            return new StorageNetwork(level, discoverTopology(level, cacheKey).endpoints());
        }

        LevelCache levelCache = LEVEL_CACHES.computeIfAbsent(level, ignored -> new LevelCache());
        return levelCache.get(level, cacheKey);
    }

    public static void invalidateOnBlockBreak(BlockEvent.BreakEvent event) {
        invalidateAt(event.getLevel(), event.getPos());
    }

    public static void invalidateOnBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiPlaceEvent) {
            for (BlockSnapshot snapshot : multiPlaceEvent.getReplacedBlockSnapshots()) {
                invalidateAt(event.getLevel(), snapshot.getPos());
            }
            return;
        }

        invalidateAt(event.getLevel(), event.getPos());
    }

    public static void invalidateOnFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        invalidateAt(event.getLevel(), event.getPos());
    }

    public static void invalidateOnLevelUnload(LevelEvent.Unload event) {
        clearLevel(event.getLevel());
    }

    private static DiscoveryResult discoverTopology(Level level, BlockPos origin) {
        Map<BlockPos, Direction> discoveredEndpoints = new LinkedHashMap<>();
        Set<BlockPos> invalidationPositions = new HashSet<>();
        Deque<BlockPos> openSet = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        openSet.add(origin);
        while (!openSet.isEmpty() && visited.size() < MAX_NETWORK_NODES) {
            BlockPos current = openSet.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            invalidationPositions.add(current.immutable());

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                invalidationPositions.add(neighbor.immutable());

                BlockState neighborState = level.getBlockState(neighbor);
                if (NetworkConnectionHelper.isNetworkNode(neighborState)) {
                    if (!visited.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                    continue;
                }

                Direction accessSide = direction.getOpposite();
                if (NetworkConnectionHelper.resolveHandler(level, neighbor, accessSide) != null) {
                    discoveredEndpoints.putIfAbsent(neighbor, accessSide);
                }
            }
        }

        List<Endpoint> endpoints = discoveredEndpoints.entrySet().stream()
                .map(entry -> new Endpoint(entry.getKey(), entry.getValue()))
                .toList();

        return new DiscoveryResult(endpoints, invalidationPositions);
    }

    public List<NetworkItem> listItems() {
        return this.listItems("");
    }

    public List<NetworkItem> listItems(String searchQuery) {
        String normalizedQuery = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        Map<ItemStack, MutableNetworkItem> merged = ItemStackMap.createTypeAndTagMap();

        for (Endpoint endpoint : this.endpoints) {
            IItemHandler handler = endpoint.resolve(this.level);
            if (handler == null) {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }

                mergeStack(merged, stack);
            }
        }

        return merged.values().stream()
                .filter(item -> matchesSearch(item.displayStack, normalizedQuery))
                .sorted(Comparator
                        .comparing((MutableNetworkItem item) -> item.displayStack.getHoverName().getString().toLowerCase(Locale.ROOT))
                        .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item.displayStack.getItem()).toString()))
                .map(item -> new NetworkItem(item.displayStack.copy(), item.count))
                .toList();
    }

    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();
        for (Endpoint endpoint : this.endpoints) {
            IItemHandler handler = endpoint.resolve(this.level);
            if (handler == null) {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                remaining = handler.insertItem(slot, remaining, false);
                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return remaining;
    }

    public ItemStack extract(ItemStack template, int amount) {
        if (template.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack extractedTotal = ItemStack.EMPTY;
        int remaining = amount;
        for (Endpoint endpoint : this.endpoints) {
            IItemHandler handler = endpoint.resolve(this.level);
            if (handler == null) {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (inSlot.isEmpty() || !ItemStack.isSameItemSameComponents(inSlot, template)) {
                    continue;
                }

                ItemStack extracted = handler.extractItem(slot, remaining, false);
                if (extracted.isEmpty()) {
                    continue;
                }

                if (extractedTotal.isEmpty()) {
                    extractedTotal = extracted;
                } else {
                    extractedTotal.grow(extracted.getCount());
                }

                remaining -= extracted.getCount();
                if (remaining <= 0) {
                    return extractedTotal;
                }
            }
        }

        return extractedTotal;
    }

    private static void mergeStack(Map<ItemStack, MutableNetworkItem> merged, ItemStack stack) {
        ItemStack displayStack = stack.copy();
        displayStack.setCount(1);

        MutableNetworkItem existing = merged.get(displayStack);
        if (existing != null) {
            existing.count = Math.min(Integer.MAX_VALUE, existing.count + stack.getCount());
            return;
        }

        merged.put(displayStack, new MutableNetworkItem(displayStack, stack.getCount()));
    }

    private static void invalidateAt(LevelAccessor levelAccessor, BlockPos changedPos) {
        if (!(levelAccessor instanceof Level level) || level.isClientSide) {
            return;
        }

        LevelCache levelCache = LEVEL_CACHES.get(level);
        if (levelCache != null) {
            levelCache.invalidate(changedPos.immutable());
        }
    }

    private static void clearLevel(LevelAccessor levelAccessor) {
        if (levelAccessor instanceof Level level) {
            LEVEL_CACHES.remove(level);
        }
    }

    private static boolean matchesSearch(ItemStack stack, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        String hoverName = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (hoverName.contains(normalizedQuery)) {
            return true;
        }

        String itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        return itemKey.contains(normalizedQuery);
    }

    public record NetworkItem(ItemStack displayStack, int count) {
    }

    private record DiscoveryResult(List<Endpoint> endpoints, Set<BlockPos> invalidationPositions) {
    }

    private record Endpoint(BlockPos pos, @Nullable Direction side) {
        @Nullable
        private IItemHandler resolve(Level level) {
            return NetworkConnectionHelper.resolveHandler(level, this.pos, this.side);
        }
    }

    private record CachedNetwork(List<Endpoint> endpoints, Set<BlockPos> invalidationPositions) {
    }

    private static final class MutableNetworkItem {
        private final ItemStack displayStack;
        private int count;

        private MutableNetworkItem(ItemStack displayStack, int count) {
            this.displayStack = displayStack;
            this.count = count;
        }
    }

    private static final class LevelCache {
        private final Map<BlockPos, CachedNetwork> networksByOrigin = new HashMap<>();
        private final Map<BlockPos, Set<BlockPos>> originsByInvalidationPos = new HashMap<>();

        private StorageNetwork get(Level level, BlockPos origin) {
            CachedNetwork cachedNetwork = this.networksByOrigin.get(origin);
            if (cachedNetwork == null) {
                cachedNetwork = this.cache(level, origin);
            }

            return new StorageNetwork(level, cachedNetwork.endpoints());
        }

        private void invalidate(BlockPos changedPos) {
            Set<BlockPos> affectedOrigins = this.originsByInvalidationPos.get(changedPos);
            if (affectedOrigins == null || affectedOrigins.isEmpty()) {
                return;
            }

            for (BlockPos origin : List.copyOf(affectedOrigins)) {
                this.remove(origin);
            }
        }

        private CachedNetwork cache(Level level, BlockPos origin) {
            DiscoveryResult discoveryResult = discoverTopology(level, origin);
            CachedNetwork cachedNetwork = new CachedNetwork(discoveryResult.endpoints(), discoveryResult.invalidationPositions());
            this.networksByOrigin.put(origin, cachedNetwork);

            for (BlockPos invalidationPos : cachedNetwork.invalidationPositions()) {
                this.originsByInvalidationPos.computeIfAbsent(invalidationPos, ignored -> new HashSet<>()).add(origin);
            }

            return cachedNetwork;
        }

        private void remove(BlockPos origin) {
            CachedNetwork cachedNetwork = this.networksByOrigin.remove(origin);
            if (cachedNetwork == null) {
                return;
            }

            for (BlockPos invalidationPos : cachedNetwork.invalidationPositions()) {
                Set<BlockPos> origins = this.originsByInvalidationPos.get(invalidationPos);
                if (origins == null) {
                    continue;
                }

                origins.remove(origin);
                if (origins.isEmpty()) {
                    this.originsByInvalidationPos.remove(invalidationPos);
                }
            }
        }

        private void clear() {
            this.networksByOrigin.clear();
            this.originsByInvalidationPos.clear();
        }
    }
}