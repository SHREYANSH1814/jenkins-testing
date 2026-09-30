// Container image scans — three sequential stages, each a different source:
//   1. Registry image        — scan an image pulled from a public/registry source.
//   2. Private registry image — scan an image pulled from a PRIVATE registry
//                               (withCredentials binds the registry secrets to
//                               IMAGE_ACCESS_ID/TOKEN).
//                               Ported from sto-testing-repo/.github/workflows/
//                               aqua-trivy-orchestration.yml.
//   3. Local build & scan     — build/tag an image on the host Docker daemon, then
//                               scan it with imageType 'local_image'.
//                               Ported from sto-testing-repo/.github/workflows/
//                               aqua-trivy-local-image.yml.
//
// All scanning goes through lib/StoScan.groovy (loaded once, reused per stage).
// Do not inline the docker run here — StoScan owns that.
//
// Pipeline from SCM → this repo; script path jobs/container-scan.Jenkinsfile
// (uncheck "Lightweight checkout"). Docker Desktop/Colima must be running
// (stage 3 uses the host Docker daemon to read the locally built image).
//
// Jenkins credentials required (Manage Jenkins → Credentials, "Secret text"):
//   harness-pat-token-sto-lab                              (HARNESS_TOKEN, all)
//   PRIVATE_DOCKER_ACCESS_ID, PRIVATE_DOCKER_ACCESS_TOKEN  (stage 2 private image)

def sto

pipeline {
    agent any

    environment {
        PATH               = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
        // Local-image scan (stage "Local build & scan") needs the host Docker
        // socket. /var/run/docker.sock is a dangling symlink here; point at the
        // real Colima socket so StoScan's preflight and bind-mount resolve.
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

      
        stage('Private registry image scan') {
            steps {
                script {
                    
                    withCredentials([
                        string(credentialsId: 'PRIVATE_DOCKER_ACCESS_ID',    variable: 'IMAGE_ACCESS_ID'),
                        string(credentialsId: 'PRIVATE_DOCKER_ACCESS_TOKEN', variable: 'IMAGE_ACCESS_TOKEN'),
                    ]) {
                        sto.run([
                            scanner       : 'aqua_trivy',
                            scanMode      : 'orchestration',
                            scanConfig    : 'default',
                            targetType    : 'container',
                            imageName     : 'nirocr/nodegoat',
                            imageTag      : 'latest',
                            targetName    : 'shreyansh/privatecontainer1',
                            targetVariant : 'latest',
                            imageAccessEnv: true,
                            outputFile    : 'scan-output-aquatrivy-private.env',
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
                    // Mirrors aqua-trivy-local-image.yml (docker build → local_image).
                    def localTag = "build-${env.BUILD_NUMBER}"
                    sh """
                        set -e
                        docker pull node:14
                        docker tag node:14 sg123:${localTag}
                    """

                    // imageType 'local_image' → StoScan uses the host Docker daemon
                    // (docker-in-docker, mounts /var/run/docker.sock). No registry pull.
                    sto.run([
                        scanner       : 'aqua_trivy',
                        scanMode      : 'orchestration',
                        scanConfig    : 'default',
                        targetType    : 'container',
                        imageName     : 'sg123',
                        imageTag      : localTag,
                        imageType     : 'local_image',
                        targetName    : 'sg123',
                        targetVariant : localTag,
                        creds         : [],
                        outputFile    : 'scan-output-aquatrivy-local.env',
                        showSummary   : true,
                    ])
                }
            }
        }

          stage('Registry image scan') {
            steps {
                script {
                    // Public/registry image → no image credentials.
                    sto.run([
                        scanner       : 'aqua_trivy',
                        scanMode      : 'orchestration',
                        scanConfig    : 'default',
                        targetType    : 'container',
                        imageName     : 'nginx',
                        imageTag      : 'latest',
                        imageDomain   : 'docker.io',
                        imageType     : 'docker_v2',
                        targetName    : 'nginx',
                        targetVariant : 'latest',
                        creds         : [],
                        outputFile    : 'scan-output-aquatrivy-registry.env',
                        showSummary   : true,
                    ])
                }
            }
        }

    }
}
