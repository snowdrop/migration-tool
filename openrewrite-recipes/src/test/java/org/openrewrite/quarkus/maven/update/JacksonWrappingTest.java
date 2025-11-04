package org.openrewrite.quarkus.maven.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JacksonWrappingTest {

	@Disabled
	@Test
	public void toJson() throws JsonProcessingException {
		String xmlsettings = """
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
				</settings>
				""";

		XmlMapper xmlMapper = new XmlMapper();
		MavenSettings settings = xmlMapper.readValue(xmlsettings, MavenSettings.class);
		assertTrue(!settings.getServers().getServers().isEmpty());
	}

}
