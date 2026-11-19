pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral(); maven("https://repo1.maven.org/maven2"); maven("https://jitpack.io") } }
rootProject.name = "CastleCrashersMobile"
include(":app")
