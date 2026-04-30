package de.juststoragepanel.network;

import de.juststoragepanel.block.AbstractPanelBlock;
import de.juststoragepanel.block.LogicCableBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class StorageNetwork {
    private static final int MAX_NETWORK_NODES = 2048;

    private final Level level;
    private final List<Endpoint> endpoints;

    private StorageNetwork(Level level, List<Endpoint> endpoints) {
        this.level = level;
        this.endpoints = endpoints;
    }

    public static StorageNetwork discover(Level level, BlockPos origin) {
        Map<BlockPos, Direction> discoveredEndpoints = new LinkedHashMap<>();
        Deque<BlockPos> openSet = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        openSet.add(origin);
        while (!openSet.isEmpty() && visited.size() < MAX_NETWORK_NODES) {
            BlockPos current = openSet.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
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

        return new StorageNetwork(level, endpoints);
    }

    public List<NetworkItem> listItems() {
        return this.listItems("");
    }

    public List<NetworkItem> listItems(String searchQuery) {
        List<MutableNetworkItem> merged = new ArrayList<>();

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

        merged.sort(Comparator
                .comparing((MutableNetworkItem item) -> item.displayStack.getHoverName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item.displayStack.getItem()).toString()));

        String normalizedQuery = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);

        return merged.stream()
            .filter(item -> matchesSearch(item.displayStack, normalizedQuery))
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

    private static void mergeStack(List<MutableNetworkItem> merged, ItemStack stack) {
        for (MutableNetworkItem item : merged) {
            if (ItemStack.isSameItemSameComponents(item.displayStack, stack)) {
                item.count = Math.min(Integer.MAX_VALUE, item.count + stack.getCount());
                return;
            }
        }

        ItemStack displayStack = stack.copy();
        displayStack.setCount(1);
        merged.add(new MutableNetworkItem(displayStack, stack.getCount()));
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

    private record Endpoint(BlockPos pos, @Nullable Direction side) {
        @Nullable
        private IItemHandler resolve(Level level) {
            return NetworkConnectionHelper.resolveHandler(level, this.pos, this.side);
        }
    }

    private static final class MutableNetworkItem {
        private final ItemStack displayStack;
        private int count;

        private MutableNetworkItem(ItemStack displayStack, int count) {
            this.displayStack = displayStack;
            this.count = count;
        }
    }
}