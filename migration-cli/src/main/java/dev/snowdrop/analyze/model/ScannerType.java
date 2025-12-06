package dev.snowdrop.analyze.model;

public enum ScannerType {
	OPENREWRITE("openrewrite"), JDTLS("jdtls"), MAVEN("maven"), FILE_SEARCH("file-search");

	private final String label;

	ScannerType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static ScannerType fromLabel(String label) {
		for (ScannerType s : values()) {
			if (s.label.equalsIgnoreCase(label)) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unknown scanner: " + label);
	}
}
