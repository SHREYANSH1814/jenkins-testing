// Wiz repository scans — one pipeline, two sequential stages:
//   1. Wiz SAST — clones OWASP NodeGoat and scans the source (scan-config: default).
//   2. Wiz IaC  — clones TerraGoat (vulnerable Terraform) and scans it (scan-config: iac).
//
// Modeled on jobs/sast-scan.Jenkinsfile / jobs/iac-scan.Jenkinsfile; scanner
// conventions from jobs/wiz-container-scan.Jenkinsfile. All scanning goes through
// lib/StoScan.groovy (loaded once, reused per stage). Scanner auth is bound
// explicitly with withCredentials → SCANNER_ACCESS_ID/TOKEN.
//
// Pipeline from SCM → this repo; script path jobs/wiz-scan.Jenkinsfile
// (uncheck "Lightweight checkout" so lib/StoScan.groovy is on disk).
//
// Jenkins credentials required (Manage Jenkins → Credentials, "Secret text"):
//   harness-pat-token-sto-lab        (HARNESS_TOKEN)
//   WIZ_ACCESS_ID, WIZ_ACCESS_TOKEN  (wiz scanner auth)

def sto

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
        stage('Load STO library') {
            steps {
                script {
                    sto = load 'lib/StoScan.groovy'
                    sto.init(this)
                }
            }
        }

        stage('Wiz SAST scan') {
            steps {
                script {
                    // SAST target: OWASP NodeGoat (intentionally-vulnerable Node.js app).
                    sh '''
                        rm -rf sast-repo
                        git clone https://github.com/OWASP/NodeGoat sast-repo
                    '''
                    withCredentials([
                        string(credentialsId: 'WIZ_ACCESS_ID',    variable: 'SCANNER_ACCESS_ID'),
                        string(credentialsId: 'WIZ_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN'),
                    ]) {
                        sto.run([
                            scanner    : 'wiz',
                            scanMode   : 'orchestration',
                            scanConfig : 'default',
                            targetType : 'repository',
                            workspace  : '.',
                            sourceRoot : "${env.WORKSPACE}/sast-repo",
                            outputFile : 'scan-output-wiz-sast.env',
                            showSummary: true,
                        ])
                    }
                }
            }
        }

        stage('Wiz IaC scan') {
            steps {
                script {
                    // IaC target: TerraGoat is Terraform IaC throughout, so scan the root.
                    sh '''
                        rm -rf iac-repo
                        git clone https://github.com/bridgecrewio/terragoat iac-repo
                    '''
                    withCredentials([
                        string(credentialsId: 'WIZ_ACCESS_ID',    variable: 'SCANNER_ACCESS_ID'),
                        string(credentialsId: 'WIZ_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN'),
                    ]) {
                        sto.run([
                            scanner    : 'wiz',
                            scanMode   : 'orchestration',
                            scanConfig : 'iac',
                            targetType : 'repository',
                            workspace  : '.',
                            sourceRoot : "${env.WORKSPACE}/iac-repo",
                            outputFile : 'scan-output-wiz-iac.env',
                            showSummary: true,
                        ])
                    }
                }
            }
        }
    }
}
