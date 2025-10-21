package net.milkbowl.vault;

import net.milkbowl.vault.placeholder.VaultPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class Vault extends JavaPlugin {

    private VaultPlaceholder placeholder;

    @Override
    public void onEnable() {
        // we will register placeholder on first server tick
        // to get the providers right
        Bukkit.getScheduler().runTask(this, () -> {
            if (super.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                this.placeholder = new VaultPlaceholder();
                this.placeholder.register();
            }
        });
    }

    @Override
    public void onDisable() {
        // check for null, bc the plugin can be disabled faster
        // than the registration task will run
        if (this.placeholder != null) {
            // runtime reload?
            try {
                this.placeholder.unregister();
            } catch (Throwable ignored) {
                // ignore if PlaceholderAPI is already disabled
            }
        }
    }
}
