package dev.snowdrop.mtool.validate;

import dev.snowdrop.mtool.model.validate.ValidationReport;

import java.nio.file.Path;

public interface Validator {

    String name();

    ValidationReport validate(Path projectPath);
}