package dev.cerus.mapads.gui;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.api.event.AdvertReviewEvent;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ItemBuilder;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import dev.pelkum.yamif.components.Button;
import dev.pelkum.yamif.components.Item;
import dev.pelkum.yamif.grid.Coordinate;
import dev.pelkum.yamif.grid.SlotRange;
import dev.pelkum.yamif.gui.GUI;
import dev.pelkum.yamif.gui.GUIBuilder;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DetailsGui {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final AdvertStorage advertStorage;
    private final ImageStorage imageStorage;
    private final AdScreenStorage adScreenStorage;
    private final Economy economy;
    private final Player player;
    private final Advertisement advertisement;

    public DetailsGui(final AdvertStorage advertStorage,
                      final ImageStorage imageStorage,
                      final AdScreenStorage adScreenStorage,
                      final Economy economy,
                      final Player player,
                      final Advertisement advertisement) {
        this.advertStorage = advertStorage;
        this.imageStorage = imageStorage;
        this.adScreenStorage = adScreenStorage;
        this.economy = economy;
        this.player = player;
        this.advertisement = advertisement;
    }

    public void open() {
        this.player.playSound(this.player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 1);
        final GUI gui = new GUIBuilder("Advertisement", 3)
                .withInteractionPolicy(SlotRange.full(), false)
                .withComponents(SlotRange.full(), new Item(new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName(" ").build()))
                .withComponents(
                        SlotRange.single(Coordinate.fromSlot(1 + 9)),
                        new Item(new ItemBuilder(Material.FILLED_MAP)
                                .setName(L10n.get("gui.details.button.info.name"))
                                .setLore(L10n.getList("gui.details.button.info.lore").stream()
                                        .map(s -> s.replace("{0}", Bukkit.getOfflinePlayer(this.advertisement.getPlayerUuid()).getName()))
                                        .map(s -> s.replace("{1}", String.valueOf(this.advertisement.getPurchasedMinutes())))
                                        .map(s -> s.replace("{2}", String.valueOf(this.advertisement.getPricePaid())))
                                        .map(s -> s.replace("{3}", this.advertisement.getAdScreenId()))
                                        .map(s -> s.replace("{4}", DATE_FORMAT.format(this.advertisement.getPurchaseTimestamp())))
                                        .collect(Collectors.toList()))
                                .build())
                )
                .withComponents(
                        SlotRange.single(Coordinate.fromSlot(4 + 9)),
                        new Button(new ItemBuilder(Material.SPYGLASS)
                                .setName(L10n.get("gui.details.button.viewimg.name"))
                                .build(), event -> {
                            this.player.closeInventory();
                            this.player.sendMessage(L10n.getPrefixed("misc.please_wait"));
                            this.imageStorage.getMapImage(this.advertisement.getImageId()).whenComplete((mapImage, throwable) -> {
                                final MapScreen screen = MapScreenRegistry.getScreen(this.adScreenStorage
                                        .getAdScreen(this.advertisement.getAdScreenId()).getScreenId());
                                ReviewerUtil.markAsReviewer(this.player, mapImage, screen);
                                ReviewerUtil.sendImage(this.player, screen, mapImage);
                                this.player.sendMessage(L10n.getPrefixed("misc.visible"));
                                this.player.spigot().sendMessage(new ComponentBuilder("§6§lCLICK HERE")
                                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                "/mapads screen teleport " + this.advertisement.getAdScreenId()))
                                        .append(" §7to teleport to the ad-screen", ComponentBuilder.FormatRetention.NONE)
                                        .create());
                                this.player.spigot().sendMessage(new ComponentBuilder("§6§lCLICK HERE")
                                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                "/mapads review single " + this.advertisement.getAdvertId()))
                                        .append(" §7to re-open the GUI", ComponentBuilder.FormatRetention.NONE)
                                        .create());
                            });
                        })
                )
                .withComponents(
                        SlotRange.single(Coordinate.fromSlot(6 + 9)),
                        new Button(new ItemBuilder(Material.EMERALD)
                                .setName(L10n.get("gui.details.button.accept.name"))
                                .build(), event -> {
                            ConfirmationGui.create(L10n.get("gui.details.button.accept.confirm.title"), () -> new ItemBuilder(Material.PAPER)
                                            .setName(L10n.get("gui.details.button.accept.confirm.name"))
                                            .setLore(L10n.getList("gui.details.button.accept.confirm.lore"))
                                            .build())
                                    .onYes(() -> {
                                        if (this.cancelReview(AdvertReviewEvent.Result.ACCEPT)) {
                                            return;
                                        }

                                        this.advertisement.setReviewed(true);
                                        this.advertStorage.updateAdvert(this.advertisement).whenComplete((unused, throwable) -> {
                                            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(MapAdsPlugin.class), () -> {
                                                this.player.closeInventory();
                                                this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
                                                this.player.performCommand("mapads review list");
                                            });
                                        });
                                    })
                                    .onNo(this::open)
                                    .open(this.player);
                        })
                )
                .withComponents(
                        SlotRange.single(Coordinate.fromSlot(7 + 9)),
                        new Button(new ItemBuilder(Material.BARRIER)
                                .setName("§cDeny (delete)")
                                .build(), event -> {
                            ConfirmationGui.create("Confirmation", () -> new ItemBuilder(Material.PAPER)
                                            .setName("§fAre you sure?")
                                            .setLore("§7This action can not be undone.")
                                            .build())
                                    .onYes(() -> {
                                        if (this.cancelReview(AdvertReviewEvent.Result.DENY)) {
                                            return;
                                        }

                                        this.imageStorage.deleteMapImages(this.advertisement.getImageId());
                                        this.advertStorage.deleteAdverts(this.advertisement.getAdvertId()).whenComplete((unused, throwable) -> {
                                            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(MapAdsPlugin.class), () -> {
                                                this.economy.depositPlayer(Bukkit.getOfflinePlayer(this.advertisement.getPlayerUuid()), this.advertisement.getPricePaid());
                                                this.player.closeInventory();
                                                this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
                                                this.player.performCommand("mapads review list");
                                            });
                                        });
                                    })
                                    .onNo(this::open)
                                    .open(this.player);
                        })
                )
                .withComponents(
                        SlotRange.single(Coordinate.fromSlot(8 + 9)),
                        new Button(new ItemBuilder(Material.IRON_DOOR)
                                .setName(L10n.get("gui.details.button.back.name"))
                                .build(), event -> {
                            this.player.closeInventory();
                            this.player.playSound(this.player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 1);
                            this.player.performCommand("mapads review list");
                        })
                )
                .build();
        gui.open(JavaPlugin.getPlugin(MapAdsPlugin.class), this.player);
    }

    private boolean cancelReview(final AdvertReviewEvent.Result result) {
        final AdvertReviewEvent ev = new AdvertReviewEvent(this.player, this.advertisement, result);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            this.player.sendMessage(L10n.getPrefixed("api.event.review.cancelled"));
            this.player.closeInventory();
            this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            return true;
        }
        return false;
    }

}