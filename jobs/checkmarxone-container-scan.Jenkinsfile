// Checkmarx One container image scans — three sequential stages (local build LAST):
//   1. Registry image        — public image (docker.io/nginx), no image creds.
//   2. Private registry image — private image; withCredentials binds
//                               PRIVATE_DOCKER_ACCESS_ID/TOKEN → IMAGE_ACCESS_ID/TOKEN.
//   3. Local build & scan     — build/tag on the host Docker daemon, then scan it
//                               with imageType 'local_image'.
//
// Modeled on the sto-testing-repo container workflows. NOTE: Checkmarx One is
// primarily a SAST/code scanner; confirm your Checkmarx One setup supports container
// target-type before relying on these stages.
// All scanning goes through lib/StoScan.groovy (loaded once, reused per stage).
// Scanner auth is bound explicitly with withCredentials → SCANNER_ACCESS_ID/TOKEN.
//
// Pipeline from SCM → this repo; script path jobs/checkmarxone-container-scan.Jenkinsfile
// (uncheck "Lightweight checkout"). Docker Desktop/Colima must be running.
//
// Jenkins credentials required (Manage Jenkins → Credentials, "Secret text"):
//   harness-pat-token-sto-lab                                (HARNESS_TOKEN, all)
//   PRIVATE_DOCKER_ACCESS_ID, PRIVATE_DOCKER_ACCESS_TOKEN    (stage 2 private image)
//   CHECKMARX_ONE_ACCESS_ID, CHECKMARX_ONE_ACCESS_TOKEN, CHECKMARX_ONE_DOMAIN  (checkmarx-one scanner auth)
//   (CHECKMARX_ONE_DOMAIN = Checkmarx One tenant/base URL; the scanner cannot connect without it.)

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
                    // Public image → no image credentials. Checkmarx One auth bound explicitly.
                    withCredentials([
                        string(credentialsId: 'CHECKMARX_ONE_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                    ]) {
                        sto.run([
                            scanner       : 'checkmarxone',
                            scanMode      : 'orchestration',
                            scanConfig    : 'default',
                            targetType    : 'container',
                            imageName     : 'nginx',
                            imageTag      : 'latest',
                            imageDomain   : 'docker.io',
                            imageType     : 'docker_v2',
                            targetName    : 'nginx',
                            targetVariant : 'latest',
                            outputFile    : 'scan-output-checkmarxone-registry.env',
                            showSummary   : true,
                        ])
                    }
                }
            }
        }

        stage('Private registry image scan') {
            steps {
                script {
                    // Private image secret + checkmarx-one scanner auth, both bound explicitly.
                    withCredentials([
                        string(credentialsId: 'PRIVATE_DOCKER_ACCESS_ID',    variable: 'IMAGE_ACCESS_ID'),
                        string(credentialsId: 'PRIVATE_DOCKER_ACCESS_TOKEN', variable: 'IMAGE_ACCESS_TOKEN'),
                        string(credentialsId: 'CHECKMARX_ONE_ACCESS_ID',     variable: 'SCANNER_ACCESS_ID'),
                        string(credentialsId: 'CHECKMARX_ONE_ACCESS_TOKEN',  variable: 'SCANNER_ACCESS_TOKEN'),
                        string(credentialsId: 'CHECKMARX_ONE_DOMAIN',        variable: 'SCANNER_DOMAIN'),
                    ]) {
                        sto.run([
                            scanner       : 'checkmarxone',
                            scanMode      : 'orchestration',
                            scanConfig    : 'default',
                            targetType    : 'container',
                            imageName     : 'nirocr/nodegoat',
                            imageTag      : 'latest',
                            imageType     : 'docker_v2',
                            targetName    : 'shreyansh/checkmarxonecontainer',
                            targetVariant : 'latest',
                            imageAccessEnv: true,
                            outputFile    : 'scan-output-checkmarxone-private.env',
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

                    withCredentials([
                        string(credentialsId: 'CHECKMARX_ONE_ACCESS_ID',    variable: 'SCANNER_ACCESS_ID'),
                        string(credentialsId: 'CHECKMARX_ONE_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN'),
                        string(credentialsId: 'CHECKMARX_ONE_DOMAIN',       variable: 'SCANNER_DOMAIN'),
                    ]) {
                        sto.run([
                            scanner       : 'checkmarxone',
                            scanMode      : 'orchestration',
                            scanConfig    : 'default',
                            targetType    : 'container',
                            imageName     : 'sg123',
                            imageTag      : localTag,
                            imageType     : 'local_image',
                            targetName    : 'sg123',
                            targetVariant : localTag,
                            outputFile    : 'scan-output-checkmarxone-local.env',
                            showSummary   : true,
                        ])
                    }
                }
            }
        }
    }
}
