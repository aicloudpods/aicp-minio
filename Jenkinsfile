pipeline {
  agent {
    kubernetes {
      label 'aicp-minio-build'
      defaultContainer 'jnlp'
      yaml """
apiVersion: v1
kind: Pod
metadata:
labels:
  component: ci
spec:
  # Use service account that can deploy to all namespaces
  # serviceAccountName: cd-jenkins
  containers:
  - name: maven
    image: maven:latest
    command:
    - cat
    tty: true
    volumeMounts:
      - mountPath: "/root/.m2"
        name: m2
  - name: docker
    image: docker:latest
    command:
    - cat
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  volumes:
    - name: docker-sock
      hostPath:
        path: /var/run/docker.sock
    - name: m2
      emptyDir: {}
"""
}
   }
  stages {
    stage('Build') {
      steps {
        container('maven') {
          sh 'mvn clean install'
        }
      }
    }
    stage('Push') {
      steps {
        container('docker') {
            withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'pass', usernameVariable: 'user')]) {
                sh """
                 docker login --username $user --password $pass
                 docker build -t aicp/aicp-minio:$BUILD_NUMBER .
                 docker push aicp/aicp-minio:$BUILD_NUMBER
                """
            }
        }
      }
    }
  }
}
