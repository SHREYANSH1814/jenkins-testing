// Ingestion scans — one pipeline, all scanners run in PARALLEL in INGESTION mode.
// Ingestion uploads a pre-generated results file (the scanner's native output)
// to STO instead of running the scanner. Ported from the ingestion result files
// shipped in sto-testing-repo (anchore-ingestion.yml is the only GHA ingestion
// workflow; the other result files live at the repo paths referenced below).
//
// Ingestion mode specifics (enforced by StoScan.groovy):
//   - SCAN_MODE=ingestion requires ingestionFile (resolved under sourceRoot).
//   - Detection is always MANUAL, so targetName + targetVariant are required.
//   - No scanner credentials needed — nothing calls the scanner's API.
//
// Pipeline from SCM → this repo; script path jobs/ingestion-scan.Jenkinsfile
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
        // Clone once so the ingestion result files are on disk. sourceRoot points
        // at the clone; each branch's ingestionFile is a path relative to it.
        stage('Checkout target repo') {
            steps {
                sh '''
                    rm -rf target-repo
                    git clone https://github.com/SHREYANSH1814/sto-testing-repo target-repo
                '''
            }
        }

        stage('Ingestion scans') {
            steps {
                script {
                    def sto = load 'lib/StoScan.groovy'
                    sto.init(this)
                    def src = "${env.WORKSPACE}/target-repo"

                    // scanner → [ingestion file (relative to repo root), target type]
                    def jobs = [
                        'anchore'      : ['ghas-ingestion/anchore-ingestion.json', 'container'],
                        'aqua_trivy'   : ['aquatrivy-ingestion.json',              'container'],
                        'prismacloud'  : ['prisma_cloud.json',                     'container'],
                        'snyk'         : ['snyk.json',                             'repository'],
                        'semgrep'      : ['semgrep/issues.json',                   'repository'],
                        'sonarqube'    : ['sonarqube/ingestion1.json',            'repository'],
                        'gitleaks'     : ['gitleaks/ingestion.json',              'repository'],
                        'checkmarx'    : ['checkmarx/checkmarx.json',             'repository'],
                        'checkmarxone' : ['checkmarx-one/ingestion1.json',        'repository'],
                        'cortex'       : ['cortex/ingestion1.json',               'repository'],
                        'wiz'          : ['wiz.json',                              'repository'],
                    ]

                    def branches = [:]
                    jobs.each { scanner, spec ->
                        def (ingestFile, targetType) = spec
                        branches[scanner] = {
                            sto.run([
                                scanner      : scanner,
                                scanMode     : 'ingestion',
                                targetType   : targetType,
                                ingestionFile: ingestFile,
                                sourceRoot   : src,
                                creds        : [],
                                targetName   : "shreyansh/${scanner}-ingest",
                                targetVariant: 'main',
                                outputFile   : "scan-output-${scanner}-ingest.env",
                                showSummary  : true,
                            ])
                        }
                    }

                    parallel(branches)
                }
            }
        }
    }
}
