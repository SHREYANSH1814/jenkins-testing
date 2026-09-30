// Harness SCA container image scans — three sequential stages (local build LAST):
//   1. Registry image        — public image (docker.io/nginx), no image creds.
//   2. Private registry image — private image; withCredentials binds
//                               PRIVATE_DOCKER_ACCESS_ID/TOKEN → IMAGE_ACCESS_ID/TOKEN.
//   3. Local build & scan     — build/tag on the host Docker daemon, then scan it
//                               with imageType 'local_image'.
//
// Ported from sto-testing-repo/.github/workflows/harness-sca-container.yml.
// All scanning goes through lib/StoScan.groovy (loaded once, reused per stage).
//
// Pipeline from SCM → this repo; script path jobs/harness-container-scan.Jenkinsfile
// (uncheck "Lightweight checkout"). Docker Desktop/Colima must be running.
//
// Jenkins credentials required (Manage Jenkins → Credentials, "Secret text"):
//   harness-pat-token-sto-lab                              (HARNESS_TOKEN, all)
//   PRIVATE_DOCKER_ACCESS_ID, PRIVATE_DOCKER_ACCESS_TOKEN  (stage 2 private image)
// Harness SCA uses the Harness platform token; no separate scanner API credentials.

def sto

pipeline {
    agent any

    environment {
        PATH               = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
        // Local-image scan needs the host Docker socket; point at the real Colima
        // socket (/var/run/docker.sock is a dangling symlink on this host).
        DOCKER_HOST        = 'unix:///Users/shreyanshgupta/.colima/default/docker.sock'
        HARNESS_DOMAIN     = 'https://sto.harness.io'
        HARNESS_ACCOUNT_ID = 'YTg1ZTIzODYtZGU3Yy00Mm'
        HARNESS_ORG_ID     = 'jenkinstest'
        HARNESS_PROJECT_ID = 'jenkins'
        HARNESS_TOKEN      = credentials('harness-pat-token-sto-lab')
    }

    stages {
        stage('Load STO library') {
            steps {
                script {
                    sto = load 'lib/StoScan.groovy'
                    sto.init(this)
                }
            }
        }

        stage('Registry image scan') {
            steps {
                script {
                    // Public image → no image credentials.
                    sto.run([
                        scanner       : 'harnesssca',
                        scanMode      : 'orchestration',
                        scanConfig    : 'default',
                        targetType    : 'container',
                        imageName     : 'nginx',
                        imageTag      : 'latest',
                        imageDomain   : 'docker.io',
                        imageType     : 'docker_v2',
                        targetName    : 'nginx',
                        targetVariant : 'latest',
                        outputFile    : 'scan-output-harnesssca-registry.env',
                        showSummary   : true,
                    ])
                }
            }
        }

        stage('Private registry image scan') {
            steps {
                script {
                    // Private image secret bound explicitly.
                    withCredentials([
                        string(credentialsId: 'PRIVATE_DOCKER_ACCESS_ID',    variable: 'IMAGE_ACCESS_ID'),
                        string(credentialsId: 'PRIVATE_DOCKER_ACCESS_TOKEN', variable: 'IMAGE_ACCESS_TOKEN'),
                    ]) {
                        sto.run([
                            scanner       : 'harnesssca',
                            scanMode      : 'orchestration',
                            scanConfig    : 'default',
                            targetType    : 'container',
                            imageName     : 'nirocr/nodegoat',
                            imageTag      : 'latest',
                            imageType     : 'docker_v2',
                            targetName    : 'shreyansh/harnessscacontainer',
                            targetVariant : 'latest',
                            imageAccessEnv: true,
                            outputFile    : 'scan-output-harnesssca-private.env',
                            showSummary   : true,
                        ])
                    }
                }
            }
        }

        stage('Local build & scan') {
            steps {
                script {
                    // Build/tag an image on the host Docker daemon, then scan it.
                    def localTag = "build-${env.BUILD_NUMBER}"
                    sh """
                        set -e
                        docker pull node:14
                        docker tag node:14 sg123:${localTag}
                    """

                    sto.run([
                        scanner       : 'harnesssca',
                        scanMode      : 'orchestration',
                        scanConfig    : 'default',
                        targetType    : 'container',
                        imageName     : 'sg123',
                        imageTag      : localTag,
                        imageType     : 'local_image',
                        targetName    : 'sg123',
                        targetVariant : localTag,
                        creds         : [],
                        outputFile    : 'scan-output-harnesssca-local.env',
                        showSummary   : true,
                    ])
                }
            }
        }
    }
}
