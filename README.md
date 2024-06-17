# TestNG to Junit5 recipe   [![Compatible with Java 8](https://img.shields.io/badge/Works%20on%20Java-8-seagreen?logo=openjdk&labelColor=snow&logoColor=black)](#)

[![Build](https://github.com/Philzen/rewrite-recipe-testng-to-junit-jupiter/actions/workflows/ci.yml/badge.svg)](https://github.com/Philzen/rewrite-recipe-testng-to-junit-jupiter/actions/workflows/ci.yml)

Converts [TestNG](https://testng.org/) test annotations and assertions to
[Junit 5](https://junit.org/junit5/docs/current/user-guide/).

Inspired by the [Migrate JUnit 4 @Test annotations to JUnit 5](https://docs.openrewrite.org/recipes/java/testing/junit5/updatetestannotation) recipe

## Usage

### Prerequisites

- cloned the project to your local machine 
- if you're an SDKMAN!-Andy, simply run `sdk env` from the project directory   
  otherwise ensure:  
   - JDK 17+
   - Maven 3.9.x *or* Gradle 8.x  

   are provided on your system.

### Build & install this recipe to your local repository

From the project directory, run one of the following: 

<details><summary>Maven</summary>
<p>

```bash
mvn install -DskipTests
```
</p>
</details> 

<details><summary>Gradle</summary>
<p>

```bash
./gradlew publishToMavenLocal
# or ./gradlew pTML
# or mvn install
```
</p>
</details> 

This will publish to your local maven repository, typically under `~/.m2/repository`.

### Migrate a project

<details><summary>Maven</summary>
<p>

In the `pom.xml` of a different project you wish to run the recipe on, 
make it a plugin dependency of rewrite-maven-plugin:

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>org.openrewrite.maven</groupId>
        <artifactId>rewrite-maven-plugin</artifactId>
        <version>5.33.0</version>
        <configuration>
          <activeRecipes>
            <recipe>org.philzen.oss.testng.MigrateToJunit5</recipe>
          </activeRecipes>
        </configuration>
        <dependencies>
          <dependency>
            <groupId>org.philzen.oss</groupId>
            <artifactId>rewrite-testng-to-junit5</artifactId>
            <version>1.0.1-SNAPSHOT</version>
          </dependency>
        </dependencies>
      </plugin>
    </plugins>
  </build>
</project>
```
Now run the recipe via `mvn rewrite:run`.
</details> 

<details><summary>Gradle</summary>
<p>

Unlike Maven, Gradle must be explicitly configured to resolve dependencies from Maven local.
In the root project of a gradle build that you wish to run this recipe on,
make it a dependency of the `rewrite` configuration:

```groovy
plugins {
    id("java")
    id("org.openrewrite.rewrite") version("latest.release")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    rewrite("com.yourorg:rewrite-recipe-starter:latest.integration")
}

rewrite {
    activeRecipe("com.yourorg.NoGuavaListsNewArrayList")
}
```

Now run the recipe via `gradlew rewriteRun`.
</details>
