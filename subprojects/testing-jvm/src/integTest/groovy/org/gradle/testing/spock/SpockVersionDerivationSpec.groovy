/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.testing.spock

import org.gradle.api.plugins.jvm.internal.DefaultJvmTestSuite
import org.gradle.integtests.fixtures.MultiVersionIntegrationSpec
import org.gradle.integtests.fixtures.TargetCoverage
import org.gradle.testing.fixture.GroovyCoverage
import org.gradle.util.internal.GroovyDependencyUtil

@TargetCoverage({ GroovyCoverage.SUPPORTED_BY_JDK })
class SpockVersionDerivationSpec extends MultiVersionIntegrationSpec {

    def setup() {
        def groovyVersion = versionNumber
        def expectedSpockCoreJar = "spock-core-${DefaultJvmTestSuite.getSpockVersionForGroovy(groovyVersion)}.jar"

        buildScript("""
            plugins {
                id 'groovy'
            }

            ${mavenCentralRepository()}

            dependencies {
                implementation '${GroovyDependencyUtil.groovyModuleDependency('groovy', groovyVersion)}'
            }

            testing {
                suites {
                    integTest(JvmTestSuite) {
                        useSpock()
                    }
                }
            }

            task checkConfiguration {
                dependsOn integTest
                doLast {
                    assert configurations.integTestRuntimeClasspath.files.any { it.name == "${expectedSpockCoreJar}" }
                }
            }
        """)
    }

    def 'spock version adapts to Groovy version'() {
        expect:
        succeeds("checkConfiguration")
    }
}
