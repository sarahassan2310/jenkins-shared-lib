def call(Map config = [:]) {

    pipeline {

        agent any

        stages {

            stage('Clone Repository') {
                steps {
                    script {

                        def repo = config.gitRepo
                        def branch = config.gitBranch ?: 'main'

                        echo "Cloning repository..."
                        echo "Repo: ${repo}"
                        echo "Branch: ${branch}"

                        git branch: branch,
                            url: repo
                    }
                }
            }

        }
    }
}
