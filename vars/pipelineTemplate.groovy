def call(Map config = [:]) {
//
    pipeline {

        agent any

        tools {
            maven 'maven-iti'
            jdk 'java17'
        }

        environment {
            IMAGE_NAME = "${config.imageName}"
            IMAGE_TAG  = "${config.imageTag ?: 'latest'}"
        }

        stages {

            // CLONE
            stage('Clone') {
                steps {
                    git branch: "${config.branch ?: 'main'}",
                        url: config.repo,
                        credentialsId: 'github-token'
                }
            }

            // 2 - CONFIG 
            stage('Config') {
                steps {
                    script {

                        env.APP_PORT = config.port ?: '8080'

                    }
                }
            }

            // 3 - COMPILE
            stage('Compile') {
                steps {
                    sh "mvn clean compile"
                }
            }

            // 4 - TEST
            stage('Test') {
                steps {
                    sh "mvn test -Dspring.profiles.active=test -B"
                }
            }

            // 5 - PACKAGE
            stage('Package') {
                steps {
                    sh "mvn clean package -DskipTests"
                }
            }

            // 6 - DOCKER LOGIN
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

            // 7 - BUILD IMAGE
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

            // 8 - PUSH IMAGE
            stage('Push Docker Image') {
                steps {

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {

                        sh '''
                        docker push $DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG}
                        '''
                    }
                }
            }

            // 9 - DEPLOY
            stage('Deploy') {
                steps {

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {

                        sh '''
                        docker rm -f ${IMAGE_NAME} || true

                        docker run -d \
                        --name ${IMAGE_NAME} \
                        -p ${APP_PORT}:8080 \
                        $DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG}
                        '''
                    }
                }
            }
        }
    }
}
