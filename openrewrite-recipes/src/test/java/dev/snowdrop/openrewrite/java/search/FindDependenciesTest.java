package dev.snowdrop.openrewrite.java.search;

import dev.snowdrop.openrewrite.maven.search.scanner.FindDependencies;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.maven.Assertions.pomXml;

public class FindDependenciesTest implements RewriteTest {

	@Disabled
	@Test
	void simple() {
		rewriteRun(spec -> spec.recipe(new FindDependencies("match-001", "io.jsonwebtoken:jjwt:0.9.1"))
				.typeValidationOptions(TypeValidation.all().immutableExecutionContext(false)), pomXml("""
						<project>
						  <modelVersion>4.0.0</modelVersion>
						  <groupId>org.sample</groupId>
						  <artifactId>sample</artifactId>
						  <version>1.0.0</version>
						  <dependencies>
						    <dependency>
						      <groupId>io.jsonwebtoken</groupId>
						      <artifactId>jjwt</artifactId>
						      <version>0.9.1</version>
						    </dependency>
						  </dependencies>
						</project>
						""", """
						<project>
						  <modelVersion>4.0.0</modelVersion>
						  <groupId>org.sample</groupId>
						  <artifactId>sample</artifactId>
						  <version>1.0.0</version>
						  <dependencies>
						    <!--~~>--><dependency>
						      <groupId>io.jsonwebtoken</groupId>
						      <artifactId>jjwt</artifactId>
						      <version>0.9.1</version>
						    </dependency>
						  </dependencies>
						</project>
						"""));
	}
}