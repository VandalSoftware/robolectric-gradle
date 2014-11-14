package com.vandalsoftware.tools.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class RobolectricExtension {
    boolean testCoverageEnabled
    private final NamedDomainObjectContainer<TestConfig> buildTypesContainer
    private final NamedDomainObjectContainer<TestConfig> productFlavorsContainer
    final TestConfig defaultConfig

    RobolectricExtension(Project project) {
        buildTypesContainer = project.container(TestConfig)
        productFlavorsContainer = project.container(TestConfig)
        defaultConfig = new TestConfig('default')
    }

    NamedDomainObjectContainer<TestConfig> getBuildTypes() {
        buildTypesContainer
    }

    NamedDomainObjectContainer<TestConfig> getProductFlavors() {
        productFlavorsContainer
    }

    void buildTypes(Action<NamedDomainObjectContainer<TestConfig>> action) {
        action.execute(buildTypesContainer)
    }

    void productFlavors(Action<NamedDomainObjectContainer<TestConfig>> action) {
        action.execute(productFlavorsContainer)
    }

    void include(String... includes) {
        defaultConfig.include(includes)
    }

    void exclude(String... excludes) {
        defaultConfig.exclude(excludes)
    }

    Set<String> getIncludes() {
        defaultConfig.includes
    }

    Set<String> getExcludes() {
        defaultConfig.excludes
    }
}
