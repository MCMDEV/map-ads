package dev.cerus.mapads;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigModel {

    public int maxImageSize;
    public List<String> trustedImageUrls;
    public boolean enableAdvOpt;
    public int minAdMins;
    public int maxAdMins;
    public int minsStep;
    public double pricePerMin;
    public boolean useColorCache;
    public boolean updateMessage;
    public boolean onlyGroups;
    public boolean deductEachScreenInGroup;
    public boolean enableCustomDespawning;

    public ConfigModel(final FileConfiguration configuration) {
        this.maxImageSize = configuration.getInt("images.max-size");
        this.trustedImageUrls = configuration.getStringList("images.url-whitelist");
        this.enableAdvOpt = configuration.getBoolean("enable-advanced-optimization-algorithm");
        this.minAdMins = configuration.getInt("ads.min-ad-minutes");
        this.maxAdMins = configuration.getInt("ads.max-ad-minutes");
        this.minsStep = configuration.getInt("ads.step");
        this.pricePerMin = configuration.getDouble("ads.price-per-minute");
        this.useColorCache = configuration.getBoolean("images.use-color-cache");
        this.updateMessage = configuration.getBoolean("send-update-message");
        this.onlyGroups = configuration.getBoolean("only-show-groups");
        this.deductEachScreenInGroup = configuration.getBoolean("deduct-each-screen-in-group");
        this.enableCustomDespawning = configuration.getBoolean("custom-despawning");
    }

}
