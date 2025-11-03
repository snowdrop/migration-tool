package dev.snowdrop.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class RecipeDTOSerializer extends StdSerializer<RecipeDTO> {

	public RecipeDTOSerializer() {
		this(null);
	}

	public RecipeDTOSerializer(Class<RecipeDTO> t) {
		super(t);
	}

	@Override
	public void serialize(RecipeDTO dto, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeStartObject();
		gen.writeFieldName(dto.name());

		gen.writeStartObject();
		for (Parameter p : dto.parameters()) {
			gen.writeStringField(p.parameter(), p.value());
		}

		gen.writeEndObject();
		gen.writeEndObject();
	}
}