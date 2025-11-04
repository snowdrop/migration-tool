import dev.snowdrop.openrewrite.maven.search.FindDependency;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class FindDependencyTest implements RewriteTest {

	@Test
	void simple() {
		rewriteRun(spec -> spec.recipe(new FindDependency("match-001", "io.jsonwebtoken:jjwt:0.9.1")), pomXml("""
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