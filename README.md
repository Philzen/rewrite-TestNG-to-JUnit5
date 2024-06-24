# TestNG to Junit5 recipe   [![Compatible with Java 8](https://img.shields.io/badge/Works%20on%20Java-8-seagreen?logo=openjdk&labelColor=snow&logoColor=black)](#)

[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/philzen/rewrite-recipe-testng-to-junit-jupiter/ci.yml?logo=github)](https://github.com/Philzen/rewrite-recipe-testng-to-junit-jupiter/actions/workflows/ci.yml)   
[![Sonar Coverage](https://img.shields.io/sonar/coverage/Philzen_rewrite-recipe-testng-to-junit-jupiter?server=https%3A%2F%2Fsonarcloud.io&logo=sonarcloud&label=Coverage)](https://sonarcloud.io/summary/new_code?id=Philzen_rewrite-recipe-testng-to-junit-jupiter)

Converts [TestNG](https://testng.org/) test annotations and assertions to
[Junit 5](https://junit.org/junit5/docs/current/user-guide/).

Inspired by the [Migrate JUnit 4 @Test annotations to JUnit 5](https://docs.openrewrite.org/recipes/java/testing/junit5/updatetestannotation) recipe

## Roadmap 

This repository is under heavy development. Refer to the [project kanban board](https://github.com/users/Philzen/projects/2/views/2) 
for ongoing updates on the road to feature completeness. Should you notice a blind spot 
regarding TestNG features that should be included in a project migration, or if you have idea 
for a particular implementation, your contributions are welcome!

See below tables for a brief summary of the implementation progress.

### Annotations

| Annotation                                      |     on<br>Class      |    on<br>Methods     |
|-------------------------------------------------|:--------------------:|:--------------------:|
| `@Test`                                         |  :heavy_check_mark:  |  :heavy_check_mark:  |
| `@Test(description = "%s")`                     |    :hammer: [#23]    |  :heavy_check_mark:  |
| `@Test(enabled = false)`                        |    :hammer: [#39]    |  :heavy_check_mark:  |
| `@Test(expectedExceptions = Exception.class)`   |    :hammer: [#20]    |  :heavy_check_mark:  |
| `@Test(expectedExceptionsMessageRegExp = "%s")` |    :hammer: [#21]    |  :heavy_check_mark:  | 
| `@Test(groups = "%s")`                          |    :hammer: [#27]    |  :heavy_check_mark:  |
| `@Test(timeOut = "%s")`                         |    :hammer: [#25]    |  :heavy_check_mark:  |
| `@DataProvider`                                 |    :hammer: [#6]     |    :hammer: [#38]    |
| `@Ignore`                                       |    :hammer: [#15]    |    :hammer: [#15]    |
| `@Test(enabled = CONSTANT_EXPRESSION)`          |   :thinking: [#35]   |   :thinking: [#35]   |
| `@Factory`                                      |   :thinking: [#8]    |   :thinking: [#8]    |
| `@Test(priority, threadPoolSize)` et al.        | :grey_question: [#5] | :grey_question: [#5] |

  [#3]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/pull/3
  [#5]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/5
  [#6]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/6
  [#7]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/7
  [#8]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/8
  [#10]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/10
  [#11]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/11
  [#12]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/12
  [#13]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/13
  [#14]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/14
  [#15]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/15
  [#20]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/20
  [#21]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/21
  [#23]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/23
  [#25]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/25
  [#27]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/27
  [#29]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/29
  [#30]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/30
  [#32]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/32
  [#35]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/35
  [#38]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/pull/38
  [#39]: https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues/39

### Framework features

| Feature                           |        on<br>Class        |
|-----------------------------------|:-------------------------:|
| Tests in inner classes            | :heavy_check_mark: [#30]  |
| Assertions                        |       :hammer: [#3]       |      
| Lifecycle annotations             |       :hammer: [#7]       |
| Per class instantiation lifecycle |      :hammer: [#14]       |
| `SkipException` (→ assumption)    |      :hammer: [#32]       |
| Dependency migration              |       :hammer:[#29]       |
| Interceptor interfaces            | :thinking: [#10] \| [#11] |
| Listener interfaces               | :thinking: [#12] \| [#13] |

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
