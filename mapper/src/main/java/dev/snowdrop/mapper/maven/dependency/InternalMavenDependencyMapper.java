package dev.snowdrop.mapper.maven.dependency;

import dev.snowdrop.mapper.QueryMapper;
import dev.snowdrop.model.MavenDependencyDTO;
import dev.snowdrop.model.Query;
import org.jboss.logging.Logger;

public class InternalMavenDependencyMapper implements QueryMapper<Object> {
	private static final Logger logger = Logger.getLogger(InternalMavenDependencyMapper.class);

	@Override
	public Object map(Query q) {
		String[] gavs = q.keyValues().get("gavs").split(",");
		String groupId;
		String artifactId;
		String version;

		// TODO: Support to handle several gav to search about !
		if (gavs.length == 1) {
			String[] gav = gavs[0].split(":");
			groupId = gav[0];
			artifactId = gav[1];
			version = (gav.length > 2 && gav[2] != null) ? gav[2] : "";

			return new MavenDependencyDTO(groupId, artifactId, version, null);
		} else {
			return new MavenDependencyDTO("", "", "", null);
		}
	}

	@Override
	public String getSupportedDtoClass() {
		return "";
	}
}
