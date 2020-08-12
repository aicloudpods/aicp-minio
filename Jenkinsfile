pipeline {

  agent {
    kubernetes {
      label 'aicp-minio-build'
      yamlFile 'JenkinsAgentPod.yaml'
      defaultContainer 'jnlp'
    }
  }

  stages {

    stage ('Init') {
        steps {
            echo 'Pulling...' + env.BRANCH_NAME
            checkout scm
        }
     }

    stage('Build the code') {
      steps {
        container('maven') {
          echo "Running the build..."
          sh 'mvn clean install'
        }
      }
    }


	stage('Push ') {
      steps {
        container('docker') {
            withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'pass', usernameVariable: 'user')]) {
                echo "Building container image and pushing to dockerhub..."
                sh """
                 docker login --username $user --password $pass
                 docker build -t aicp/aicp-minio:$BUILD_NUMBER .
                 docker push aicp/aicp-minio:$BUILD_NUMBER
                """
            }
        }
      }
    }

    stage('Deploy helm chart') {
       when {
         branch 'master'
       }
       steps {
            container('terraform') {
                 echo "Helm install coming soon!!.."
            }
       }
     }

   }

}
