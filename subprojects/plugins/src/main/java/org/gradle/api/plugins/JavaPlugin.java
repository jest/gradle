/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.api.plugins;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactoryInternal;
import org.gradle.api.internal.component.BuildableJavaComponent;
import org.gradle.api.internal.component.ComponentRegistry;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.JvmConstants;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.component.local.model.OpaqueComponentIdentifier;
import org.gradle.internal.execution.BuildOutputCleanupRegistry;
import org.gradle.jvm.component.internal.DefaultJvmSoftwareComponent;
import org.gradle.testing.base.TestingExtension;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

import static org.gradle.api.plugins.JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME;

/**
 * <p>A {@link Plugin} which compiles and tests Java source, and assembles it into a JAR file.</p>
 *
 * This plugin creates a built-in {@link JvmTestSuite test suite} named {@code test} that represents the {@link Test} task for Java projects.
 *
 * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html">Java plugin reference</a>
 * @see <a href="https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html">JVM test suite plugin reference</a>
 */
public abstract class JavaPlugin implements Plugin<Project> {
    /**
     * The name of the task that processes resources.
     */
    public static final String PROCESS_RESOURCES_TASK_NAME = JvmConstants.PROCESS_RESOURCES_TASK_NAME;

    /**
     * The name of the lifecycle task which outcome is that all the classes of a component are generated.
     */
    public static final String CLASSES_TASK_NAME = JvmConstants.CLASSES_TASK_NAME;

    /**
     * The name of the task which compiles Java sources.
     */
    public static final String COMPILE_JAVA_TASK_NAME = JvmConstants.COMPILE_JAVA_TASK_NAME;

    /**
     * The name of the task which processes the test resources.
     */
    public static final String PROCESS_TEST_RESOURCES_TASK_NAME = JvmConstants.PROCESS_TEST_RESOURCES_TASK_NAME;

    /**
     * The name of the lifecycle task which outcome is that all test classes of a component are generated.
     */
    public static final String TEST_CLASSES_TASK_NAME = JvmConstants.TEST_CLASSES_TASK_NAME;

    /**
     * The name of the task which compiles the test Java sources.
     */
    public static final String COMPILE_TEST_JAVA_TASK_NAME = JvmConstants.COMPILE_TEST_JAVA_TASK_NAME;

    /**
     * The name of the task which triggers execution of tests.
     */
    public static final String TEST_TASK_NAME = JvmConstants.TEST_TASK_NAME;

    /**
     * The name of the task which generates the component main jar.
     */
    public static final String JAR_TASK_NAME = JvmConstants.JAR_TASK_NAME;

    /**
     * The name of the task which generates the component javadoc.
     */
    public static final String JAVADOC_TASK_NAME = JvmConstants.JAVADOC_TASK_NAME;

    /**
     * The name of the API configuration, where dependencies exported by a component at compile time should
     * be declared.
     *
     * @since 3.4
     */
    public static final String API_CONFIGURATION_NAME = JvmConstants.API_CONFIGURATION_NAME;

    /**
     * The name of the implementation configuration, where dependencies that are only used internally by
     * a component should be declared.
     *
     * @since 3.4
     */
    public static final String IMPLEMENTATION_CONFIGURATION_NAME = JvmConstants.IMPLEMENTATION_CONFIGURATION_NAME;

    /**
     * The name of the configuration to define the API elements of a component.
     * That is, the dependencies which are required to compile against that component.
     *
     * @since 3.4
     */
    public static final String API_ELEMENTS_CONFIGURATION_NAME = JvmConstants.API_ELEMENTS_CONFIGURATION_NAME;

    /**
     * The name of the configuration that is used to declare dependencies which are only required to compile a component,
     * but not at runtime.
     */
    public static final String COMPILE_ONLY_CONFIGURATION_NAME = JvmConstants.COMPILE_ONLY_CONFIGURATION_NAME;

    /**
     * The name of the configuration to define the API elements of a component that are required to compile a component,
     * but not at runtime.
     *
     * @since 6.7
     */
    public static final String COMPILE_ONLY_API_CONFIGURATION_NAME = JvmConstants.COMPILE_ONLY_API_CONFIGURATION_NAME;

    /**
     * The name of the runtime only dependencies configuration, used to declare dependencies
     * that should only be found at runtime.
     *
     * @since 3.4
     */
    public static final String RUNTIME_ONLY_CONFIGURATION_NAME = JvmConstants.RUNTIME_ONLY_CONFIGURATION_NAME;

    /**
     * The name of the runtime classpath configuration, used by a component to query its own runtime classpath.
     *
     * @since 3.4
     */
    public static final String RUNTIME_CLASSPATH_CONFIGURATION_NAME = JvmConstants.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

    /**
     * The name of the runtime elements configuration, that should be used by consumers
     * to query the runtime dependencies of a component.
     *
     * @since 3.4
     */
    public static final String RUNTIME_ELEMENTS_CONFIGURATION_NAME = JvmConstants.RUNTIME_ELEMENTS_CONFIGURATION_NAME;

    /**
     * The name of the javadoc elements configuration.
     *
     * @since 6.0
     */
    public static final String JAVADOC_ELEMENTS_CONFIGURATION_NAME = JvmConstants.JAVADOC_ELEMENTS_CONFIGURATION_NAME;

    /**
     * The name of the sources elements configuration.
     *
     * @since 6.0
     */
    public static final String SOURCES_ELEMENTS_CONFIGURATION_NAME = JvmConstants.SOURCES_ELEMENTS_CONFIGURATION_NAME;

    /**
     * The name of the compile classpath configuration.
     *
     * @since 3.4
     */
    public static final String COMPILE_CLASSPATH_CONFIGURATION_NAME = JvmConstants.COMPILE_CLASSPATH_CONFIGURATION_NAME;

    /**
     * The name of the annotation processor configuration.
     *
     * @since 4.6
     */
    public static final String ANNOTATION_PROCESSOR_CONFIGURATION_NAME = JvmConstants.ANNOTATION_PROCESSOR_CONFIGURATION_NAME;

    /**
     * The name of the test implementation dependencies configuration.
     *
     * @since 3.4
     */
    public static final String TEST_IMPLEMENTATION_CONFIGURATION_NAME = JvmConstants.TEST_IMPLEMENTATION_CONFIGURATION_NAME;

    /**
     * The name of the configuration that should be used to declare dependencies which are only required
     * to compile the tests, but not when running them.
     */
    public static final String TEST_COMPILE_ONLY_CONFIGURATION_NAME = JvmConstants.TEST_COMPILE_ONLY_CONFIGURATION_NAME;

    /**
     * The name of the test runtime only dependencies configuration.
     *
     * @since 3.4
     */
    public static final String TEST_RUNTIME_ONLY_CONFIGURATION_NAME = JvmConstants.TEST_RUNTIME_ONLY_CONFIGURATION_NAME;

    /**
     * The name of the test compile classpath configuration.
     *
     * @since 3.4
     */
    public static final String TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME = JvmConstants.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME;

    /**
     * The name of the test annotation processor configuration.
     *
     * @since 4.6
     */
    public static final String TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME = JvmConstants.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME;

    /**
     * The name of the test runtime classpath configuration.
     *
     * @since 3.4
     */
    public static final String TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME = JvmConstants.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME;

    private final ObjectFactory objectFactory;

    @Inject
    public JavaPlugin(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public void apply(final Project project) {
        if (project.getPluginManager().hasPlugin("java-platform")) {
            throw new IllegalStateException("The \"java\" or \"java-library\" plugin cannot be applied together with the \"java-platform\" plugin. " +
                "A project is either a platform or a library but cannot be both at the same time.");
        }
        final ProjectInternal projectInternal = (ProjectInternal) project;

        project.getPluginManager().apply(JavaBasePlugin.class);
        project.getPluginManager().apply("org.gradle.jvm-test-suite");

        JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);

        // Create the 'java' component.
        DefaultJvmSoftwareComponent javaComponent = objectFactory.newInstance(DefaultJvmSoftwareComponent.class, "java", javaExtension);
        project.getComponents().add(javaComponent);

        // Set the 'java' component as the project's default.
        Configuration defaultConfiguration = project.getConfigurations().getByName(Dependency.DEFAULT_CONFIGURATION);
        defaultConfiguration.extendsFrom(javaComponent.getRuntimeElements());
        projectInternal.getServices().get(ComponentRegistry.class).setMainComponent(new BuildableJavaComponentImpl(javaComponent));

        BuildOutputCleanupRegistry buildOutputCleanupRegistry = projectInternal.getServices().get(BuildOutputCleanupRegistry.class);

        configureBuiltInTest(project, javaComponent);
        configureSourceSets(javaExtension, buildOutputCleanupRegistry);
        configureBuild(project);
    }

    private static void configureSourceSets(JavaPluginExtension pluginExtension, final BuildOutputCleanupRegistry buildOutputCleanupRegistry) {
        // Register the project's source set output directories
        pluginExtension.getSourceSets().all(sourceSet -> buildOutputCleanupRegistry.registerOutputs(sourceSet.getOutput()));
    }

    private static void configureBuiltInTest(Project project, DefaultJvmSoftwareComponent javaComponent) {
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
        final NamedDomainObjectProvider<JvmTestSuite> testSuite = testing.getSuites().register(DEFAULT_TEST_SUITE_NAME, JvmTestSuite.class, suite -> {
            final SourceSet testSourceSet = suite.getSources();
            ConfigurationContainer configurations = project.getConfigurations();

            Configuration testImplementationConfiguration = configurations.getByName(testSourceSet.getImplementationConfigurationName());
            Configuration testRuntimeOnlyConfiguration = configurations.getByName(testSourceSet.getRuntimeOnlyConfigurationName());
            Configuration testCompileClasspathConfiguration = configurations.getByName(testSourceSet.getCompileClasspathConfigurationName());
            Configuration testRuntimeClasspathConfiguration = configurations.getByName(testSourceSet.getRuntimeClasspathConfigurationName());

            // We cannot reference the main source set lazily (via a callable) since the IntelliJ model builder
            // relies on the main source set being created before the tests. So, this code here cannot live in the
            // JvmTestSuitePlugin and must live here, so that we can ensure we register this test suite after we've
            // created the main source set.
            final SourceSet mainSourceSet = javaComponent.getSources();
            final FileCollection mainSourceSetOutput = mainSourceSet.getOutput();
            final FileCollection testSourceSetOutput = testSourceSet.getOutput();
            testSourceSet.setCompileClasspath(project.getObjects().fileCollection().from(mainSourceSetOutput, testCompileClasspathConfiguration));
            testSourceSet.setRuntimeClasspath(project.getObjects().fileCollection().from(testSourceSetOutput, mainSourceSetOutput, testRuntimeClasspathConfiguration));

            testImplementationConfiguration.extendsFrom(configurations.getByName(mainSourceSet.getImplementationConfigurationName()));
            testRuntimeOnlyConfiguration.extendsFrom(configurations.getByName(mainSourceSet.getRuntimeOnlyConfigurationName()));
        });

        // Force the realization of this test suite, targets and task
        testSuite.get();

        project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME, task -> task.dependsOn(testSuite));
    }

    private static void configureBuild(Project project) {
        project.getTasks().named(JavaBasePlugin.BUILD_NEEDED_TASK_NAME, task -> addDependsOnTaskInOtherProjects(task, true,
            JavaBasePlugin.BUILD_NEEDED_TASK_NAME, TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME));
        project.getTasks().named(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME, task -> addDependsOnTaskInOtherProjects(task, false,
            JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME, TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME));
    }

    /**
     * Adds a dependency on tasks with the specified name in other projects.  The other projects are determined from
     * project lib dependencies using the specified configuration name. These may be projects this project depends on or
     * projects that depend on this project based on the useDependOn argument.
     *
     * @param task Task to add dependencies to
     * @param useDependedOn if true, add tasks from projects this project depends on, otherwise use projects that depend on this one.
     * @param otherProjectTaskName name of task in other projects
     * @param configurationName name of configuration to use to find the other projects
     */
    private static void addDependsOnTaskInOtherProjects(final Task task, boolean useDependedOn, String otherProjectTaskName, String configurationName) {
        Project project = task.getProject();
        final Configuration configuration = project.getConfigurations().getByName(configurationName);
        task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, otherProjectTaskName));
    }

    /**
     * This is only used by buildSrc to add to the buildscript classpath.
     */
    private static class BuildableJavaComponentImpl implements BuildableJavaComponent {
        private final DefaultJvmSoftwareComponent java;

        public BuildableJavaComponentImpl(DefaultJvmSoftwareComponent java) {
            this.java = java;
        }

        @Override
        public Collection<String> getBuildTasks() {
            return Collections.singleton(JavaBasePlugin.BUILD_TASK_NAME);
        }

        @Override
        public FileCollection getRuntimeClasspath() {
            ArtifactView view = java.getRuntimeClasspath().getIncoming().artifactView(config -> {
                config.componentFilter(componentId -> {
                    if (componentId instanceof OpaqueComponentIdentifier) {
                        DependencyFactoryInternal.ClassPathNotation classPathNotation = ((OpaqueComponentIdentifier) componentId).getClassPathNotation();
                        return classPathNotation != DependencyFactoryInternal.ClassPathNotation.GRADLE_API && classPathNotation != DependencyFactoryInternal.ClassPathNotation.LOCAL_GROOVY;
                    }
                    return true;
                });
            });
            return java.getRuntimeElements().getOutgoing().getArtifacts().getFiles().plus(view.getFiles());
        }

        @Override
        public Configuration getCompileDependencies() {
            return java.getCompileClasspath();
        }
    }
}
