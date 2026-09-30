// DAST scans — one pipeline, all DAST scanners run in PARALLEL.
// Ported from sto-testing-repo/.github/workflows/zap-orchestration.yml
// and nikto-orchestration.yml. Both scan a running instance (target-type: instance).
//
// No repo checkout: these scan a live endpoint, not source.
//
// Pipeline from SCM → this repo; script path jobs/dast-scan.Jenkinsfile
// (uncheck "Lightweight checkout").
//
// Jenkins credentials required (Manage Jenkins → Credentials, "Secret text"):
//   harness-pat-token-sto-lab   (bound as HARNESS_TOKEN for every branch)

pipeline {
    agent any

    environment {
        PATH               = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
        HARNESS_DOMAIN     = 'https://sto.harness.io'
        HARNESS_ACCOUNT_ID = 'YTg1ZTIzODYtZGU3Yy00Mm'
        HARNESS_ORG_ID     = 'jenkinstest'
        HARNESS_PROJECT_ID = 'jenkins'
    }

    stages {
        stage('DAST scans') {
            steps {
                script {
                    def sto = load 'lib/StoScan.groovy'
                    sto.init(this)

                    parallel(
                        'zap': {
                            sto.run([
                                scanner        : 'zap',
                                scanMode       : 'orchestration',
                                scanConfig     : 'default',
                                targetType     : 'instance',
                                instanceDomain : 'google-gruyere.appspot.com',
                                instanceProtocol: 'http',
                                instancePath   : '/435433807708005776736997120918226188991',
                                creds          : [],
                                outputFile     : 'scan-output-zap.env',
                                showSummary    : true,
                            ])
                        },
                        'nikto': {
                            sto.run([
                                scanner        : 'nikto',
                                scanMode       : 'orchestration',
                                scanConfig     : 'default',
                                targetType     : 'instance',
                                instanceDomain : 'httpforever.com',
                                instanceProtocol: 'http',
                                instancePort   : '443',
                                creds          : [],
                                outputFile     : 'scan-output-nikto.env',
                                showSummary    : true,
                            ])
                        },
                    )
                }
            }
        }
    }
}
