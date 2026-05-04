def call(Map config = [:]) {

    pipeline {

        agent any

        environment {
            GIT_REPO   = config.gitRepo
            GIT_BRANCH = config.gitBranch ?: 'main'
        }

        stages {

            stage('Clone Repository') {
                steps {
                    script {

                        echo "Cloning repository..."
                        echo "Repo: ${GIT_REPO}"
                        echo "Branch: ${GIT_BRANCH}"

                        git branch: GIT_BRANCH,
                            url: GIT_REPO
                    }
                }
            }

        }
    }
}
