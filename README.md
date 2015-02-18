Robolectric Gradle Plugin
=========================

This project was adapted from [Pivotal Labs' project](https://github.com/robolectric/robolectric-gradle-plugin).

## Key Differences

- Robolectric tests live separately from `androidTest`, which means they can run independently from tests created for the Android testing framework

For example, you can run the `test` task by itself or alongside `connectedCheck`:

```
$ ./gradlew test
$ ./gradlew test connectedCheck
```

## Setup

Apply the plugin:

```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.vandalsoftware.tools.gradle:robolectric:0.3.0'
    }
}

apply plugin: 'robolectric'

dependencies {
    testCompile 'org.robolectric:robolectric:2.4'
}
```

Tests are placed in the same directory as you would normally place them if you were applying the `java` plugin.

```
src/
  main/
  test/
  testFlavor/
```

## Configuration

Use the `testCompile` configuration to customize dependencies:

```
dependencies {
  testCompile 'com.example:example:1.0'
}
```

By default, the plugin adds Robolectric and JUnit JARs. Use the `testCompile` configuration to select specific versions:

```
dependencies {
  testCompile 'junit:junit:4.11'
  testCompile 'org.robolectric:robolectric:2.+'
}
```

### Flavor configuration

```
testPaidCompile 'junit:junit:3.8'
```

### Test task configuration

The `RobolectricExtension` allows you to configure default includes and excludes; and includes and excludes by build type and product flavor:

```
robolectric {
    include '**/Foo*.class'
    
    buildTypes {
        debug {
            include '**/Bar*.class'
        }
    }
    
    productFlavors {
        free {
            exclude '**/PaidTest*.class'
        }
    }
}
```

This configures `robolectricTestFreeDebug` task to include `[**/Foo*.class, **/Bar*.class]` and exclude `[**/PaidTest*.class]`

Alternatively, you may want to configure the Test task directly. Tasks prefixed with `robolectricTest*` are [Test](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.testing.Test.html) tasks and can be configured as such:

```
project.afterEvaluate {
  project.robolectricTestDebug {
    // set a system property for the test JVM(s)
    systemProperty 'some.prop', 'value'

    // explicitly include or exclude tests
    include 'org/foo/**'
    exclude 'org/boo/**'

    // set heap size for the test JVM(s)
    minHeapSize = "128m"
    maxHeapSize = "512m"

    // set JVM arguments for the test JVM(s)
    jvmArgs '-XX:MaxPermSize=256m'
  }
}
```

## License

    Copyright 2013 Square, Inc.
              2014 Pivotal Labs
              2014 Vandal LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
