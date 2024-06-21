package org.philzen.oss;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeTypeProblemTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeTypeProblem());
    }

    @Test void classLevelAnnotationWillBeRetained_whenAttributesCannotBeMigrated() {
        // language=java
        rewriteRun(java(
          """
            import org.testng.annotations.Test;
            
            @Test(threadPoolSize = 8)
            class Baz {
            }
            """,
          """
            import org.junit.jupiter.api.Test;
            
            /* ❗️ ❗️ ❗️
               At least one `@Test`-attribute could not be migrated to JUnit 5. Kindly review the remainder below
               and manually apply any changes you may require to retain the existing test suite's behavior. Delete
            ↓  the annotation and this comment when satisfied, or use `git reset --hard` to roll back the migration.
            
               If you think this is a mistake or have an idea how this migration could be implemented instead, any
               feedback to https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues will be greatly appreciated.
            */
            @org.testng.annotations.Test(threadPoolSize = 8)
            class Baz {
            }
            """
        ));
    }
}
