pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                checkout scm
                sh 'mvn clean install'
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            }
        }
    }
}
