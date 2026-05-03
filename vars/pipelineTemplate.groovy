def call(Map config = [:]) {

    pipeline {

        agent any

        tools {
            maven 'maven-iti'
            jdk 'java17'
        }

        environment {

            IMAGE_NAME = "${config.imageName}"
            IMAGE_TAG  = "${config.imageTag ?: 'latest'}"
            PORT       = "${config.port ?: '8080'}"
        }

        stages {

            // 1 - CLONE
            stage('Clone') {
                steps {
                    git branch: "${config.branch ?: 'main'}",
                        url: config.repo,
                        credentialsId: 'github-token'
                }
            }

            // 2 - COMPILE
            stage('Compile') {
                steps {
                    sh "mvn clean compile"
                }
            }

            // 3 - TEST
            stage('Test') {
                steps {
                    sh "mvn test -Dspring.profiles.active=test"
                }
            }

            // 4 - PACKAGE
            stage('Package') {
                steps {
                    sh "mvn clean package -DskipTests"
                }
            }

            // 5 - DOCKER LOGIN
            stage('Docker Login') {

                steps {

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {

                        sh '''
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        '''
                    }
                }
            }

            // 6 - BUILD IMAGE
            stage('Build Docker Image') {
                steps {

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {

                        sh '''
                        docker build -t $DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG} .
                        '''
                    }
                }
            }

            // 7 - PUSH IMAGE
            stage('Push Docker Image') {
                steps {

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {

                        sh '''
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        '''
                    }
                }
            }

            // 8 - DEPLOY
            stage('Deploy') {
                steps {

                    sh '''
                    docker rm -f ${IMAGE_NAME} || true

                    docker run -d \
                    --name ${IMAGE_NAME} \
                    -p ${PORT}:8080 \
                    yourdockerhub/${IMAGE_NAME}:${IMAGE_TAG}
                    '''
                }
            }
        }
    }
}
