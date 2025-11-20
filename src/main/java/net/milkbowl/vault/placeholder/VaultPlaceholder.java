package net.milkbowl.vault.placeholder;

import com.google.common.collect.ImmutableMap;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import net.milkbowl.vault.placeholder.children.EconomyHook;
import net.milkbowl.vault.placeholder.children.PermissionHook;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;

public class VaultPlaceholder extends PlaceholderExpansion implements Configurable, Taskable {

    private final EconomyHook economyHook;
    private final PermissionHook permissionHook;

    public VaultPlaceholder() {
        this.economyHook = new EconomyHook(this);
        this.permissionHook = new PermissionHook();
    }

    @NotNull
    public String getIdentifier() {
        return "vault";
    }

    @NotNull
    public String getAuthor() {
        return "GroundbreakingMC";
    }

    @NotNull
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public Map<String, Object> getDefaults() {
        return ImmutableMap.<String, Object>builder()
                .put("formatting.us-number-format", "false")
                .put("formatting.thousands", "k")
                .put("formatting.millions", "M")
                .put("formatting.billions", "B")
                .put("formatting.trillions", "T")
                .put("formatting.quadrillions", "Q")
                .build();
    }

    @Override
    public void start() {
        this.economyHook.setup();
    }

    @Override
    public void stop() {

    }

    @Nullable
    public String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        } else if (params.startsWith("eco_")) {
            return this.economyHook.onRequest(player, params.substring(4));
        } else {
            return this.permissionHook.onRequest(player, params);
        }
    }
}
