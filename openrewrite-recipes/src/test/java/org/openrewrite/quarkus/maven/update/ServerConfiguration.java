package org.openrewrite.quarkus.maven.update;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

import java.util.List;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
@With
public class ServerConfiguration {
    @JacksonXmlProperty(localName = "property")
    @JacksonXmlElementWrapper(localName = "httpHeaders")
    @Nullable
    public List<HttpHeader> httpHeaders;

    /**
     * Timeout in milliseconds for reading connecting to and reading from the connection.
     */
    @Nullable
    public Long timeout;

    @JsonCreator
    public ServerConfiguration(List<HttpHeader> httpHeaders, @JsonProperty("timeout") @Nullable Long timeout) {
        this.httpHeaders = httpHeaders;
        this.timeout = timeout;
    }
}