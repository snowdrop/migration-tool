package dev.snowdrop.mapper.file;

import dev.snowdrop.mapper.QueryMapper;
import dev.snowdrop.model.FileContentDTO;
import dev.snowdrop.model.Query;
import org.jboss.logging.Logger;

/**
 * Mapper for FileContentDTO-specific queries.
 * This mapper is optimized for file content analysis and creates RecipeDTO objects
 * specifically tailored for searching within file contents.
 */
public class FileContentMapper implements QueryMapper<FileContentDTO> {

	private static final Logger logger = Logger.getLogger(FileContentMapper.class);

	@Override
	public FileContentDTO map(Query query) {
		logger.debugf("Mapping query %s.%s for FileContentDTO", query.fileType(), query.symbol());
		return null;
	}

	@Override
	public String getSupportedDtoClass() {
		return "dev.snowdrop.model.FileContentDTO";
	}
}