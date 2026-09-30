// Prisma Cloud (Twistlock) container image scans — three sequential stages
// (local build LAST):
//   1. Registry image        — public image (docker.io/nginx), no image creds.
//   2. Private registry image — private image; withCredentials binds
//                               PRIVATE_DOCKER_ACCESS_ID/TOKEN → IMAGE_ACCESS_ID/TOKEN.
//   3. Local build & scan     — build/tag on the host Docker daemon, then scan it
//                               with imageType 'local_image'.
//
// Ported from sto-testing-repo/.github/workflows/prisma-cloud-container.yml.
// All scanning goes through lib/StoScan.groovy (loaded once, reused per stage).
// Scanner auth is bound explicitly with withCredentials → SCANNER_ACCESS_ID/TOKEN.
//
// Note: Prisma Cloud container orchestration runs on the HOST Docker daemon
// automatically (StoScan autoHostDocker for twistlock), so every stage — not just
// the local build — needs the Docker socket. DOCKER_HOST below provides it.
//
// Pipeline from SCM → this repo; script path jobs/prismacloud-container-scan.Jenkinsfile
// (uncheck "Lightweight checkout"). Docker Desktop/Colima must be running.
//
// Jenkins credentials required (Manage Jenkins → Credentials, "Secret text"):
//   harness-pat-token-sto-lab                              (HARNESS_TOKEN, all)
//   PRIVATE_DOCKER_ACCESS_ID, PRIVATE_DOCKER_ACCESS_TOKEN  (stage 2 private image)
//   PRISMA_ACCESS_ID, PRISMA_ACCESS_TOKEN, PRISMA_DOMAIN   (prisma scanner auth)
//   (PRISMA_DOMAIN = Prisma Console URL; Twistlock cannot connect without it.)

def sto

pipeline {
    agent any

    environment {
        PATH               = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
        // Prisma runs on the host Docker daemon for all container stages; point at
        // the real Colima socket (/var/run/docker.sock is a dangling symlink here).
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
                    // Public image → no image credentials. Prisma auth bound explicitly.
                    withCredentials([
                        string(credentialsId: 'PRISMA_ACCESS_ID',    variable: 'SCANNER_ACCESS_ID'),
                        string(credentialsId: 'PRISMA_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN'),
                        string(credentialsId: 'PRISMA_DOMAIN',       variable: 'SCANNER_DOMAIN'),
                    ]) {
                        sto.run([
                            scanner       : 'prismacloud',
                            scanMode      : 'orchestration',
                            scanConfig    : 'default',
                            targetType    : 'container',
                            imageName     : 'nginx',
                            imageTag      : 'latest',
                            imageDomain   : 'docker.io',
                            imageType     : 'docker_v2',
                            targetName    : 'nginx',
                            targetVariant : 'latest',
                            outputFile    : 'scan-output-prismacloud-registry.env',
                            showSummary   : true,
                        ])
                    }
                }
            }
        }

        stage('Private registry image scan') {
            steps {
                script {
                    // Private image secret + prisma scanner auth, both bound explicitly.
                    withCredentials([
                        string(credentialsId: 'PRIVATE_DOCKER_ACCESS_ID',    variable: 'IMAGE_ACCESS_ID'),
                        string(credentialsId: 'PRIVATE_DOCKER_ACCESS_TOKEN', variable: 'IMAGE_ACCESS_TOKEN'),
                        string(credentialsId: 'PRISMA_ACCESS_ID',            variable: 'SCANNER_ACCESS_ID'),
                        string(credentialsId: 'PRISMA_ACCESS_TOKEN',         variable: 'SCANNER_ACCESS_TOKEN'),
                        string(credentialsId: 'PRISMA_DOMAIN',               variable: 'SCANNER_DOMAIN'),
                    ]) {
                        sto.run([
                            scanner       : 'prismacloud',
                            scanMode      : 'orchestration',
                            scanConfig    : 'default',
                            targetType    : 'container',
                            imageName     : 'nirocr/nodegoat',
                            imageTag      : 'latest',
                            imageType     : 'docker_v2',
                            targetName    : 'shreyansh/prismacontainer',
                            targetVariant : 'latest',
                            imageAccessEnv: true,
                            outputFile    : 'scan-output-prismacloud-private.env',
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
                        string(credentialsId: 'PRISMA_ACCESS_ID',    variable: 'SCANNER_ACCESS_ID'),
                        string(credentialsId: 'PRISMA_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN'),
                        string(credentialsId: 'PRISMA_DOMAIN',       variable: 'SCANNER_DOMAIN'),
                    ]) {
                        sto.run([
                            scanner       : 'prismacloud',
                            scanMode      : 'orchestration',
                            scanConfig    : 'default',
                            targetType    : 'container',
                            imageName     : 'sg123',
                            imageTag      : localTag,
                            imageType     : 'local_image',
                            targetName    : 'sg123',
                            targetVariant : localTag,
                            outputFile    : 'scan-output-prismacloud-local.env',
                            showSummary   : true,
                        ])
                    }
                }
            }
        }
    }
}
