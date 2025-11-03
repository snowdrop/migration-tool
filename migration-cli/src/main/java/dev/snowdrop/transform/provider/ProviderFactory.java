package dev.snowdrop.transform.provider;

import dev.snowdrop.transform.provider.impl.AiProvider;
import dev.snowdrop.transform.provider.impl.ManualProvider;
import dev.snowdrop.transform.provider.impl.OpenRewriteProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for creating and managing migration providers.
 */
public class ProviderFactory {

	private static final Map<String, MigrationProvider> providers = new HashMap<>();

	static {
		registerProvider(new OpenRewriteProvider());
		registerProvider(new AiProvider());
		registerProvider(new ManualProvider());
	}

	/**
	 * Registers a provider with the factory.
	 *
	 * @param provider
	 *            the provider to register
	 */
	public static void registerProvider(MigrationProvider provider) {
		providers.put(provider.getProviderType(), provider);
	}

	/**
	 * Gets a provider by type.
	 *
	 * @param providerType
	 *            the provider type (ai, openrewrite, manual)
	 *
	 * @return the provider if found, empty otherwise
	 */
	public static Optional<MigrationProvider> getProvider(String providerType) {
		return Optional.ofNullable(providers.get(providerType));
	}
}