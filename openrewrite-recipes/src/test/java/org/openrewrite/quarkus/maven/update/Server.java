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
public class Server {
	public String id;
	public String username;
	public String password;
	public ServerConfiguration configuration;

	@JsonCreator
	public Server(@JsonProperty("id") String id, @JsonProperty("username") String username,
			@JsonProperty("password") String password,
			@JsonProperty("configuration") ServerConfiguration configuration) {
		this.id = id;
		this.username = username;
		this.password = password;
		this.configuration = configuration;
	}
}