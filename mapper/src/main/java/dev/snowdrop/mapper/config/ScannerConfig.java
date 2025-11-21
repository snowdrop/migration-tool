package dev.snowdrop.mapper.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration entry for a specific query type and symbol combination.
 * Contains scanner type, DTO class, and description.
 */
public class ScannerConfig {

	@JsonProperty("scanner")
	private String scanner;

	@JsonProperty("dto")
	private String dto;

	@JsonProperty("description")
	private String description;

	// Constructors
	public ScannerConfig() {
	}

	public ScannerConfig(String scanner, String dto, String description) {
		this.scanner = scanner;
		this.dto = dto;
		this.description = description;
	}

	// Getters and setters
	public String getScanner() {
		return scanner;
	}

	public void setScanner(String scanner) {
		this.scanner = scanner;
	}

	public String getDto() {
		return dto;
	}

	public void setDto(String dto) {
		this.dto = dto;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "ScannerConfig{" + "scanner='" + scanner + '\'' + ", dto='" + dto + '\'' + ", description='"
				+ description + '\'' + '}';
	}
}