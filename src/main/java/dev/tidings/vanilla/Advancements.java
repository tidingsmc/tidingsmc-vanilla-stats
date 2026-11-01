package dev.tidings.vanilla;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.MinecraftServer;

final class Advancements {
    private Advancements() {}

    static List<Advancement> shown(MinecraftServer server) {
        List<Advancement> shown = new ArrayList<>();
        for (Advancement advancement : server.getAdvancements().getAllAdvancements()) {
            if (advancement.getDisplay() != null) shown.add(advancement);
        }
        return shown;
    }

    static Set<String> ids(List<Advancement> shown) {
        Set<String> ids = new HashSet<>();
        for (Advancement advancement : shown) ids.add(advancement.getId().toString());
        return ids;
    }

    static Map<Advancement, List<Advancement>> byTab(List<Advancement> shown) {
        Map<Advancement, List<Advancement>> byRoot = new HashMap<>();
        for (Advancement advancement : shown) {
            byRoot.computeIfAbsent(root(advancement), root -> new ArrayList<>()).add(advancement);
        }
        Map<Advancement, List<Advancement>> tabs = new LinkedHashMap<>();
        byRoot.keySet().stream()
                .filter(root -> root.getDisplay() != null)
                .sorted(Comparator.comparing(Advancements::title))
                .forEach(root -> tabs.put(root, byRoot.get(root)));
        return tabs;
    }

    static Advancement shownParent(Advancement advancement) {
        Advancement above = advancement.getParent();
        while (above != null && above.getDisplay() == null) above = above.getParent();
        return above;
    }

    static Advancement root(Advancement advancement) {
        while (advancement.getParent() != null) advancement = advancement.getParent();
        return advancement;
    }

    static String title(Advancement advancement) {
        DisplayInfo display = advancement.getDisplay();
        return display == null ? advancement.getId().toString() : display.getTitle().getString();
    }

    static Instant doneAt(AdvancementProgress progress) {
        List<Instant> obtained = new ArrayList<>();
        for (String criterion : progress.getCompletedCriteria()) {
            CriterionProgress c = progress.getCriterion(criterion);
            if (c != null && c.getObtained() != null) obtained.add(c.getObtained().toInstant());
        }
        return PlayerStats.madeAt(obtained);
    }
}
