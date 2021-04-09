import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.githubConnection
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2019.2"

project {
    vcsRoot(PetclinicVcs)
    buildType(wrapWithFeature(Build) {
        swabra {}
    })
    buildType(Publish)

    sequential {
        buildType(wrapWithFeature(Build) {
            swabra {}
        })
        buildType(Publish)
    }

    features {
        githubConnection {
            id = "PROJECT_EXT_3"
            displayName = "GitHub.com"
            clientId = "4e2ed1e3a1fadc2f9b0b"
            clientSecret = "credentialsJSON:5af32dee-9400-4397-84f1-a65064b8d0ec"
        }
    }

}

object Build : BuildType({
    name = "Build"
    artifactRules = "target/*jar"

    vcs {
        branchFilter = """
            +:*
            -:<default>
        """
        root(PetclinicVcs)
    }
    steps {
        maven {
            goals = "clean package"
            localRepoScope = MavenBuildStep.RepositoryScope.MAVEN_DEFAULT
        }
        script {
            println("Renaming artifact")
            println("Base dir ${DslContext.baseDir}")
            println("Settings root ${DslContext.settingsRoot}")
            scriptContent = """
                BRANCH=`echo %teamcity.build.branch% | sed 's|refs/heads/||g'`
                BUILD_NO="%build.counter%"
                echo "were in ${'$'}(pwd)"
                
                echo "Build is running on branch ${'$'}BRANCH"
                echo "Build count is currently at ${'$'}BUILD_NO"
            """.trimIndent()
        }

    }
    triggers {
        vcs {
            groupCheckinsByCommitter = true
        }
    }
})

object Publish: BuildType({
    name = "Publish"
    artifactRules = "*jar"
    steps {
        script {
            println("Publish step content")
            scriptContent = """
                echo 'Creating artifacts directory'
                mkdir -p ./artifacts/
                echo 'Finding created jar artifacts'
                find -name 'target' -type d | xargs -I{} find {} -name "*.jar" | xargs -I{}
                echo 'Copying found artifacts in a folder'
                cp {} ./artifacts/
                echo 'Content of artifacts folder'
                ls -lah artifacts/
            """.trimIndent()
        }
    }
    dependencies {
        snapshot(Build) {}
        artifacts(Build) {
            artifactRules = "*jar"
        }
    }
})

object PetclinicVcs : GitVcsRoot({
    name = "PetclinicVcs"
    url = "https://github.com/spring-projects/spring-petclinic.git"
    branch="refs/heads/main"
    branchSpec=
        """
        +:refs/heads/main
        +:refs/heads/master
        +:refs/heads/(main)
        +:refs/heads/feature/*
        """.trimIndent()
})

fun cleanFiles(buildType: BuildType): BuildType {
    if (buildType.features.items.find { feature -> feature.type == "swabra"} == null) {
        buildType.features { swabra {  } }
    }
    return buildType
}


fun wrapWithFeature(buildType: BuildType, featureBlock: BuildFeatures.() -> Unit): BuildType {
    buildType.features {
        featureBlock()
    }
    return buildType
}