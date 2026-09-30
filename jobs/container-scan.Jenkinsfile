// Container image scans — one pipeline, all container scanners run in PARALLEL.
// Ported from sto-testing-repo/.github/workflows/*-container / *-orchestration.yml
// (aqua_trivy, grype, harness-sca, prisma-cloud, blackduck).
//
// No repo checkout: these scan container images, not source. The scanner reads
// the image from a registry (or the host Docker daemon for local images).
//
// Pipeline from SCM → this repo; script path jobs/container-scan.Jenkinsfile
// (uncheck "Lightweight checkout"). Docker Desktop/Colima must be running.
//
// Jenkins credentials required (Manage Jenkins → Credentials, "Secret text"):
//   harness-pat-token-sto-lab                              (HARNESS_TOKEN, all)
//   PRIVATE_DOCKER_ACCESS_ID, PRIVATE_DOCKER_ACCESS_TOKEN  (aqua_trivy private image)
//   PRISMA_ACCESS_ID, PRISMA_ACCESS_TOKEN, PRISMA_DOMAIN   (prisma-cloud)
//   BLACKDUCK_DOMAIN, BLACKDUCK_ACCESS_ID, BLACKDUCK_ACCESS_TOKEN (blackduck)

pipeline {
    agent any

    environment {
        PATH               = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
        HARNESS_DOMAIN     = 'https://sto.harness.io'
        HARNESS_ACCOUNT_ID = 'YTg1ZTIzODYtZGU3Yy00Mm'
        HARNESS_ORG_ID     = 'jenkinstest'
        HARNESS_PROJECT_ID = 'jenkins'
        HARNESS_TOKEN      = credentials('harness-pat-token-sto-lab')
    }

    stages {
        stage('Container scans') {
            steps {
                script {
                    def sto = load 'lib/StoScan.groovy'
                    sto.init(this)

                    parallel(
                        'aqua-trivy': {
                            // Private registry image → creds:['docker'] binds
                            // PRIVATE_DOCKER_ACCESS_ID/TOKEN → IMAGE_ACCESS_*.
                            sto.run([
                                scanner       : 'aqua_trivy',
                                scanMode      : 'orchestration',
                                scanConfig    : 'default',
                                targetType    : 'container',
                                imageName     : 'johnkday/nodegoat',
                                imageTag      : 'latest',
                                creds         : ['docker'],
                                targetName    : 'johnkday/nodegoat',
                                targetVariant : 'latest',
                                failOnSeverity: 'critical',
                                outputFile    : 'scan-output-aquatrivy.env',
                                showSummary   : true,
                            ])
                        }
                        // 'grype': {
                        //     // Public Docker Hub image → no image credentials.
                        //     sto.run([
                        //         scanner    : 'grype',
                        //         scanMode   : 'orchestration',
                        //         scanConfig : 'default',
                        //         targetType : 'container',
                        //         imageName  : 'nginx',
                        //         imageTag   : 'latest',
                        //         imageDomain: 'docker.io',
                        //         imageType  : 'docker_v2',
                        //         creds      : [],
                        //         outputFile : 'scan-output-grype.env',
                        //         showSummary: true,
                        //     ])
                        // },
                        // 'harness-sca': {
                        //     sto.run([
                        //         scanner    : 'harnesssca',
                        //         scanMode   : 'orchestration',
                        //         scanConfig : 'default',
                        //         targetType : 'container',
                        //         imageName  : 'nginx',
                        //         imageTag   : 'latest',
                        //         imageDomain: 'docker.io',
                        //         imageType  : 'docker_v2',
                        //         creds      : [],
                        //         outputFile : 'scan-output-harnesssca.env',
                        //         showSummary: true,
                        //     ])
                        // },
                        // 'prisma-cloud': {
                        //     // creds:['prisma'] binds PRISMA_ACCESS_ID/TOKEN/DOMAIN.
                        //     sto.run([
                        //         scanner    : 'prismacloud',
                        //         scanMode   : 'orchestration',
                        //         scanConfig : 'default',
                        //         targetType : 'container',
                        //         imageName  : 'nginx',
                        //         imageTag   : 'latest',
                        //         imageDomain: 'docker.io',
                        //         imageType  : 'docker_v2',
                        //         creds      : ['prisma'],
                        //         outputFile : 'scan-output-prismacloud.env',
                        //         showSummary: true,
                        //     ])
                        // },
                        // 'blackduck': {
                        //     // creds:['blackduck'] binds BLACKDUCK_DOMAIN/ACCESS_ID/TOKEN.
                        //     sto.run([
                        //         scanner             : 'blackduck',
                        //         scanMode            : 'orchestration',
                        //         scanConfig          : 'default',
                        //         targetType          : 'container',
                        //         imageName           : 'johnkday/nodegoat',
                        //         imageTag            : 'latest',
                        //         imageDomain         : 'docker.io',
                        //         imageType           : 'docker_v2',
                        //         creds               : ['blackduck'],
                        //         scannerAuthType     : 'apiKey',
                        //         scannerApiVersion   : '5.0.2',
                        //         scannerVerifySsl    : 'false',
                        //         scannerProjectName  : 'mudit',
                        //         scannerProjectVersion: 'test',
                        //         toolArgs            : '--blackduck.trust.cert=true',
                        //         logLevel            : 'INFO',
                        //         privileged          : true,
                        //         outputFile          : 'scan-output-blackduck.env',
                        //         showSummary         : true,
                        //     ])
                        // },
                    )
                }
            }
        }
    }
}
