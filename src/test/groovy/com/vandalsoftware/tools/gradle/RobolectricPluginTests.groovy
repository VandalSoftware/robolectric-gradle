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

    @Test
    public void testConfigAdded() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.library'
        project.apply plugin: 'robolectric'
        project.robolectric {
            include '**/*GlobalTest.class'
            exclude '**/*NonGlobalTest.class'
            buildTypes {
                debug {
                    include '**/*'
                    exclude '**/*Tests.class'
                }
            }
            productFlavors {
                free {
                    include '**/*FreeTest.class'
                }
            }
        }
        assert project.robolectric.includes == ['**/*GlobalTest.class'] as Set
        assert project.robolectric.excludes == ['**/*NonGlobalTest.class'] as Set
        assert project.robolectric.buildTypes['debug'].includes == ['**/*'] as Set
        assert project.robolectric.buildTypes['debug'].excludes == ['**/*Tests.class'] as Set
        assert project.robolectric.productFlavors['free'].includes == ['**/*FreeTest.class'] as Set
    }

    @Test
    public void testTaskAdded() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.library'
        project.apply plugin: 'robolectric'
        assert project.tasks.findByName('robolectricTest') != null
    }
}
