package dev.snowdrop.openrewrite.recipe.spring;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodReturnType extends Recipe {

	@Option(displayName = "Method pattern", description = MethodMatcher.METHOD_PATTERN_DESCRIPTION, example = "org.mockito.Matchers anyVararg()")
	String methodPattern;

	@Option(displayName = "New method invocation return type", description = "The fully qualified new return type of method invocation.", example = "long")
	String newReturnType;

	@Override
	public String getDisplayName() {
		return "Change method invocation return type";
	}

	@Override
	public String getDescription() {
		return "Changes the return type of a method invocation.";
	}

	@Override
	public Validated<Object> validate() {
		return super.validate().and(MethodMatcher.validate(methodPattern));
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {

		return new JavaIsoVisitor<ExecutionContext>() {
			private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

			@Override
			public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
				J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

				if (methodMatcher.matches(m.getMethodType())) {
					m = m.withReturnTypeExpression(
							TypeTree.build(extractTypeName(newReturnType, TypeFormat.CLASS_NAME_WITH_GENERICS)));
					doAfterVisit(new AddImport<>(extractTypeName(newReturnType, TypeFormat.QUALIFIED_WITHOUT_GENERICS),
							null, false));
					return autoFormat(m, ctx);
					/*
					System.out.println("========== AFTER ==========");
					System.out.printf("Method name: %s \n", m.getSimpleName());
					System.out.printf("Method modifiers: %s \n", m.getModifiers());
					System.out.printf("Return Type: %s \n", m.getType());
					System.out.printf("Return Type expression: %s \n", m.getReturnTypeExpression());
					*/
				}
				return m;
			}
		};
	}

	/**
	 * Extracts from the return type expressed as FQN, the class name (e.g., List), import name (e.g., java.util.List)
	 * or simply the Class name with the generic (e.g., List<String></String>)
	 *
	 *
	 * @param fqn The fully qualified name (e.g., "java.util.List<String>").
	 * @return The type name, including generic parameters or just the class name or import name.
	 */
	public static String extractTypeName(String fqn, TypeFormat format) {
		if (fqn == null || fqn.isEmpty()) {
			return "";
		}

		if (format == TypeFormat.QUALIFIED_WITH_GENERICS) {
			return fqn;
		}

		// 1. Handle Generics: Find the position of '<' (start of generic parameters)
		int genericIndex = fqn.indexOf('<');
		String baseName = fqn;
		String generics = "";

		if (genericIndex != -1) {
			generics = fqn.substring(genericIndex); // Captures <String>, <T>, etc.
			baseName = fqn.substring(0, genericIndex); // Captures only "java.util.List"
		}

		// 2. Find the position of the last dot (package separator)
		int lastDotIndex = baseName.lastIndexOf('.');

		String simpleName;

		if (lastDotIndex != -1) {
			// Extract the name after the last dot
			simpleName = baseName.substring(lastDotIndex + 1);
		} else {
			// No package separator found (already a simple name)
			simpleName = baseName;
		}

		// 3. Return the result based on requirements
		return switch (format) {
			case SIMPLE -> simpleName; // Return "List" from java.util.List<String>
			case CLASS_NAME_WITH_GENERICS -> simpleName + generics; // Return "List" + "<String>"
			case QUALIFIED_WITHOUT_GENERICS -> baseName; // Return java.util.List;
			case QUALIFIED_WITH_GENERICS -> fqn;
		};
	}

	public enum TypeFormat {
		// 1. Return the simple name of the Class (ex: List)
		SIMPLE,
		// 2. Return the name of the class and its generic (ex: List<String>)
		CLASS_NAME_WITH_GENERICS,
		// 3. Return the FQName but without the generic (ex: java.util.List)
		QUALIFIED_WITHOUT_GENERICS,
		// 4. Return the FQName with the generic (ex: java.util.List)
		QUALIFIED_WITH_GENERICS
	}
}
