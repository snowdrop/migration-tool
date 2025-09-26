package dev.snowdrop.openrewrite;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.eclipse.lsp4j.SymbolKind;

import java.io.IOException;

public class SymbolKindDeserializer extends JsonDeserializer<SymbolKind> {
    @Override
    public SymbolKind deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        int kindValue = parser.getIntValue();
        return SymbolKind.forValue(kindValue);
    }
}
