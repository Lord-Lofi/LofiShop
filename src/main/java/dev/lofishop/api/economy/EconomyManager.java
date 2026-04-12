package dev.lofishop.api.economy;

import dev.lofishop.LofiShop;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages all registered economy providers and resolves them by ID.
 */
public class EconomyManager {

    private final LofiShop plugin;
    private final Map<String, EconomyProvider> providers = new LinkedHashMap<>();
    private EconomyProvider defaultProvider;

    public EconomyManager(LofiShop plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all supported providers and selects the default.
     * Returns false if no provider is available.
     */
    public boolean setup() {
        register(new VaultEconomyProvider(plugin));
        register(new EssentialsEconomyProvider(plugin));

        String preferred = plugin.getConfig().getString("general.economy", "vault");
        defaultProvider = providers.get(preferred);

        if (defaultProvider == null || !defaultProvider.isAvailable()) {
            // Fall back to first available
            for (EconomyProvider p : providers.values()) {
                if (p.isAvailable()) {
                    defaultProvider = p;
                    break;
                }
            }
        }

        if (defaultProvider == null) return false;
        plugin.getLogger().info("Using economy: " + defaultProvider.getId());
        return true;
    }

    private void register(EconomyProvider provider) {
        if (provider.isAvailable()) {
            providers.put(provider.getId(), provider);
            plugin.getLogger().info("Registered economy provider: " + provider.getId());
        }
    }

    /** Returns the provider for the given ID, or the default if not found. */
    public EconomyProvider getProvider(String id) {
        return providers.getOrDefault(id, defaultProvider);
    }

    public EconomyProvider getDefault() {
        return defaultProvider;
    }
}
