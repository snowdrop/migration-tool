package org.openrewrite.quarkus.maven.update;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.With;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
@With
public class HttpHeader {
	String name;
	String value;

	@JsonCreator
	public HttpHeader(@JsonProperty("name") String name, @JsonProperty("value") String value) {
		this.name = name;
		this.value = value;
	}
}