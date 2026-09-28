// What non-Harness CI onboarding should emit for Jenkins (GHA-aligned).
// One pipeline, one stage per scanner — same as multiple `uses: harness/github-actions/sto` steps.
//
// Scanner tokens are per-stage (like GHA step `with:`), not pipeline-wide, so Wiz
// credentials do not leak into Bandit.
//
// Pipeline from SCM → this repo, script path examples/onboarding.Jenkinsfile
// (uncheck Lightweight checkout). Later: @Library('harness-sto@v1') _ + stoScan(...).

pipeline {
    agent any

    environment {
        HARNESS_DOMAIN     = 'https://sto.harness.io'
        HARNESS_ACCOUNT_ID = 'YTg1ZTIzODYtZGU3Yy00Mm'
        HARNESS_ORG_ID     = 'jenkinstest'
        HARNESS_PROJECT_ID = 'jenkins'
        HARNESS_TOKEN      = credentials('harness-pat-token-sto-lab')
    }

    stages {
        // Clone the target repo into a subdir of the Jenkins workspace.
        // The scanner reads it via the bind mount, not via its own git clone.
        stage('Checkout NodeGoat') {
            steps {
                sh '''
                    rm -rf nodegoat
                    git clone --depth 1 https://github.com/owasp/nodegoat.git nodegoat
                '''
            }
        }

        stage('Bandit') {
            steps {
                // Show what WORKSPACE points to and what's inside it.
                echo "WORKSPACE = ${env.WORKSPACE}"
                sh '''
                    echo "===== WORKSPACE path ====="
                    echo "$WORKSPACE"
                    echo "===== top-level contents ====="
                    ls -la "$WORKSPACE"
                    echo "===== nodegoat clone contents ====="
                    ls -la "$WORKSPACE/nodegoat"
                '''
                script {
                    def sto = load 'lib/StoScan.groovy'
                    sto.init(this)
                    sto.run([
                        scanner   : 'bandit',
                        scanMode  : 'orchestration',
                        targetType: 'repository',
                        workspace : '.',
                        // Point at the nodegoat clone so /harness IS its clone root
                        // (has .git) — required for the plugin's git auto-detect.
                        sourceRoot: "${env.WORKSPACE}/nodegoat",
                    ])
                }
            }
        }
    }
}