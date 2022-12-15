/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.kotlin.dsl.integration

import org.junit.Test
import spock.lang.Issue


class GradleKotlinDslRegressionsTest : AbstractPluginIntegrationTest() {

    @Test
    @Issue("https://github.com/gradle/gradle/issues/9919")
    fun `gradleKotlinDsl dependency declaration does not throw`() {

        withBuildScript(
            """
            plugins { java }
            dependencies {
                compileOnly(gradleKotlinDsl())
            }
            """
        )

        build("help")
    }

    @Test
    fun `can configure ext extension`() {
        withBuildScript(
            """
            ext {
                set("foo", "bar")
            }
            """
        )

        build("help")
    }

    @Test
    @Issue("https://youtrack.jetbrains.com/issue/KT-55068")
    fun `kotlin ir backend issue kt-55068`() {

        assumeNonEmbeddedGradleExecuter()

        withDefaultSettingsIn("buildSrc")
        withKotlinBuildSrc()
        withFile("buildSrc/src/main/kotlin/my-plugin.gradle.kts", """
            data class Container(val property: Property<String> = objects.property())
        """)
        withBuildScript("""plugins { id("my-plugin") }""")

        build("help")
    }

    @Test
    @Issue("https://youtrack.jetbrains.com/issue/KT-55065")
    fun `kotlin ir backend issue kt-55065`() {

        assumeNonEmbeddedGradleExecuter()

        withDefaultSettingsIn("buildSrc")
        withKotlinBuildSrc()
        withFile("buildSrc/src/main/kotlin/my-plugin.gradle.kts", """
            tasks.withType<DefaultTask>().configureEach {
                val p: String by project
            }
        """)
        withBuildScript("""plugins { id("my-plugin") }""")

        build("help")
    }
}
