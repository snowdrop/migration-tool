package org.openrewrite.quarkus.maven.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.maven.internal.MavenXmlMapper;
import org.openrewrite.maven.internal.RawRepositories;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ProfileActivation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_LOCAL_DEFAULT;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@AllArgsConstructor
@JacksonXmlRootElement(localName = "settings")
public class MavenSettings {
    @Nullable
    public String localRepository;

    @Nullable
    @NonFinal
    @JsonIgnore
    public MavenRepository mavenLocal;

    public MavenSettings.Profiles profiles;

    public MavenSettings.ActiveProfiles activeProfiles;

    public MavenSettings.Mirrors mirrors;

    @With
    public MavenSettings.Servers servers;

    @JsonCreator
    public MavenSettings(@Nullable String localRepository, MavenSettings.Profiles profiles,
            MavenSettings.ActiveProfiles activeProfiles, MavenSettings.Mirrors mirrors, MavenSettings.Servers servers) {
        this.localRepository = localRepository;
        this.profiles = profiles;
        this.activeProfiles = activeProfiles;
        this.mirrors = mirrors;
        this.servers = servers;
    }

    public static MavenSettings parse(Parser.Input source, ExecutionContext ctx) {
        try {
            MavenSettings settings = new MavenSettings.Interpolator()
                    .interpolate(MavenXmlMapper.readMapper().readValue(source.getSource(ctx), MavenSettings.class));
            settings.maybeDecryptPasswords(ctx);
            return settings;
        } catch (IOException e) {
            ctx.getOnError().accept(new IOException("Failed to parse " + source.getPath(), e));
            return null;
        }
    }

    public static @Nullable MavenSettings parse(Path settingsPath, ExecutionContext ctx) {
        MavenSettings settings = parse(new Parser.Input(settingsPath, () -> {
            try {
                return Files.newInputStream(settingsPath);
            } catch (IOException e) {
                ctx.getOnError().accept(new IOException("Failed to read settings.xml at " + settingsPath, e));
                return null;
            }
        }), ctx);

        if (settings != null) {
            settings.maybeDecryptPasswords(ctx);
        }

        return settings;
    }

    public static @Nullable MavenSettings readMavenSettingsFromDisk(ExecutionContext ctx) {
        final Optional<MavenSettings> userSettings = Optional.of(userSettingsPath()).filter(MavenSettings::exists)
                .map(path -> parse(path, ctx));
        final MavenSettings installSettings = findMavenHomeSettings().map(path -> parse(path, ctx)).orElse(null);
        MavenSettings settings = userSettings.map(mavenSettings -> mavenSettings.merge(installSettings))
                .orElse(installSettings);

        if (settings != null) {
            settings.maybeDecryptPasswords(ctx);
        }

        return settings;
    }

    void maybeDecryptPasswords(ExecutionContext ctx) {
        MavenSecuritySettings security = MavenSecuritySettings.readMavenSecuritySettingsFromDisk(ctx);
        if (security == null) {
            return;
        }

        String decryptedMasterPassword = security.decrypt(security.getMaster(), "settings.security");
        if (decryptedMasterPassword != null) {
            if (mavenLocal != null) {
                String password = security.decrypt(mavenLocal.getPassword(), decryptedMasterPassword);
                if (password != null) {
                    mavenLocal = mavenLocal.withPassword(password);
                }
            }
            if (servers != null) {
                servers.servers = ListUtils.map(servers.servers, server -> {
                    String password = security.decrypt(server.getPassword(), decryptedMasterPassword);
                    return password == null ? server : server.withPassword(password);
                });
            }
        }
    }

    /**
     * @return True property `org.openrewrite.test.readMavenSettingsFromDisk` is true, false otherwise.
     *
     * @deprecated The concept of property `org.openrewrite.test.readMavenSettingsFromDisk` is no longer in use.
     */
    @Deprecated
    public static boolean readFromDiskEnabled() {
        final String propertyValue = System.getProperty("org.openrewrite.test.readMavenSettingsFromDisk");
        return propertyValue != null && !"false".equalsIgnoreCase(propertyValue);
    }

    private static Path userSettingsPath() {
        return Paths.get(System.getProperty("user.home")).resolve(".m2/settings.xml");
    }

    private static Optional<Path> findMavenHomeSettings() {
        for (String envVariable : Arrays.asList("MVN_HOME", "M2_HOME", "MAVEN_HOME")) {
            for (String s : Optional.ofNullable(System.getenv(envVariable)).map(Arrays::asList).orElse(emptyList())) {
                Path resolve = Paths.get(s).resolve("conf/settings.xml");
                if (exists(resolve)) {
                    return Optional.of(resolve);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean exists(Path path) {
        try {
            return path.toFile().exists();
        } catch (SecurityException e) {
            return false;
        }
    }

    public MavenSettings merge(@Nullable MavenSettings installSettings) {
        return installSettings == null ? this
                : new MavenSettings(localRepository == null ? installSettings.localRepository : localRepository,
                        profiles == null ? installSettings.profiles : profiles.merge(installSettings.profiles),
                        activeProfiles == null ? installSettings.activeProfiles
                                : activeProfiles.merge(installSettings.activeProfiles),
                        mirrors == null ? installSettings.mirrors : mirrors.merge(installSettings.mirrors),
                        servers == null ? installSettings.servers : servers.merge(installSettings.servers));
    }

    public List<RawRepositories.Repository> getActiveRepositories(Iterable<String> activeProfiles) {
        LinkedHashMap<String, RawRepositories.Repository> activeRepositories = new LinkedHashMap<>();

        if (profiles != null) {
            for (MavenSettings.Profile profile : profiles.getProfiles()) {
                if (profile.isActive(activeProfiles)
                        || (this.activeProfiles != null && profile.isActive(this.activeProfiles.getActiveProfiles()))) {
                    if (profile.repositories != null) {
                        for (RawRepositories.Repository repository : profile.repositories.getRepositories()) {
                            activeRepositories.put(repository.getId(), repository);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(activeRepositories.values());
    }

    public MavenRepository getMavenLocal() {
        if (localRepository == null) {
            return MAVEN_LOCAL_DEFAULT;
        }
        if (mavenLocal == null) {
            mavenLocal = MavenRepository.builder().id("local").uri(asUriString(localRepository)).knownToExist(true)
                    .build();
        }
        return mavenLocal;
    }

    private static String asUriString(final String pathname) {
        return pathname.startsWith("file://") ? pathname : Paths.get(pathname).toUri().toString();
    }

    /**
     * Resolve all properties EXCEPT in the profiles section, which can be affected by the POM using the settings.
     */
    private static class Interpolator {
        private static final PropertyPlaceholderHelper propertyPlaceholders = new PropertyPlaceholderHelper("${", "}",
                null);

        private static final UnaryOperator<String> propertyResolver = key -> {
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

        private MavenSettings.ActiveProfiles interpolate(MavenSettings.ActiveProfiles activeProfiles) {
            if (activeProfiles == null)
                return null;
            return new MavenSettings.ActiveProfiles(
                    ListUtils.map(activeProfiles.getActiveProfiles(), this::interpolate));
        }

        private MavenSettings.Mirrors interpolate(MavenSettings.Mirrors mirrors) {
            if (mirrors == null)
                return null;
            return new MavenSettings.Mirrors(ListUtils.map(mirrors.getMirrors(), this::interpolate));
        }

        private MavenSettings.Mirror interpolate(MavenSettings.Mirror mirror) {
            return new MavenSettings.Mirror(interpolate(mirror.id), interpolate(mirror.url),
                    interpolate(mirror.getMirrorOf()), mirror.releases, mirror.snapshots);
        }

        private MavenSettings.Servers interpolate(MavenSettings.Servers servers) {
            if (servers == null)
                return null;
            return new MavenSettings.Servers(ListUtils.map(servers.getServers(), this::interpolate));
        }

        private MavenSettings.ServerConfiguration interpolate(MavenSettings.ServerConfiguration configuration) {
            if (configuration == null) {
                return null;
            }
            return new MavenSettings.ServerConfiguration(ListUtils.map(configuration.httpHeaders, this::interpolate),
                    configuration.timeout);
        }

        private MavenSettings.HttpHeader interpolate(MavenSettings.HttpHeader httpHeader) {
            return new MavenSettings.HttpHeader(interpolate(httpHeader.getName()), interpolate(httpHeader.getValue()),
                    interpolate(httpHeader.getProperty()));
        }

        private MavenSettings.Server interpolate(MavenSettings.Server server) {
            return new MavenSettings.Server(interpolate(server.id), interpolate(server.username),
                    interpolate(server.password), interpolate(server.configuration));
        }

        private @Nullable String interpolate(@Nullable String s) {
            return s == null ? null : propertyPlaceholders.replacePlaceholders(s, propertyResolver);
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Profiles {
        @JacksonXmlProperty(localName = "profile")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MavenSettings.Profile> profiles = emptyList();

        public MavenSettings.Profiles merge(MavenSettings.Profiles profiles) {
            final Map<String, MavenSettings.Profile> merged = new LinkedHashMap<>();
            for (MavenSettings.Profile profile : this.profiles) {
                merged.put(profile.id, profile);
            }
            if (profiles != null) {
                profiles.getProfiles().forEach(profile -> merged.putIfAbsent(profile.getId(), profile));
            }
            return new MavenSettings.Profiles(new ArrayList<>(merged.values()));
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActiveProfiles {
        @JacksonXmlProperty(localName = "activeProfile")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<String> activeProfiles = emptyList();

        public MavenSettings.ActiveProfiles merge(MavenSettings.ActiveProfiles activeProfiles) {
            if (activeProfiles == null) {
                return new MavenSettings.ActiveProfiles(new ArrayList<>(this.activeProfiles));
            }
            List<String> result = new ArrayList<>();
            Set<String> uniqueValues = new HashSet<>();
            for (String s : ListUtils.concatAll(this.activeProfiles, activeProfiles.activeProfiles)) {
                if (uniqueValues.add(s)) {
                    result.add(s);
                }
            }
            return new MavenSettings.ActiveProfiles(result);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Profile {
        @Nullable
        String id;

        @Nullable
        ProfileActivation activation;

        @Nullable
        RawRepositories repositories;

        public boolean isActive(Iterable<String> activeProfiles) {
            return ProfileActivation.isActive(id, activeProfiles, activation);
        }

        @SuppressWarnings("unused")
        public boolean isActive(String... activeProfiles) {
            return isActive(Arrays.asList(activeProfiles));
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Mirrors {
        @JacksonXmlProperty(localName = "mirror")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MavenSettings.Mirror> mirrors = emptyList();

        public MavenSettings.Mirrors merge(MavenSettings.Mirrors mirrors) {
            final Map<String, MavenSettings.Mirror> merged = new LinkedHashMap<>();
            for (MavenSettings.Mirror mirror : this.mirrors) {
                merged.put(mirror.id, mirror);
            }
            if (mirrors != null) {
                mirrors.getMirrors().forEach(mirror -> merged.putIfAbsent(mirror.getId(), mirror));
            }
            return new MavenSettings.Mirrors(new ArrayList<>(merged.values()));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Mirror {
        @Nullable
        public String id;

        @Nullable
        public String url;

        @Nullable
        public String mirrorOf;

        @Nullable
        public Boolean releases;

        @Nullable
        public Boolean snapshots;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Servers {
        @JacksonXmlProperty(localName = "server")
        @JacksonXmlElementWrapper(useWrapping = false)
        @With
        List<Server> servers = emptyList();

        public Servers merge(Servers servers) {
            final Map<String, Server> merged = new LinkedHashMap<>();
            for (Server server : this.servers) {
                merged.put(server.id, server);
            }
            if (servers != null) {
                servers.getServers().forEach(server -> merged.putIfAbsent(server.getId(), server));
            }
            return new Servers(new ArrayList<>(merged.values()));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class Server {
        public String id;

        public String username;
        public String password;

        public MavenSettings.ServerConfiguration configuration;
    }

    @SuppressWarnings("DefaultAnnotationParam")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    @JsonIgnoreProperties("httpHeaders")
    public static class ServerConfiguration {
        // @JacksonXmlProperty(localName = "property")
        // @JacksonXmlElementWrapper(localName = "httpHeaders", useWrapping = true)
        // wrapping is disabled by default on MavenXmlMapper

        @JacksonXmlProperty(localName = "property")
        @JacksonXmlElementWrapper(localName = "httpHeaders")
        @Nullable
        public List<MavenSettings.HttpHeader> httpHeaders;

        /**
         * Timeout in milliseconds for reading connecting to and reading from the connection.
         */
        @Nullable
        public Long timeout;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class HttpHeader {
        String name;
        String value;
        String property;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class Property {
        String name;
        String value;
    }

}