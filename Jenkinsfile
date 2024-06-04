pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials-id')
        DOCKERHUB_REPO = 'qpfriday/mytrophy'
        DOCKER_IMAGE_TAG = "${DOCKERHUB_REPO}:${BUILD_NUMBER}"
        SERVER_CREDENTIALS = credentials('server-ssh-credentials-id')
        SERVER_IP = '34.64.52.132' // 서버 IP 주소
    }

    stages {

        stage('git clone') {
            steps {
                git branch: 'dev', credentialsId: 'gitlab-credentials-id', url: 'https://kdt-gitlab.elice.io/cloud_track/class_02/web_project3/team04/team4-back.git'
            }
        }

        stage('Build') {
            steps {
                script {
                    sh 'chmod +x gradlew'
                    sh './gradlew clean build'
                }
            }
        }

        stage('Verify Docker Installation') {
            steps {
                script {
                    sh 'docker --version'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t ${DOCKER_IMAGE_TAG} ."
                }
            }
        }

        stage('Docker Login'){
            steps{
                script {
                    sh 'echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin'
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/') {
                        sh "docker push ${DOCKER_IMAGE_TAG}"
                    }
                }
            }
        }

        stage('SSH to Server') {
            steps {
                script {
                    sshagent(['server-ssh-credentials-id']) {
                        // SSH를 통해 서버에 접속
                        sh "ssh ${SERVER_CREDENTIALS_USR}@${SERVER_IP}"
                    }
                }
            }
        }

        stage('Pull Docker Image on Server') {
            steps {
                script {
                    sshagent(['server-ssh-credentials-id']) {
                        // 서버에서 도커 이미지 풀링
                        sh "ssh ${SERVER_CREDENTIALS_USR}@${SERVER_IP} 'docker pull ${DOCKER_IMAGE_TAG}'"
                    }
                }
            }
        }

        stage('Run Docker Container on Server') {
            steps {
                script {
                    sshagent(['server-ssh-credentials-id']) {
                        // 서버에서 기존 서비스 종료 및 새 컨테이너 실행
                        sh """
                        ssh ${SERVER_CREDENTIALS_USR}@${SERVER_IP} '
                        if [ \$(docker ps -q -f name=mytrophy-service) ]; then
                            docker stop mytrophy-service
                            docker rm mytrophy-service
                        fi
                        docker run -d --name mytrophy-service -p 8080:8080 ${DOCKER_IMAGE_TAG}'
                        """
                    }
                }
            }
        }
    }
}
