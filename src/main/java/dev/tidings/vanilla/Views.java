package dev.tidings.vanilla;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.tidings.core.PlayerProfile;
import dev.tidings.core.addon.AddonContext;
import dev.tidings.core.addon.AddonError;
import dev.tidings.core.addon.Block;
import dev.tidings.core.addon.Link;
import dev.tidings.core.addon.Request;
import dev.tidings.core.addon.View;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

final class Views {
    /** How many players the leaderboard lists */
    static final int TOP = 10;

    private final AddonContext tidings;
    private final StatsCache stats;

    Views(AddonContext tidings, StatsCache stats) {
        this.tidings = tidings;
        this.stats = stats;
    }

    View me(Request request) {
        MinecraftServer server = server();
        UUID me = request.player().id();
        Locale locale = Format.locale(request.locale());
        List<Advancement> shown = Advancements.shown(server);
        PlayerStats mine =
                stats.of(server, me, shown).orElse(new PlayerStats(me, null, Map.of(), Map.of()));
        List<Block> blocks = new ArrayList<>(statsBlocks(mine, shown, locale));
        blocks.add(Block.button("Leaderboards", Link.to("top")));
        if (tidings.isHidden(me)) {
            blocks.add(
                    Block.muted(
                            "You're hidden from phones (/tidings hide), so other players don't see"
                                    + " you on leaderboards."));
        }
        return View.of(null, blocks);
    }

    View player(Request request) {
        MinecraftServer server = server();
        List<Advancement> shown = Advancements.shown(server);
        PlayerStats them = visible(server, request, uuid(request.param("id")), shown);
        Locale locale = Format.locale(request.locale());
        List<Block> blocks = new ArrayList<>();
        if (server.getPlayerList().getPlayer(them.id()) != null) {
            blocks.add(Block.muted("Playing now"));
        }
        blocks.addAll(statsBlocks(them, shown, locale));
        return View.of(them.name(), blocks).player(profile(them));
    }

    View top(Request request) {
        MinecraftServer server = server();
        Ranking by = Ranking.byId(request.param("by", Ranking.DEATHS.id));
        Locale locale = Format.locale(request.locale());
        UUID me = request.player().id();
        List<Advancement> shownList = Advancements.shown(server);
        Set<String> shown = Advancements.ids(shownList);

        List<Block.Choice> choices = new ArrayList<>();
        for (Ranking ranking : Ranking.values()) {
            choices.add(
                    Block.Choice.of(
                            ranking.label,
                            Link.to("top", Map.of("by", ranking.id)),
                            ranking == by));
        }

        record Ranked(PlayerStats stats, long value) {}
        List<Ranked> ranked =
                stats.all(server, shownList).stream()
                        .filter(s -> s.id().equals(me) || !tidings.isHidden(s.id()))
                        .map(s -> new Ranked(s, by.value(s, shown)))
                        .sorted(
                                Comparator.comparingLong(Ranked::value)
                                        .reversed()
                                        .thenComparing(r -> String.valueOf(r.stats.name())))
                        .toList();

        List<Block.ListItem> rows = new ArrayList<>();
        String myRank = null;
        int rank = 0;
        long previous = Long.MIN_VALUE;
        for (int i = 0; i < ranked.size(); i++) {
            Ranked r = ranked.get(i);
            // Ties share a rank: 1, 2, 2, 4.
            if (r.value != previous) rank = i + 1;
            previous = r.value;
            boolean mine = r.stats.id().equals(me);
            if (mine) myRank = "#" + rank + " · " + by.format(r.value, locale);
            if (rows.size() < TOP) {
                rows.add(
                        Block.ListItem.of(r.stats.name())
                                .subtitle(mine ? "#" + rank + " · You" : "#" + rank)
                                .trailing(by.format(r.value, locale))
                                .player(profile(r.stats))
                                .link(Link.to("player", Map.of("id", r.stats.id().toString()))));
            }
        }

        List<Block> blocks = new ArrayList<>();
        blocks.add(Block.choices(choices));
        blocks.add(Block.list(rows).empty("Nobody has played here yet"));
        if (myRank != null && ranked.size() > TOP) blocks.add(Block.muted("You: " + myRank));
        if (!stats.loaded()) blocks.add(Block.muted("Still reading everyone's stats…"));
        return View.of("Leaderboard", blocks);
    }

    View advancements(Request request) {
        MinecraftServer server = server();
        List<Advancement> shown = Advancements.shown(server);
        PlayerStats them =
                visible(
                        server,
                        request,
                        uuid(request.param("id", request.player().id().toString())),
                        shown);
        Locale locale = Format.locale(request.locale());
        Map<Advancement, List<Advancement>> tabs = Advancements.byTab(shown);
        if (tabs.isEmpty()) return View.of("Advancements", Block.text("This server has none."));

        Advancement tab = tabs.keySet().iterator().next();
        String asked = request.param("tab", null);
        for (Advancement root : tabs.keySet()) {
            if (root.getId().toString().equals(asked)) tab = root;
        }

        List<Block.Choice> choices = new ArrayList<>();
        for (Advancement root : tabs.keySet()) {
            choices.add(
                    Block.Choice.of(
                            Advancements.title(root),
                            Link.to(
                                    "advancements",
                                    Map.of(
                                            "id", them.id().toString(),
                                            "tab", root.getId().toString())),
                            root == tab));
        }

        AdvancementTree tree = new AdvancementTree(them, locale);
        tree.add(tab, null, false);

        List<Block> blocks = new ArrayList<>();
        blocks.add(Block.choices(choices));
        blocks.add(
                Block.progress(
                        Advancements.title(tab), tree.done, Math.max(1, tabs.get(tab).size())));
        blocks.add(Block.tree(tree.nodes));
        if (tree.secrets > 0) {
            blocks.add(
                    Block.muted(
                            tree.secrets == 1
                                    ? "And a secret one, revealed once it's made."
                                    : "And "
                                            + tree.secrets
                                            + " secret ones, revealed once they're made."));
        }
        String title =
                them.id().equals(request.player().id())
                        ? "Advancements"
                        : them.name() + "'s advancements";
        return View.of(title, blocks);
    }

    private static final class AdvancementTree {
        private final PlayerStats player;
        private final Locale locale;
        final List<Block.TreeNode> nodes = new ArrayList<>();
        int done;
        int secrets;

        AdvancementTree(PlayerStats player, Locale locale) {
            this.player = player;
            this.locale = locale;
        }

        void add(Advancement advancement, String parent, boolean secret) {
            DisplayInfo display = advancement.getDisplay();
            String id = advancement.getId().toString();
            String shownAs = parent;
            if (display != null) {
                Instant at = player.advancements().get(id);
                if (at != null) done++;
                if (at == null && (secret || display.isHidden())) {
                    secrets++;
                    secret = true;
                } else {
                    nodes.add(node(advancement, display, id, parent, at));
                    shownAs = id;
                }
            }
            for (Advancement child : advancement.getChildren()) add(child, shownAs, secret);
        }

        private Block.TreeNode node(
                Advancement advancement,
                DisplayInfo display,
                String id,
                String parent,
                Instant at) {
            Advancement above = Advancements.shownParent(advancement);
            Block.TreeNode.State state =
                    at != null
                            ? Block.TreeNode.State.DONE
                            : above == null
                                            || player.advancements()
                                                    .containsKey(above.getId().toString())
                                    ? Block.TreeNode.State.AVAILABLE
                                    : Block.TreeNode.State.LOCKED;
            Block.TreeNode node =
                    Block.TreeNode.of(id, parent, display.getTitle().getString())
                            .subtitle(display.getDescription().getString())
                            .state(state);
            if (at != null && !at.equals(PlayerStats.UNKNOWN_TIME)) {
                node = node.trailing(Format.date(at, locale));
            }
            return switch (display.getFrame()) {
                case CHALLENGE -> node.badge("Challenge");
                case GOAL -> node.badge("Goal");
                case TASK -> node;
            };
        }
    }

    private List<Block> statsBlocks(PlayerStats s, List<Advancement> shownList, Locale locale) {
        Set<String> shown = Advancements.ids(shownList);
        return List.of(
                Block.stats(
                        Block.Stat.of("Deaths", Format.number(s.deaths(), locale)),
                        Block.Stat.of("Play time", Format.duration(s.playTimeTicks(), locale)),
                        Block.Stat.of("Walked", Format.distance(s.walkedCm(), locale)),
                        Block.Stat.of("Travelled", Format.distance(s.travelledCm(), locale)),
                        Block.Stat.of("Mob kills", Format.number(s.mobKills(), locale)),
                        Block.Stat.of("Player kills", Format.number(s.playerKills(), locale))),
                Block.progress(
                        "Advancements", s.advancementsDone(shown), Math.max(1, shown.size())),
                Block.button(
                        "See advancements",
                        Link.to("advancements", Map.of("id", s.id().toString()))));
    }

    private PlayerStats visible(
            MinecraftServer server, Request request, UUID id, List<Advancement> shown) {
        boolean self = id.equals(request.player().id());
        if (!self && tidings.isHidden(id)) {
            // hidden players aren't listed
            throw new AddonError("No stats for this player");
        }
        return stats.of(server, id, shown)
                .orElseThrow(
                        () ->
                                new AddonError(
                                        self
                                                ? "You haven't played here yet"
                                                : "No stats for this player"));
    }

    private PlayerProfile profile(PlayerStats s) {
        return profile(tidings, s.id(), s.name());
    }

    static PlayerProfile profile(AddonContext tidings, UUID id, String name) {
        return tidings.player(id).orElse(new PlayerProfile(id, name, null));
    }

    private static UUID uuid(String param) {
        try {
            return UUID.fromString(param);
        } catch (IllegalArgumentException e) {
            throw new AddonError("No stats for this player");
        }
    }

    private static MinecraftServer server() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new AddonError("The server is starting or stopping");
        return server;
    }
}
