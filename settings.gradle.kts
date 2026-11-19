pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral(); maven("https://maven-central.storage-download.googleapis.com/maven2"); maven("https://repository.liferay.com/nexus/content/repositories/public/"); maven("https://maven.wso2.org/nexus/content/repositories/public/"); maven("https://repository.apache.org/content/repositories/releases/"); maven("https://repo1.maven.org/maven2"); maven("https://jitpack.io") } }
rootProject.name = "CastleCrashersMobile"
include(":app")
