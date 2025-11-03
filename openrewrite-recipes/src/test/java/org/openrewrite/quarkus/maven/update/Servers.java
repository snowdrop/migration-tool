package org.openrewrite.quarkus.maven.update;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Servers {
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