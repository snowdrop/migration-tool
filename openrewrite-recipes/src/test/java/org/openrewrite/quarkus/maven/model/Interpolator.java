package org.openrewrite.quarkus.maven.model;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;

import java.util.function.UnaryOperator;

public class Interpolator {
	public static final PropertyPlaceholderHelper propertyPlaceholders = new PropertyPlaceholderHelper("${", "}", null);

	public static final UnaryOperator<String> propertyResolver = key -> {
		String property = System.getProperty(key);
		if (property != null) {
			return property;
		}
		if (key.startsWith("env.")) {
			return System.getenv().get(key.substring(4));
		}
		return System.getenv().get(key);
	};

	public MavenSettings interpolate(MavenSettings mavenSettings) {
		return new MavenSettings(interpolate(mavenSettings.localRepository), mavenSettings.profiles,
				interpolate(mavenSettings.activeProfiles), interpolate(mavenSettings.mirrors),
				interpolate(mavenSettings.servers));
	}

	public MavenSettings.ActiveProfiles interpolate(MavenSettings.ActiveProfiles activeProfiles) {
		if (activeProfiles == null)
			return null;
		return new MavenSettings.ActiveProfiles(ListUtils.map(activeProfiles.getActiveProfiles(), this::interpolate));
	}

	public MavenSettings.Mirrors interpolate(MavenSettings.Mirrors mirrors) {
		if (mirrors == null)
			return null;
		return new MavenSettings.Mirrors(ListUtils.map(mirrors.getMirrors(), this::interpolate));
	}

	public MavenSettings.Mirror interpolate(MavenSettings.Mirror mirror) {
		return new MavenSettings.Mirror(interpolate(mirror.id), interpolate(mirror.url),
				interpolate(mirror.getMirrorOf()), mirror.releases, mirror.snapshots);
	}

	public MavenSettings.Servers interpolate(MavenSettings.Servers servers) {
		if (servers == null)
			return null;
		return new MavenSettings.Servers(ListUtils.map(servers.getServers(), this::interpolate));
	}

	public MavenSettings.ServerConfiguration interpolate(MavenSettings.ServerConfiguration configuration) {
		if (configuration == null) {
			return null;
		}
		return new MavenSettings.ServerConfiguration(ListUtils.map(configuration.httpHeaders, this::interpolate),
				configuration.timeout);
	}

	public MavenSettings.HttpHeader interpolate(MavenSettings.HttpHeader httpHeader) {
		return new MavenSettings.HttpHeader(interpolate(httpHeader.getName()), interpolate(httpHeader.getValue()),
				interpolate(httpHeader.getProperty()));
	}

	public MavenSettings.Server interpolate(MavenSettings.Server server) {
		return new MavenSettings.Server(interpolate(server.id), interpolate(server.username),
				interpolate(server.password), interpolate(server.configuration));
	}

	public @Nullable String interpolate(@Nullable String s) {
		return s == null ? null : propertyPlaceholders.replacePlaceholders(s, propertyResolver);
	}
}