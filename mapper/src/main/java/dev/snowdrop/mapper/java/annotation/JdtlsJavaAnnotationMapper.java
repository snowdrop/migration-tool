package dev.snowdrop.mapper.java.annotation;

import dev.snowdrop.mapper.QueryMapper;
import dev.snowdrop.model.JavaAnnotationDTO;
import dev.snowdrop.model.Query;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * JDTLS scanner-specific mapper for JavaAnnotation queries.
 * Maps java.annotation queries to JDTLS-compatible DTO format.
 * Returns JavaAnnotationDTO objects suitable for JDTLS scanner processing.
 */
public class JdtlsJavaAnnotationMapper implements QueryMapper<Object> {

	private static final Logger logger = Logger.getLogger(JdtlsJavaAnnotationMapper.class);

	@Override
	public JavaAnnotationDTO map(Query query) {
		logger.debugf("Creating JavaAnnotationDTO for JDTLS scanner: %s.%s", query.fileType(), query.symbol());

		// Extract annotation name from query key-values
		String annotationName = query.keyValues().get("name");
		if (annotationName == null) {
			throw new IllegalArgumentException("Missing 'name' parameter for JDTLS java.annotation query");
		}

		// Create JDTLS-compatible DTO
		// Note: Some fields may be null initially and will be populated by the scanner
		return new JavaAnnotationDTO(annotationName, // annotationName
				null, // fullyQualifiedName (to be populated by scanner)
				null, // sourceFile (to be populated by scanner)
				0, // lineNumber (to be populated by scanner)
				null, // targetElement (to be populated by scanner)
				Map.copyOf(query.keyValues()), // attributes (copy query parameters)
				List.of() // values (to be populated by scanner)
		);
	}

	@Override
	public String getSupportedDtoClass() {
		return "dev.snowdrop.model.JavaAnnotationDTO";
	}
}