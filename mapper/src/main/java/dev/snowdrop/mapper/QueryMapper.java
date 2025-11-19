package dev.snowdrop.mapper;

import dev.snowdrop.model.Query;

/**
 * Interface for mapping Query objects to RecipeDTO objects.
 * Different implementations can provide specific mapping logic based on DTO types.
 */
public interface QueryMapper<T> {

	/**
	 * Maps a Query to a RecipeDTO.
	 *
	 * @param query The query to map
	 * @return RecipeDTO object appropriate for the specific mapper implementation
	 */
	T map(Query query);

	/**
	 * Returns the DTO class that this mapper is designed for.
	 *
	 * @return The fully qualified class name of the supported DTO
	 */
	String getSupportedDtoClass();
}