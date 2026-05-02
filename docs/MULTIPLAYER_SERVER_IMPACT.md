Just Storage Panel Multiplayer Server Impact
===========================================

Overview
--------

This document describes the expected multiplayer server impact of Just Storage Panel based on the current implementation.

The goal is to provide code-based, reproducible estimates for server owners and pack maintainers. The numbers below are implementation-derived upper bounds and scaling estimates, not wall-clock benchmark timings.

Design Summary
--------------

Just Storage Panel is lightweight while idle:

- The storage network is not backed by persistent block entities.
- The mod does not register a continuous background server tick for storage processing.
- Network work is performed on demand when a player interacts with a panel.

In practice, this means inactive networks have very little server impact. The main cost appears only during active panel usage.

How the Network Works
---------------------

When a player opens or uses an Access Panel or Crafting Panel, the server discovers the connected network starting from that panel position.

The current implementation:

- Traverses connected Logic Cables and panels.
- Checks adjacent blocks for accessible item handlers.
- Builds a temporary list of reachable storage endpoints.
- Reads inventory contents from those endpoints on demand.
- Aggregates matching items into a display list.
- Sorts the display list before sending the visible page to the client.

The traversal is capped at 2,048 visited network nodes per discovery pass.

To reduce repeated topology work, discovered endpoints are now cached per level and panel origin. The cache also keeps a reverse index of block positions that can affect each discovered network. When a block is placed, broken, or replaced by fluid, only the cached networks tied to that changed position are invalidated. The full level cache is only discarded when the level unloads.

What a Single Refresh Costs
---------------------------

One display refresh can include all of the following work:

- One full network discovery pass.
- Up to 2,048 visited network nodes.
- Up to 6 neighbor checks per visited node.
- Up to 12,288 neighbor examinations during traversal in the worst case.
- One full scan of every reachable inventory slot to build the visible item summary.
- One merge pass over the discovered item stacks.
- One sort pass over the merged item list.
- Synchronization of up to 54 visible display slots.
- Synchronization of up to 54 displayed item count values.

This means the dominant cost is usually proportional to the total number of reachable storage slots and the number of distinct item types in the network.

What Player Actions Trigger
---------------------------

The current behavior is demand-driven, but some actions are still expensive because they rebuild the display.

- Opening a panel triggers a full display build.
- Keeping a panel open triggers periodic passive refreshes instead of a full rebuild every server tick.
- Changing the page triggers one full refresh.
- Search input is debounced before it is sent to the server.
- One insert action triggers one storage operation pass and then one additional refresh pass.
- One extract action triggers one storage operation pass and then one additional refresh pass.

In other words, a single insert or extract click typically causes two full network-level passes overall: one pass for the item operation and one pass to rebuild the displayed contents.

The default passive refresh interval is 4 server ticks, which is about 0.2 seconds at 20 TPS. The default search debounce delay is also 4 client ticks.

Configuration
-------------

These defaults are configurable:

- Server passive refresh interval: `config/juststoragepanel-server.toml`
- Client search debounce delay: `config/juststoragepanel-client.toml`

Current config keys:

- `panel_updates.passiveRefreshIntervalTicks`
- `search.searchDebounceTicks`

Practical use:

- Lower `passiveRefreshIntervalTicks` for a more reactive panel at the cost of more server work.
- Raise `passiveRefreshIntervalTicks` to reduce steady-state server load for players who leave panels open.
- Lower `searchDebounceTicks` for faster search updates.
- Raise `searchDebounceTicks` to reduce packet spam while typing.

Quantified Examples
-------------------

The examples below use inventory slot counts because slot reads are a stable, implementation-derived unit of work.

Example: 50 double chests

- One vanilla double chest exposes 54 slots.
- 50 double chests expose about 2,700 slots.
- One display refresh reads about 2,700 slots.
- One insert click or extract click reads about 5,400 slots overall, because it performs the item operation and then refreshes the view.

If one player keeps such a panel open and the menu performs its passive refresh at the current 4-tick interval, the upper-bound read volume is roughly:

- 2,700 slot reads per refresh
- 5 refreshes per second
- about 13,500 slot reads per second for that one active viewer

This does not include the additional cost of:

- capability lookups for each endpoint
- item aggregation work
- item list sorting
- container synchronization to the client

Compared to the original per-tick refresh path, the default settings lower the steady-state passive read pressure for an open panel by about 75 percent, while keeping active actions immediate.

Scaling Rules
-------------

The server cost scales roughly with these factors:

- Number of connected storage slots
- Number of connected network nodes
- Number of distinct item variants in the network
- Number of players simultaneously viewing panels
- How often players type into search, switch pages, or move items

Practical interpretation:

- Small to medium networks with occasional access are usually fine for normal modded multiplayer.
- Large shared networks with many distinct items cost noticeably more CPU time.
- Multiple simultaneous viewers still scale roughly linearly, but repeated topology traversal is reduced because network endpoints are cached until the affected topology changes.

Crafting Panel Notes
--------------------

The Crafting Panel can be more expensive than the Access Panel during active recipe transfer because it may repeatedly pull ingredients from player inventory, buffered items, and the discovered network before refreshing the panel view.

This does not create meaningful idle cost, but it can increase burst load during rapid crafting interactions.

Operational Guidance for Server Owners
--------------------------------------

Just Storage Panel is suitable for multiplayer servers if used with reasonable network sizes.

Recommended expectations:

- Idle networks should have minimal measurable impact.
- Small and medium systems should be acceptable on most modded servers.
- Very large central storage systems should be tested with realistic concurrent usage.
- If TPS is a hard requirement, validate with a profiler on the target server pack instead of relying only on theoretical estimates.

Recommended profiling targets:

- one player opening and browsing a large panel
- two to five players using the same network simultaneously
- repeated search input on a populated network
- repeated insert and extract clicks on a large network
- bulk recipe transfer with the Crafting Panel

Bottom Line
-----------

Just Storage Panel has low idle overhead and on-demand server cost.

For multiplayer, the mod is generally safe for normal use, but active panel usage on very large shared networks can still become CPU-intensive because item contents are rebuilt on demand even though network topology is now cached and invalidated selectively.