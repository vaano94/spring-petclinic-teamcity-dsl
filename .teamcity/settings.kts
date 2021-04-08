import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
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

version = "2020.2"

project {

    vcsRoot(PetclinicVcs)

    buildType(Build)

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
        root(PetclinicVcs)
    }

    steps {
        maven {
            goals = "clean package"
            localRepoScope = MavenBuildStep.RepositoryScope.MAVEN_DEFAULT
        }
    }

    triggers {
        vcs {
            groupCheckinsByCommitter = true
        }
    }
})

object PetclinicVcs : GitVcsRoot({
    name = "PetclinicVcs"
    url = "https://github.com/spring-projects/spring-petclinic.git"
    branch = "main"
    authMethod = password {
        userName = "vaano94"
        password = "credentialsJSON:c57c4a0d-1b85-4ce0-b4b6-d2dc8b51e099"
    }
})
