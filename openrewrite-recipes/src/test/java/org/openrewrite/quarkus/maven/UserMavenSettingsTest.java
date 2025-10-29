package org.openrewrite.quarkus.maven;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.internal.MavenXmlMapper;
import org.openrewrite.quarkus.maven.model.Interpolator;
import org.openrewrite.quarkus.maven.model.MavenSettings;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/*
   Version of jackson reporting the issue:
     [INFO] +- com.fasterxml.jackson.core:jackson-core:jar:2.19.2:compile
     [INFO] +- com.fasterxml.jackson.core:jackson-databind:jar:2.19.2:compile

   Version of the rewrite-maven module
     +--- com.fasterxml.jackson.datatype:jackson-datatype-jdk8 -> 2.17.2
     |    +--- com.fasterxml.jackson.core:jackson-core:2.17.2
     |    \--- com.fasterxml.jackson.core:jackson-databind:2.17.2 (*)

 */

public class UserMavenSettingsTest {

    private final MavenExecutionContextView ctx = MavenExecutionContextView
            .view(new InMemoryExecutionContext((ThrowingConsumer<Throwable>) input -> {
                throw input;
            }));

    @Disabled
    @Test
    void serverHttpHeaders() throws IOException {
        var settingsXML = Parser.Input.fromString(Path.of("settings.xml"),
                // language=xml
                """
                        <settings>
                            <servers>
                                <server>
                                    <id>maven-snapshots</id>
                                    <configuration>
                                        <httpHeaders>
                                            <property>
                                                <name>X-JFrog-Art-Api</name>
                                                <value>myApiToken</value>
                                            </property>
                                        </httpHeaders>
                                    </configuration>
                                </server>
                            </servers>
                            <profiles>
                                <profile>
                                    <id>my-profile</id>
                                    <repositories>
                                        <repository>
                                            <id>maven-snapshots</id>
                                            <name>Private Repo</name>
                                            <url>https://repo.company.net/maven</url>
                                        </repository>
                                    </repositories>
                                </profile>
                            </profiles>
                        </settings>
                        """);

        MavenSettings settings = new Interpolator()
                .interpolate(MavenXmlMapper.readMapper().readValue(settingsXML.getSource(ctx), MavenSettings.class));

        MavenSettings.Server server = settings.getServers().getServers().getFirst();
        assertThat(server.getConfiguration().getHttpHeaders().getFirst().getName()).isEqualTo("X-JFrog-Art-Api");
    }
}
