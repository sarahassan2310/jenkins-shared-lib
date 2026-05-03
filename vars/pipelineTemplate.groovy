def call(Map config = [:]) {

    pipeline {

        agent any

        tools {
            maven 'maven-iti'
            jdk 'java17'
        }

        options {
            timestamps()
            ansiColor('xterm')
            timeout(time: 20, unit: 'MINUTES')
        }

        environment {

            IMAGE_NAME = "${config.imageName}"
            IMAGE_TAG  = "${config.imageTag ?: 'latest'}"
            PORT       = "${config.port ?: '8080'}"
        }

        stages {

            stage('Clone') {
                steps {
                    git branch: "${config.branch ?: 'main'}",
                        url: config.repo,
                        credentialsId: 'github-token'
                }
            }

            stage('Compile') {
                steps {
                    sh "mvn clean compile"
                }
            }

            stage('Test') {
                steps {
                    sh "mvn test -Dspring.profiles.active=test"
                }
            }

            stage('Package') {
                steps {
                    sh "mvn clean package -DskipTests"
                }
            }

            stage('Docker Build & Push') {
                steps {
                    dockerBuildAndPush()
                }
            }

            stage('Deploy') {
                steps {
                    sh '''
                    docker rm -f ${IMAGE_NAME} || true

                    docker run -d \
                    --name ${IMAGE_NAME} \
                    -p ${PORT}:8080 \
                    $DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG}
                    '''
                }
            }
        }
    }
}
