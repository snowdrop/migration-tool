package org.openrewrite.quarkus.maven.update;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@JacksonXmlRootElement(localName = "settings")
public class MavenSettings {

	@With
	public Servers servers;

	@JsonCreator
	public MavenSettings(@JsonProperty("servers") Servers servers) {
		this.servers = servers;
	}
}
