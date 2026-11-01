package dev.tidings.vanilla;

import dev.tidings.core.addon.Addon;
import dev.tidings.core.addon.AddonContext;
import dev.tidings.core.addon.Icon;
import dev.tidings.core.addon.Link;
import dev.tidings.core.addon.Notification;
import dev.tidings.forge.TidingsAddons;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Map;

final class VanillaAddon {
    private final AddonContext tidings;
    private final StatsCache stats;

    private VanillaAddon(AddonContext tidings) {
        this.tidings = tidings;
        this.stats = new StatsCache(tidings);
    }

    static void register(FMLJavaModLoadingContext context) {
        Holder holder = new Holder();
        AddonContext tidings =
                TidingsAddons.register(
                        context,
                        Addon.builder("Stats")
                                .icon(Icon.CHART)
                                .view("me", r -> holder.views.me(r))
                                .view("top", r -> holder.views.top(r))
                                .view("player", r -> holder.views.player(r))
                                .view("advancements", r -> holder.views.advancements(r)));
        VanillaAddon addon = new VanillaAddon(tidings);
        holder.views = new Views(tidings, addon.stats);
        MinecraftForge.EVENT_BUS.register(addon);
    }

    private static final class Holder {
        Views views;
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        stats.serverStarted(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        stats.serverStopped();
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stats.playerLeft(player.server, player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return; // automation, not a person
        if (!player.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) return;
        notifyAbout(player, player.getCombatTracker().getDeathMessage());
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return; // automation, not a person
        Advancement advancement = event.getAdvancement();
        DisplayInfo display = advancement.getDisplay();
        if (display == null
                || display.getFrame() != FrameType.CHALLENGE
                || !display.shouldAnnounceChat()
                || !player.level()
                        .getGameRules()
                        .getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) {
            return;
        }
        notifyAbout(
                player,
                Component.translatable(
                        "chat.type.advancement.challenge",
                        player.getDisplayName(),
                        advancement.getChatComponent()));
    }

    private void notifyAbout(ServerPlayer player, Component message) {
        String text = message.getString();
        if (text.length() > Notification.MAX_TEXT_LENGTH) {
            text = text.substring(0, Notification.MAX_TEXT_LENGTH - 1) + "…";
        }
        tidings.notify(
                Notification.of(text)
                        .about(
                                Views.profile(
                                        tidings,
                                        player.getUUID(),
                                        player.getGameProfile().getName()))
                        .link(Link.to("player", Map.of("id", player.getUUID().toString()))));
    }
}
