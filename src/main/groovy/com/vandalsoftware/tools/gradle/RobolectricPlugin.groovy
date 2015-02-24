package com.vandalsoftware.tools.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport

class RobolectricPlugin implements Plugin<Project> {
    public static final String COMPILE_TASK_NAME = 'compile'
    public static final String TEST_TASK_NAME = 'robolectricTest'
    public static final String TASK_NAME_PREFIX = 'robolectric'

    public static final String TEST_COMPILE_CONFIGURATION_NAME = 'testCompile'

    public static final String ROBOLECTRIC_GROUP_NAME = 'Robolectric'
    public static final String CHECK_TASK_NAME = 'check'

    @Override
    void apply(Project project) {
        def variants
        def plugin
        if (project.plugins.hasPlugin(AppPlugin)) {
            variants = project.android.applicationVariants
            plugin = project.plugins.getPlugin(AppPlugin)
        } else if (project.plugins.hasPlugin(LibraryPlugin)) {
            variants = project.android.libraryVariants
            plugin = project.plugins.getPlugin(LibraryPlugin)
        } else {
            throw new UnsupportedOperationException("This project must apply" +
                    " com.android.application or com.android.library")
        }

        def extension = project.extensions.create('robolectric', RobolectricExtension, project)
        TestConfig debugTestConfig = extension.buildTypes.create('debug')

        def compileConfig = project.configurations.getByName(COMPILE_TASK_NAME)

        Configuration testConfig = project.configurations.findByName(TEST_COMPILE_CONFIGURATION_NAME)
        if (testConfig == null) {
            testConfig = project.configurations.create(TEST_COMPILE_CONFIGURATION_NAME) {
                extendsFrom compileConfig
            }
        }

        project.android.productFlavors.all {
            project.configurations.create("test${it.name.capitalize()}Compile", {
                extendsFrom testConfig
            })
        }

        def testAll = project.tasks.create(name: TEST_TASK_NAME, group: ROBOLECTRIC_GROUP_NAME,
                description: 'Runs all Robolectric tests')

        variants.all { variant ->
            def testTypes = extension.buildTypes.matching() {
                it.name.equals(variant.buildType.name)
            }
            if (testTypes.isEmpty()) {
                return
            }
            def flavors = variant.productFlavors.collect { flavor ->
                flavor.name.capitalize()
            }
            def testVariant
            if (flavors.isEmpty()) {
                testVariant = variant.buildType.name.capitalize()
            } else {
                testVariant = flavors.join('') + variant.buildType.name.capitalize()
            }
            def variantJavaCompile = variant.javaCompile
            def testClasspath = testConfig.plus(project.files(variantJavaCompile.destinationDir,
                    variantJavaCompile.classpath))
            flavors.each { flavor ->
                def config = project.configurations.findByName("test${flavor}Compile")
                if (config) {
                    testClasspath.add(config)
                }
            }
            FileTree tree = project.fileTree('src') {
                include 'test/java/**/*.java'
                flavors.each { flavor ->
                    include "test${flavor}/java/**/*.java"
                }
            }
            def testVariantOutputDirName = testVariant.toLowerCase()
            def testDestinationDir =
                    project.file("${project.buildDir}/test-classes/$testVariantOutputDirName")
            def compileTask = project.tasks.create("${TASK_NAME_PREFIX}Compile${testVariant}TestJava",
                    JavaCompile)
            compileTask.dependsOn variantJavaCompile
            compileTask.classpath = testClasspath
            compileTask.source = tree
            compileTask.destinationDir = testDestinationDir
            compileTask.doFirst {
                compileTask.options.bootClasspath = plugin.bootClasspath.join(File.pathSeparator)
            }

            def procResTask = project.tasks.create("${TASK_NAME_PREFIX}Process${testVariant}TestResources",
                    Copy)
            procResTask.from('src/test/resources') {
                include '**/*'
            }
            flavors.each { flavor ->
                procResTask.from("test${flavor}/resources") {
                    include '**/*'
                }
            }
            procResTask.destinationDir =
                    project.file("${project.buildDir}/test-resources/$testVariantOutputDirName")

            def classesTask = project.tasks.create("${TASK_NAME_PREFIX}${testVariant}TestClasses")
            classesTask.dependsOn procResTask, compileTask

            def testTask = project.tasks.create("${TASK_NAME_PREFIX}Test${testVariant}", Test) {
                description = "Runs Robolectric tests on $testVariant variant"
                group = ROBOLECTRIC_GROUP_NAME
            }
            testTask.dependsOn classesTask

            def testResultsOutput =
                    project.file("${project.testResultsDir}/$testVariantOutputDirName/binary/${testTask.name}")
            addConfigToTestTask(extension.defaultConfig, testTask)
            addConfigToTestTask(debugTestConfig, testTask)
            variant.productFlavors.each {
                def flav = extension.productFlavors.findByName(it.name)
                if (flav) {
                    addConfigToTestTask(flav, testTask)
                }
            }
            testTask.reports.html.destination = project.reporting.file(testVariantOutputDirName)
            testTask.reports.junitXml.destination = testResultsOutput
            testTask.binResultsDir = testResultsOutput

            testTask.testClassesDir = testDestinationDir
            testTask.inputs.sourceFiles.from.clear()
            testTask.classpath = testClasspath.plus(project.files(testDestinationDir, procResTask.destinationDir))
            testTask.doFirst {
                testTask.classpath = testClasspath.plus(project.files(testDestinationDir,
                        procResTask.destinationDir,
                        plugin.bootClasspath.join(File.pathSeparator)))
            }
            // Set the applicationId as the packageName to avoid unknown resource errors when
            // applicationIdSuffix is used.
            def applicationId = project.android.defaultConfig.applicationId
            if (applicationId != null) {
                testTask.systemProperties.put('android.package', applicationId)
            }

            // Add the path to the correct manifest, resources, assets as a system property.
            def processedManifestPath = variant.outputs[0].processManifest.manifestOutputFile
            def processedResourcesPath = variant.mergeResources.outputDir
            def processedAssetsPath = variant.mergeAssets.outputDir

            testTask.systemProperties.put('android.manifest', processedManifestPath)
            testTask.systemProperties.put('android.resources', processedResourcesPath)
            testTask.systemProperties.put('android.assets', processedAssetsPath)

            // Work around http://issues.gradle.org/browse/GRADLE-1682
            testTask.scanForTestClasses = false

            if (extension.testCoverageEnabled) {
                if (!project.plugins.hasPlugin(JacocoPlugin)) {
                    project.apply plugin: JacocoPlugin
                }
                testTask.jacoco {
                    destinationFile =
                            project.file("${project.buildDir}/jacoco/${testVariantOutputDirName}/coverage.exec")
                }
                def jacocoTask = project.tasks.create("$TASK_NAME_PREFIX${testVariant}JacocoTestReport", JacocoReport)
                jacocoTask.classDirectories = project.fileTree(
                        dir: variantJavaCompile.destinationDir,
                        excludes: ['**/R.class',
                                   '**/R$*.class',
                                   '**/BuildConfig.*',
                                   '**/Manifest*.*']
                )

                def sourceDirs = [project.android.sourceSets.main.java.srcDirs]
                variant.productFlavors.each { flavor ->
                    sourceDirs.add(project.android.sourceSets[flavor.name].java.srcDirs)
                }
                jacocoTask.sourceDirectories = project.files(sourceDirs)
                jacocoTask.executionData =
                        project.files("${project.buildDir}/jacoco/${testVariantOutputDirName}/coverage.exec")
                jacocoTask.reports {
                    xml.enabled true
                    html {
                        enabled true
                        destination "${project.buildDir}/jacoco/${testVariantOutputDirName}/html"
                    }
                    csv.enabled false
                }
                jacocoTask.dependsOn testTask
                testAll.dependsOn jacocoTask
            } else {
                testAll.dependsOn testTask
            }
        }
        def checkTask = project.tasks.findByName(CHECK_TASK_NAME)
        if (checkTask) {
            checkTask.dependsOn testAll
        }
    }

    static def addConfigToTestTask(TestConfig testConfig, Test testTask) {
        if (testConfig) {
            if (!testConfig.includes.isEmpty()) {
                testTask.include(testConfig.includes)
            }
            if (!testConfig.excludes.isEmpty()) {
                testTask.exclude(testConfig.excludes)
            }
        }
    }
}
