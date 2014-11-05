package com.vandalsoftware.tools.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class RobolectricPluginTests {
    @Test(expected = Exception)
    public void checksAndroidPlugin() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'robolectric'
    }

    @Test
    public void checksAppPlugin() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'robolectric'
        assert project.plugins.hasPlugin(RobolectricPlugin)
    }

    @Test
    public void checksLibraryPlugin() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.library'
        project.apply plugin: 'robolectric'
        assert project.plugins.hasPlugin(RobolectricPlugin)
    }
}
