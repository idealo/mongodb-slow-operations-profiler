pipeline {
        agent { label 'slave' }

        tools { maven 'maven 3.6.3' }

        environment {
            ECR_BASE = '054331651301.dkr.ecr.us-east-1.amazonaws.com'
            IMAGE_TAG = "dev-${BUILD_NUMBER}"
            IMAGE_BASE = "${ECR_BASE}/com-wss-index/mongo-profiler"
        }
  
        stages{
            stage ('clean and clone') {
                steps {
                    cleanWs()
                    sh "mkdir tmp && cd tmp"
                    git branch: 'master',
                        url: 'https://github.com/whitesource/mongodb-slow-operations-profiler.git'
                }
            }
            stage('Build') {
                steps {
                    sh "cd tmp && mvn package"
                }
            }           
            stage('Deploy') {
                steps {
                    script {                      
                          sh """
                            echo 'building image ${IMAGE_BASE}'                            
                            docker build -t ${IMAGE_BASE} .
                            echo 'pushing image ${IMAGE_BASE}:${IMAGE_TAG}'
                            docker push ${IMAGE_BASE}:${IMAGE_TAG}                                               
                          """
                    }
                }
            }
        }
    }
}
