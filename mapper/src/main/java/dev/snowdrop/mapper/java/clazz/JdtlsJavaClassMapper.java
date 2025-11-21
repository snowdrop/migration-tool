package dev.snowdrop.mapper.java.clazz;

import dev.snowdrop.mapper.QueryMapper;
import dev.snowdrop.model.JavaClassDTO;
import dev.snowdrop.model.Query;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

import static dev.snowdrop.mapper.JdtLsUtils.getLocationCode;

/**
 * JDTLS scanner-specific mapper for JavaClass queries.
 * Maps java.annotation queries to JDTLS-compatible DTO format.
 * Returns JavaClassDTO objects suitable for JDTLS scanner processing.
 */
public class JdtlsJavaClassMapper implements QueryMapper<Object> {

	private static final Logger logger = Logger.getLogger(JdtlsJavaClassMapper.class);

	@Override
	public JavaClassDTO map(Query q) {
		logger.debugf("Creating JavaClassDTO for JDTLS scanner: %s.%s", q.fileType(), q.symbol());

		String location = getLocationCode(q.symbol());
		if (location == null || location.equals("0")) {
			throw new IllegalStateException(String.format(
					"The language server's location code don't exist using the when condition of the query: %s-%s",
					q.fileType(), q.symbol()));
		}

		/*
		   Map the Query object with the RuleEntry parameters to be sent to the Language Server
		   where the parameters are defined as such:
		   - project: java
		   - location matches one of the values that the Language Server supports: annotation, etc
		   - query corresponds to what the user passed as parameter from the query
		   - analysisMode: source-only # 2 modes are supported: source-only and full
		 */
		var cmdParams = Map.of("project", q.fileType().toLowerCase(), "location", location, "query",
				q.keyValues().get("name"), // TODO: To be improved as we need a mapper able to also extract k=v when defined
				"analysisMode", "source-only");

		return new JavaClassDTO(cmdParams);
	}

	@Override
	public String getSupportedDtoClass() {
		return "dev.snowdrop.model.JavaClassDTO";
	}
}