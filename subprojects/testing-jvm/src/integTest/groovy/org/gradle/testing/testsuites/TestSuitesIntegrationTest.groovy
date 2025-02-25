/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testing.testsuites

import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.internal.tasks.testing.testng.TestNGTestFramework
import org.gradle.api.plugins.jvm.internal.DefaultJvmTestSuite
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.JUnitXmlTestExecutionResult
import spock.lang.Issue

class TestSuitesIntegrationTest extends AbstractIntegrationSpec {
    def "new test suites adds appropriate test tasks"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    eagerTest(JvmTestSuite)
                    register("registerTest", JvmTestSuite)
                }
            }
        """
        expect:
        succeeds("eagerTest")
        succeeds("registerTest")
    }

    def "built-in test suite does not have any testing framework set at the test suite level"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            tasks.test {
                doLast {
                    assert testFramework instanceof ${JUnitTestFramework.canonicalName}
                    assert classpath.empty
                }
            }
        """
        expect:
        succeeds("test")
    }

    def "configuring test framework on built-in test suite is honored in task and dependencies with JUnit"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    test {
                        useJUnit()
                    }
                }
            }

            tasks.test {
                doLast {
                    assert testFramework instanceof ${JUnitTestFramework.canonicalName}
                    assert classpath.size() == 2
                    assert classpath.any { it.name == "junit-${DefaultJvmTestSuite.TestingFramework.JUNIT4.getDefaultVersion()}.jar" }
                }
            }
        """
        expect:
        succeeds("test")
    }

    def "configuring test framework on built-in test suite is honored in task and dependencies with JUnit and explicit version"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    test {
                        useJUnit("4.12")
                    }
                }
            }

            tasks.test {
                doLast {
                    assert testFramework instanceof ${JUnitTestFramework.canonicalName}
                    assert classpath.size() == 2
                    assert classpath.any { it.name == "junit-4.12.jar" }
                }
            }
        """
        expect:
        succeeds("test")
    }

    def "configuring test framework on built-in test suite using a Provider is honored in task and dependencies with JUnit and explicit version"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            Provider<String> junitVersion = project.provider(() -> '4.12')

            testing {
                suites {
                    test {
                        useJUnit(junitVersion)
                    }
                }
            }

            tasks.test {
                doLast {
                    assert testFramework instanceof ${JUnitTestFramework.canonicalName}
                    assert classpath.size() == 2
                    assert classpath.any { it.name == "junit-4.12.jar" }
                }
            }
        """
        expect:
        succeeds("test")
    }

    def "configuring test framework on built-in test suite is honored in task and dependencies with JUnit Jupiter"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    test {
                        useJUnitJupiter()
                    }
                }
            }

            tasks.test {
                doLast {
                    assert test.testFramework instanceof ${JUnitPlatformTestFramework.canonicalName}
                    assert classpath.size() == 8
                    assert classpath.any { it.name =~ /junit-platform-launcher-.*.jar/ }
                    assert classpath.any { it.name == "junit-jupiter-${DefaultJvmTestSuite.TestingFramework.JUNIT_JUPITER.getDefaultVersion()}.jar" }
                }
            }
        """
        expect:
        succeeds("test")
    }

    def "configuring test framework on built-in test suite is honored in task and dependencies with JUnit Jupiter with explicit version"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    test {
                        useJUnitJupiter("5.7.2")
                    }
                }
            }

            tasks.test {
                doLast {
                    assert testFramework instanceof ${JUnitPlatformTestFramework.canonicalName}
                    assert classpath.size() == 9
                    assert classpath.any { it.name == "junit-jupiter-5.7.2.jar" }
                }
            }
        """
        expect:
        succeeds("test")
    }

    def "configuring test framework on built-in test suite using a Provider is honored in task and dependencies with JUnit Jupiter with explicit version"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            Provider<String> junitVersion = project.provider(() -> '5.7.2')

            testing {
                suites {
                    test {
                        useJUnitJupiter(junitVersion)
                    }
                }
            }

            tasks.test {
                doLast {
                    assert testFramework instanceof ${JUnitPlatformTestFramework.canonicalName}
                    assert classpath.size() == 9
                    assert classpath.any { it.name == "junit-jupiter-5.7.2.jar" }
                }
            }
        """
        expect:
        succeeds("test")
    }

    def "conventional test framework on custom test suite is JUnit Jupiter"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    integTest(JvmTestSuite)
                }
            }

            tasks.integTest {
                doLast {
                    assert testFramework instanceof ${JUnitPlatformTestFramework.canonicalName}
                    assert classpath.size() == 8
                    assert classpath.any { it.name =~ /junit-platform-launcher-.*.jar/ }
                    assert classpath.any { it.name == "junit-jupiter-${DefaultJvmTestSuite.TestingFramework.JUNIT_JUPITER.getDefaultVersion()}.jar" }
                }
            }
        """
        expect:
        succeeds("integTest")
    }

    def "configuring test framework on custom test suite is honored in task and dependencies with #testingFrameworkDeclaration"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    integTest(JvmTestSuite) {
                        ${testingFrameworkDeclaration}
                    }
                }
            }

            tasks.integTest {
                doLast {
                    assert testFramework instanceof ${testingFrameworkType.canonicalName}
                    assert classpath.any { it.name == "${testingFrameworkDep}" }
                }
            }
        """
        expect:
        succeeds("integTest")

        where: // When testing a custom version, this should be a different version that the default
        testingFrameworkDeclaration  | testingFrameworkType       | testingFrameworkDep
        'useJUnit()'                 | JUnitTestFramework         | "junit-${DefaultJvmTestSuite.TestingFramework.JUNIT4.getDefaultVersion()}.jar"
        'useJUnit("4.12")'           | JUnitTestFramework         | "junit-4.12.jar"
        'useJUnitJupiter()'          | JUnitPlatformTestFramework | "junit-jupiter-${DefaultJvmTestSuite.TestingFramework.JUNIT_JUPITER.getDefaultVersion()}.jar"
        'useJUnitJupiter("5.7.1")'   | JUnitPlatformTestFramework | "junit-jupiter-5.7.1.jar"
        'useSpock()'                 | JUnitPlatformTestFramework | "spock-core-${DefaultJvmTestSuite.TestingFramework.SPOCK.getDefaultVersion()}.jar"
        'useSpock("2.2-groovy-3.0")' | JUnitPlatformTestFramework | "spock-core-2.2-groovy-3.0.jar"
        'useSpock("2.2-groovy-4.0")' | JUnitPlatformTestFramework | "spock-core-2.2-groovy-4.0.jar"
        'useKotlinTest()'            | JUnitPlatformTestFramework | "kotlin-test-junit5-${DefaultJvmTestSuite.TestingFramework.KOTLIN_TEST.getDefaultVersion()}.jar"
        'useKotlinTest("1.5.30")'    | JUnitPlatformTestFramework | "kotlin-test-junit5-1.5.30.jar"
        'useTestNG()'                | TestNGTestFramework        | "testng-${DefaultJvmTestSuite.TestingFramework.TESTNG.getDefaultVersion()}.jar"
        'useTestNG("7.3.0")'         | TestNGTestFramework        | "testng-7.3.0.jar"
    }

    def "configuring test framework on custom test suite using a Provider is honored in task and dependencies with #testingFrameworkMethod version #testingFrameworkVersion"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            Provider<String> frameworkVersion = project.provider(() -> '$testingFrameworkVersion')

            testing {
                suites {
                    integTest(JvmTestSuite) {
                        $testingFrameworkMethod(frameworkVersion)
                    }
                }
            }

            tasks.integTest {
                doLast {
                    assert testFramework instanceof ${testingFrameworkType.canonicalName}
                    assert classpath.any { it.name == "$testingFrameworkDep" }
                }
            }
        """
        expect:
        succeeds("integTest")

        where: // When testing a custom version, this should be a different version that the default
        testingFrameworkMethod       | testingFrameworkVersion      | testingFrameworkType       | testingFrameworkDep
        'useJUnit'                   | '4.12'                       | JUnitTestFramework         | "junit-4.12.jar"
        'useJUnitJupiter'            | '5.7.1'                      | JUnitPlatformTestFramework | "junit-jupiter-5.7.1.jar"
        'useSpock'                   | '2.2-groovy-3.0'             | JUnitPlatformTestFramework | "spock-core-2.2-groovy-3.0.jar"
        'useSpock'                   | '2.2-groovy-4.0'             | JUnitPlatformTestFramework | "spock-core-2.2-groovy-4.0.jar"
        'useKotlinTest'              | '1.5.30'                     | JUnitPlatformTestFramework | "kotlin-test-junit5-1.5.30.jar"
        'useTestNG'                  | '7.3.0'                      | TestNGTestFramework        | "testng-7.3.0.jar"
    }

    def "can override previously configured test framework on a test suite"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    integTest(JvmTestSuite) {
                        useJUnit()
                        useJUnitJupiter()
                    }
                }
            }

            testing {
                suites {
                    integTest {
                        useJUnit()
                    }
                }
            }

            tasks.integTest {
                doLast {
                    assert testFramework instanceof ${JUnitTestFramework.canonicalName}
                    assert classpath.size() == 2
                    assert classpath.any { it.name == "junit-4.13.2.jar" }
                }
            }
        """
        expect:
        succeeds("integTest")
    }

    def "task configuration overrules test suite configuration"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    integTest(JvmTestSuite) {
                        // uses junit jupiter by default
                        targets {
                            all {
                                testTask.configure {
                                    useJUnit()
                                }
                            }
                        }
                    }
                }
            }

            // force integTest to be configured
            tasks.named("integTest").get()
        """
        expect:
        fails("help")
        failure.assertHasCause("The value for task ':integTest' property 'testFrameworkProperty' cannot be changed any further.")
    }

    def "task configuration overrules test suite configuration with test suite set test framework"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    integTest(JvmTestSuite) {
                        useJUnit()
                        targets {
                            all {
                                testTask.configure {
                                    useJUnitPlatform()
                                }
                            }
                        }
                    }
                }
            }

            // force integTest to be configured
            tasks.named("integTest").get()
        """
        expect:
        fails("help")
        failure.assertHasCause("The value for task ':integTest' property 'testFrameworkProperty' cannot be changed any further.")
    }

    @Issue("https://github.com/gradle/gradle/issues/18622")
    def "custom Test tasks eagerly realized prior to Java and Test Suite plugin application do not fail to be configured when combined with test suites"() {
        buildFile << """
            tasks.withType(Test) {
                // realize all test tasks
            }
            tasks.register("mytest", Test)
            apply plugin: 'java'

            ${mavenCentralRepository()}

            testing {
                suites {
                    test {
                        useJUnit()
                    }
                }
            }
"""
        file('src/test/java/example/UnitTest.java') << '''
            package example;

            import org.junit.Assert;
            import org.junit.Test;

            public class UnitTest {
                @Test
                public void unitTest() {
                    Assert.assertTrue(true);
                }
            }
        '''
        expect:
        succeeds("mytest")
        def unitTestResults = new JUnitXmlTestExecutionResult(testDirectory, 'build/test-results/mytest')
        unitTestResults.assertTestClassesExecuted('example.UnitTest')
    }

    @Issue("https://github.com/gradle/gradle/issues/18622")
    def "custom Test tasks still function if java plugin is never applied to create sourcesets"() {
        buildFile << """
            tasks.withType(Test) {
                // realize all test tasks
            }

            def customClassesDir = file('src/custom/java')
            tasks.register("mytest", Test) {
                // Must ensure a base dir is set here
                testClassesDirs = files(customClassesDir)
            }

            task assertNoTestClasses {
                inputs.files mytest.testClassesDirs

                doLast {
                    assert inputs.files.contains(customClassesDir)
                }
            }
        """
        expect:
        succeeds("mytest", "assertNoTestClasses")
    }

    def "multiple getTestingFramework() calls on a test suite return same instance"() {
        given:
        buildFile << """
            plugins {
                id 'java'
            }

            def first = testing.suites.test.getTestSuiteTestingFramework()
            def second = testing.suites.test.getTestSuiteTestingFramework()

            tasks.register('assertSameFrameworkInstance') {
                doLast {
                    assert first.getOrNull() === second.getOrNull()
                }
            }""".stripIndent()

        expect:
        succeeds("assertSameFrameworkInstance")
    }

    def "multiple getTestingFramework() calls on a test suite return same instance even when calling useJUnit"() {
        given:
        buildFile << """
            plugins {
                id 'java'
            }

            def first = testing.suites.test.getTestSuiteTestingFramework()

            testing {
                suites {
                    test {
                        useJUnit()
                    }
                }
            }

            def second = testing.suites.test.getTestSuiteTestingFramework()

            tasks.register('assertSameFrameworkInstance') {
                doLast {
                    assert first.get() === second.get()
                }
            }""".stripIndent()

        expect:
        succeeds("assertSameFrameworkInstance")
    }

    def "the default test suite does NOT use JUnit 4 by default"() {
        given: "a build which uses the default test suite and doesn't specify a testing framework"
        file("build.gradle") << """
            plugins {
                id 'java-library'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    test {
                        // Empty
                    }
                }
            }
        """

        and: "containing a test which uses Junit 4"
        file("src/test/java/org/test/MyTest.java") << """
            package org.test;

            import org.junit.Test;
            import org.junit.Assert;

            public class MyTest {
                @Test
                public void testSomething() {
                    Assert.assertEquals(1, MyFixture.calculateSomething());
                }
            }
        """

        expect: "does NOT compile due to a missing dependency"
        fails("test")
        failure.assertHasErrorOutput("Compilation failed; see the compiler error output for details.")
        failure.assertHasErrorOutput("error: package org.junit does not exist")
    }

    def "eagerly iterating over dependency bucket does not break automatic dependencies for test suite"() {
        buildFile << """
            plugins {
                id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    integTest(JvmTestSuite)
                }
            }

            // mimics behavior from https://github.com/JetBrains/kotlin/commit/4a172286217a1a7d4e7a7f0eb6a0bc53ebf56515
            configurations.integTestImplementation.dependencies.all { }

            testing {
                suites {
                    integTest {
                        useJUnit('4.12')
                    }
                }
            }

            tasks.integTest {
                doLast {
                    assert classpath.any { it.name == "junit-4.12.jar" }
                }
            }
        """
        expect:
        succeeds("integTest")
    }

    @Issue("https://github.com/gradle/gradle/issues/20846")
    def "when tests are NOT run they are NOT configured"() {
        given: "a build which will throw an exception upon configuring test tasks"
        file("build.gradle") << """
            plugins {
                id 'java-library'
            }

            ${mavenCentralRepository()}

            tasks.withType(Test).configureEach {
                throw new RuntimeException('Configuring tests failed')
            }
        """

        and: "containing a class to compile"
        file("src/main/java/org/test/App.java") << """
            public class App {
                public String getGreeting() {
                    return "Hello World!";
                }
            }
        """

        and: "containing a test"
        file("src/test/java/org/test/MyTest.java") << """
            package org.test;

            import org.junit.*;

            public class MyTest {
                @Test
                public void testSomething() {
                    App classUnderTest = new App();
                    Assert.assertNotNull(classUnderTest.getGreeting(), "app should have a greeting");
                }
            }
        """

        expect: "compilation does not configure tests"
        succeeds("compileJava")

        and: "running tests fails due to configuring tests"
        fails("test")
        failure.assertHasErrorOutput("Configuring tests failed")
    }

    @Issue("https://github.com/gradle/gradle/issues/20846")
    def "when tests are NOT run they are NOT configured - even when adding an implementation dep"() {
        given: "a build which will throw an exception upon configuring test tasks"
        file("build.gradle") << """
            plugins {
                id 'java-library'
            }

            ${mavenCentralRepository()}

            dependencies {
                implementation 'com.google.guava:guava:30.1.1-jre'
            }

            tasks.withType(Test).configureEach {
                throw new RuntimeException('Configuring tests failed')
            }
        """

        and: "containing a class to compile"
        file("src/main/java/org/test/App.java") << """
            public class App {
                public String getGreeting() {
                    return "Hello World!";
                }
            }
        """

        and: "containing a test"
        file("src/test/java/org/test/MyTest.java") << """
            package org.test;

            import org.junit.*;

            public class MyTest {
                @Test
                public void testSomething() {
                    App classUnderTest = new App();
                    Assert.assertNotNull(classUnderTest.getGreeting(), "app should have a greeting");
                }
            }
        """

        expect: "compilation does NOT configure tests"
        succeeds("compileJava")

        and: "running tests fails due to configuring tests"
        fails("test")
        failure.assertHasErrorOutput("Configuring tests failed")
    }

    @Issue("https://github.com/gradle/gradle/issues/20846")
    def "when tests are NOT run they are NOT configured - even when adding an implementation dep to the integration test suite"() {
        given: "a build which will throw an exception upon configuring test tasks"
        file("build.gradle") << """
            plugins {
                id 'java-library'
            }

            ${mavenCentralRepository()}

            tasks.withType(Test).configureEach {
                throw new RuntimeException('Configuring tests failed')
            }

            testing {
                suites {
                    integrationTest(JvmTestSuite) {
                        useJUnit()
                    }
                }
            }

            dependencies {
                integrationTestImplementation 'com.google.guava:guava:30.1.1-jre'
            }
        """

        and: "containing a class to compile"
        file("src/main/java/org/test/App.java") << """
            public class App {
                public String getGreeting() {
                    return "Hello World!";
                }
            }
        """

        and: "containing an integration test"
        file("src/integrationTest/java/org/test/MyTest.java") << """
            package org.test;

            import org.junit.*;

            public class MyTest {
                @Test
                public void testSomething() {
                    App classUnderTest = new App();
                    Assert.assertNotNull(classUnderTest.getGreeting(), "app should have a greeting");
                }
            }
        """

        expect: "compilation does NOT configure tests"
        succeeds("compileJava")

        and: "running integration tests fails due to configuring tests"
        fails("integrationTest")
        failure.assertHasErrorOutput("Configuring tests failed")
    }
}
