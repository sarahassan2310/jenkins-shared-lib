
def call(Map config = [:]) {

    pipeline {

        agent any

        environment {
            GIT_REPO   = "${config.gitRepo}"
            GIT_BRANCH = "${config.gitBranch ?: 'main'}"
        }

        stages {

            stage('Clone Repository') {
                steps {

                    echo "Cloning repository..."

                    git branch: "${GIT_BRANCH}",
                        url: "${GIT_REPO}"

                    echo "Repository cloned successfully"
                }
            }

        }

    }

}
