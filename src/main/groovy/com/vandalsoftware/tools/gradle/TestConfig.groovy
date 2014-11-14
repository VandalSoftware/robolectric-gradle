package com.vandalsoftware.tools.gradle

class TestConfig {
    private Set<String> includes = []
    private Set<String> excludes = []

    private final String name

    TestConfig(String name) {
        this.name = name
    }

    String getName() {
        return name;
    }

    Set<String> getIncludes() {
        includes
    }

    Set<String> getExcludes() {
        excludes
    }

    void include(String... includes) {
        includes.each {
            this.includes.add(it)
        }
    }

    void exclude(String... excludes) {
        excludes.each {
            this.excludes.add(it)
        }
    }
}
