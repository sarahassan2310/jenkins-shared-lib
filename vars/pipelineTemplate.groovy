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
                    sh "sh "mvn clean package -DskipTests""
                }
            }

        
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

        
            stage('Build Docker Image') {
                steps {
                    sh '''
                    docker build -t $DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG} .
                    '''
                }
            }

            stage('Push Image') {
                steps {
                    sh '''
                    docker push $DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG}
                    '''
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
