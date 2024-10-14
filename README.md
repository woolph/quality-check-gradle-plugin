# quality-check-gradle-plugin

`quality-check` is a gradle plugin which adds and preconfigures other plugins which are essential to ensure code quality
and security.

## Quick setup
Add the plugin with the id `io.github.woolph.quality-check` to your gradle build:

>####Kotlin DSL Sample *build.gradle.kts*
>```kotlin
>plugins {
>    id("io.github.woolph.quality-check") version "2.1.0"
>}
>```

>####Groovy DSL Sample *build.gradle*
>```groovy
>plugins {
>    id 'io.github.woolph.quality-check' version '2.1.0'
>}
>```

## What does it do?
The quality check adds several other plugins to your gradle build with the intent to ensure code quality and code 
security. More precisely, it adds the following plugins:
* `org.owasp:dependency-check-gradle:10.0.4` which checks your 3rd party dependencies for known vulnerabilities
* `com.github.jk1:gradle-license-report:2.7` which checks the licenses of your 3rd party dependencies

Your gradle build is automatically configured in a way that the `check` of your build depends upon 
all the relevant tasks introduced by these plugins. Here is what it would look like, if you'd have to do this
explicitly (but you DON'T have to do that, 'cause it is already setup this way!):

````kotlin
tasks.check {
    dependsOn(
        tasks.dependencyCheckAnalyze,
        tasks.checkLicense,
    )
}
````

The `check` task (which also depends on the `test` task executing the unit tests of your project) will be executed 
with every execution of the `build` task, so you do not have to do anything except for adding the plugin to your build.

In some cases, some tweaking of the checks performed may be necessary to better fit your projects purpose.

### Dependency Check
#### Suppression file
With the help of the suppression file (usually located here `{projectDir}\dependency-check-suppression.xml`) you can 
suppress either false positives or vulnerabilities you want to ignore for now.

Please make sure to either mark these suppression entries with a FALSE-POSITIVE "tag" or with an expiration date because 
there's a task `checkSuppressionFile` which is performed before `dependencyCheckAnalyze` and checks the suppression file 
provided for "inappropriate" suppression entries. An entry is considered appropriate it 
* it contains a FALSE POSITIVE "tag" in the notes, which indicates that the suppressed vulnerability is not affecting the 
application,
* or it has an expiration date which is not further way than the maxSuppressionUntil (usually a year from now). Passing 
the expiration date disables the suppression entry, forcing you to reevaluate the vulnerability. Therefore, this expiring 
suppression may be useful for vulnerabilities that, for some reason, cannot be fixed right away.

##### Generate a suppression file
There is also a task which allows you to generate a new suppression file containing all the suppression entries of the 
original file which are still necessary (suppression entries which aren't needed anymore are removed) adding new 
suppression entries for each vulnerability found in the dependencyCheck report (so, please make sure to perform the 
`dependencyCheckAnalyze` task beforehand).

````shell
gradlew generateSuppressionFile -PsuppressUntil=2023-04-01
````

##### Show vulnerabilities in dependency tree
The following command can be used to highlight all dependencies in the dependency tree for which there is 
currently an entry in the suppression file:
````shell
gradlew dependencies --configuration runtimeClasspath | grep --color=auto -E "$(cat dependency-check-suppression.xml | grep '<packageUrl>' | sed 's_.*pkg:maven/\(.*\)/\(.*\)@.*_\1:\2_' | sort -u | tr '\n' '|')\$"
````

#### Using a mirror database
The `dependencyCheckAnalyze` task relies on a database containing information about vulnerabilities in 3rd party 
libraries and build artifacts. This information comes e.g. from the Nist NVD. By default, the plugin downloads
the data and stores it in a local database in your local gradle cache. Once a database exists, the task checks if it
needs to be updated.

Under certain circumstances it might be beneficially to avoid this default mechanism (for example on build agents
you might lose the gradle cache, which causes the dependencyCheckUpdate task to download the NVD data for every build 
anew), by providing a database for the plugin to be used. You can do so by setting special gradle properties:

* `DEPENDENCY_CHECK_DB_DRIVER` is the full-qualified name of the jdbc driver (the following drivers are readily 
available in the classpath of this gradle plugin:)
  * `org.postgresql.Driver` (from `org.postgresql:postgresql:42.4.0`)
  * `com.microsoft.sqlserver.jdbc.SQLServerDriver` (from `com.microsoft.sqlserver:mssql-jdbc:12.2.0.jre11`)
* `DEPENDENCY_CHECK_DB_CONNECTION` is a jdbc string for accessing the database
* `DEPENDENCY_CHECK_DB_USER`
* `DEPENDENCY_CHECK_DB_PASSWORD`


### License Report
In software projects, you may want to take a close look on the licenses of your 3rd party dependencies, because some of 
the commonly used licenses could force you to disclose your source code, which may be detrimental especially for closed 
source projects. The license report plugin allows you to define a set of licenses which you consider "safe" and the 
`checkLicense` (which the `check` task will depend upon) checks whether the licenses of your 3rd party dependencies are
within this set. If not, the build fails.
