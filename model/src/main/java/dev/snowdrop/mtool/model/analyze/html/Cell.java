package dev.snowdrop.mtool.model.analyze.html;

public record Cell(String text, String url) {
	public Cell(String text) {
		this(text, null);
	}

	public String getText() {
		return text;
	}

	public String getUrl() {
		return url;
	}
}