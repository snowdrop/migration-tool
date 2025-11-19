package dev.snowdrop.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for file content analysis results from file scanner.
 * Contains information about file contents and their characteristics.
 */
public record FileContentDTO(String filePath, String fileName, String fileExtension, String mimeType, long fileSize,
		String encoding, String content, String contentHash, LocalDateTime lastModified, List<String> matchingPatterns,
		Map<String, Object> metadata, boolean isBinary, int lineCount) {

	/**
	 * Creates a new FileContentDTO with different content.
	 */
	public FileContentDTO withContent(String newContent) {
		return new FileContentDTO(filePath, fileName, fileExtension, mimeType, newContent.length(), encoding,
				newContent, generateContentHash(newContent), lastModified, matchingPatterns, metadata, isBinary,
				newContent.split("\n").length);
	}

	/**
	 * Creates a new FileContentDTO with additional matching patterns.
	 */
	public FileContentDTO withAdditionalPatterns(List<String> additionalPatterns) {
		List<String> updatedPatterns = new java.util.ArrayList<>(matchingPatterns);
		updatedPatterns.addAll(additionalPatterns);
		return new FileContentDTO(filePath, fileName, fileExtension, mimeType, fileSize, encoding, content, contentHash,
				lastModified, updatedPatterns, metadata, isBinary, lineCount);
	}

	/**
	 * Creates a new FileContentDTO with additional metadata.
	 */
	public FileContentDTO withAdditionalMetadata(Map<String, Object> additionalMetadata) {
		Map<String, Object> updatedMetadata = new java.util.HashMap<>(metadata);
		updatedMetadata.putAll(additionalMetadata);
		return new FileContentDTO(filePath, fileName, fileExtension, mimeType, fileSize, encoding, content, contentHash,
				lastModified, matchingPatterns, updatedMetadata, isBinary, lineCount);
	}

	/**
	 * Returns whether the file is a text file.
	 */
	public boolean isTextFile() {
		return !isBinary;
	}

	/**
	 * Returns whether the file is empty.
	 */
	public boolean isEmpty() {
		return content == null || content.trim().isEmpty();
	}

	/**
	 * Returns the file size in KB.
	 */
	public double getFileSizeInKB() {
		return fileSize / 1024.0;
	}

	/**
	 * Simple hash generation for content (placeholder implementation).
	 */
	private static String generateContentHash(String content) {
		return content != null ? String.valueOf(content.hashCode()) : "0";
	}
}