// Shared STO docker runner. Product/image maps match:
// - template-library/.harness/<name>Step PRODUCT_NAME + image
// - sto-testing-repo .github/actions/harness-sto/action.yml aliases
// Load from a Jenkinsfile (Pipeline from SCM of this repo):
//   def sto = load 'lib/StoScan.groovy'
//   sto.init(this)
//   sto.runFromParams('bandit', [creds: []])
//
// load() already binds Pipeline `steps` (the DSL). Never assign that name —
// overwriting it with the WorkflowScript makes steps.string ClassCastException.

def init(_ignored = null) {
    return this
}

def failOnSeverityEnv(Map cfg = [:]) {
    def v = cfg.failOnSeverity
    if (!v || v == 'NONE' || v == 'none') {
        return '0'
    }
    return v.toString()
}

// Plugin git auto-detect runs `git -C $WORKSPACE`. That only works if /harness
// is the clone root (has .git). Scan subdir is WORKSPACE=/harness/<rel>, matching
// Harness CI (clone at /harness, template WORKSPACE default /harness).
def pluginWorkspace(String workspaceRel) {
    def w = (workspaceRel ?: '.').toString().trim()
    if (!w || w == '.' || w == './' || w == '/harness') {
        return '/harness'
    }
    w = w.replaceFirst('^\\./', '').replaceFirst('^/+', '')
    // A repo subdirectory named harness/foo is /harness/foo.
    // The directory itself is /harness/harness (clone root stays /harness).
    if (w == 'harness') {
        return '/harness/harness'
    }
    if (w.startsWith('harness/')) {
        return "/${w}"
    }
    return "/harness/${w}"
}

def productName(String scanner) {
    switch (scanner.toLowerCase()) {
        case 'harnesssast':
        case 'harness-sast':
        case 'harness_sast':
            return 'shiftleftsast'
        case 'harnesssca':
        case 'harness-sca':
        case 'harness_sca':
            return 'shiftleftsca'
        case 'qwiet':
            return 'shiftleft'
        case 'blackduck':
            return 'blackduckhub'
        case 'prisma':
        case 'prismacloud':
        case 'prisma_cloud':
        case 'prisma-cloud':
            return 'twistlock'
        case 'osv':
            return 'osv_scanner'
        case 'ghas':
        case 'githubadvancedsecurity':
            return 'github_advanced_security'
        case 'aquasecurity':
            return 'aqua_security'
        case 'aquatrivy':
        case 'aqua_trivy':
        case 'trivy':
            return 'aqua_trivy'
        case 'awsecr':
            return 'aws_ecr'
        case 'awssecurityhub':
            return 'aws_security_hub'
        case 'owaspzap':
            return 'zap'
        case 'checkmarxone':
        case 'checkmarx_one':
            return 'checkmarx-one'
        case 'mend':
            return 'whitesource'
        case 'customingest':
        case 'custom_ingest':
        case 'custom':
            return 'custom'
        case 'cortexcloud':
        case 'cortex_cloud':
        case 'cortex':
            return 'cortex_cloud'
        default:
            return scanner
    }
}

def jobRunnerImage(String scanner, Map cfg = [:]) {
    def lc = scanner.toLowerCase()
    def name
    switch (lc) {
        case 'anchore':                      name = 'anchore-job-runner'; break
        case 'aquasecurity':
        case 'aqua_security':                name = 'aqua-security-job-runner'; break
        case 'awsecr':
        case 'aws_ecr':                      name = 'aws-ecr-job-runner'; break
        case 'awssecurityhub':
        case 'aws_security_hub':             name = 'aws-security-hub-job-runner'; break
        case 'aqua_trivy':
        case 'aquatrivy':
        case 'trivy':                        name = 'aqua-trivy-job-runner'; break
        case 'bandit':                       name = 'bandit-job-runner'; break
        case 'blackduck':                    name = 'blackduckhub-job-runner'; break
        case 'brakeman':                     name = 'brakeman-job-runner'; break
        case 'burp':                         name = 'burp-job-runner'; break
        case 'checkmarxone':
        case 'checkmarx_one':                name = 'checkmarx-one-job-runner'; break
        case 'checkmarx':                    name = 'checkmarx-job-runner'; break
        case 'checkov':                      name = 'checkov-job-runner'; break
        case 'codeql':
        case 'coverity':
        case 'custom':
        case 'customingest':
        case 'custom_ingest':
        case 'metasploit':                   name = 'sto-plugin'; break
        case 'fossa':                        name = 'fossa-job-runner'; break
        case 'cortexcloud':
        case 'cortex_cloud':
        case 'cortex':                       name = 'cortex-cloud-job-runner'; break
        case 'ghas':
        case 'githubadvancedsecurity':       name = 'github-advanced-security-job-runner'; break
        case 'gitleaks':                     name = 'gitleaks-job-runner'; break
        case 'grype':                        name = 'grype-job-runner'; break
        case 'harnesssast':
        case 'harnesssca':
        case 'shiftleft':
        case 'qwiet':                        name = 'shiftleft-job-runner'; break
        case 'mend':
        case 'whitesource':                  name = 'whitesource-agent-job-runner'; break
        case 'modelscan':                    name = 'modelscan-job-runner'; break
        case 'nexusiq':                      name = 'nexusiq-job-runner'; break
        case 'nikto':                        name = 'nikto-job-runner'; break
        case 'nmap':                         name = 'nmap-job-runner'; break
        case 'osv':                          name = 'osv-job-runner'; break
        case 'owasp':                        name = 'owasp-dependency-check-job-runner'; break
        case 'prisma':
        case 'prismacloud':
        case 'prisma_cloud':
        case 'prisma-cloud':
        case 'twistlock':                    name = 'twistlock-job-runner'; break
        case 'prowler':                      name = 'prowler-job-runner'; break
        case 'semgrep':                      name = 'semgrep-job-runner'; break
        case 'snyk':                         name = 'snyk-job-runner'; break
        case 'sonarqube':                    name = 'sonarqube-agent-job-runner'; break
        case 'sysdig':                       name = 'sysdig-job-runner'; break
        case 'traceable':                    name = 'traceable-job-runner'; break
        case 'veracode':                     name = 'veracode-agent-job-runner'; break
        case 'wiz':                          name = 'wiz-job-runner'; break
        case 'zap':
        case 'owaspzap':                     name = 'zap-job-runner'; break
        default:                             name = "${scanner}-job-runner"; break
    }
    def tag = cfg.dockerImageTag ?: 'dev'
    return "harness/${name}:${tag}"
}

// Socket the Jenkins agent uses to talk to Docker.
def dockerSockPath() {
    def host = env.DOCKER_HOST ?: 'unix:///var/run/docker.sock'
    return host.startsWith('unix://') ? host.substring('unix://'.length()) : '/var/run/docker.sock'
}

// Path the Docker daemon can bind-mount into the scanner container.
// Colima and Docker Desktop give the Mac a proxy socket. The daemon inside
// the VM only has /var/run/docker.sock, and mounting the Mac path fails with
// "mkdir .../docker.sock: operation not supported".
def dockerSockMountPath() {
    def client = dockerSockPath()
    if (client.contains('/.colima/') || client.contains('/.docker/run/') || client.endsWith('/.rd/docker.sock')) {
        return '/var/run/docker.sock'
    }
    return client
}

def coerceTargetType(String scanner, String targetType) {
    def lc = scanner.toLowerCase()
    def tt = (targetType ?: 'repository').toLowerCase()
    // Empty or repository follows the action: remap. Any other type is a mistake.
    if (lc == 'prowler') {
        if (!tt || tt == 'repository') {
            return 'configuration'
        }
        if (tt != 'configuration') {
            steps.error("prowler supports target-type configuration only (got: ${targetType})")
        }
        return tt
    }
    if (lc == 'traceable') {
        if (!tt || tt == 'repository') {
            return 'instance'
        }
        if (tt != 'instance') {
            steps.error("traceable supports target-type instance only (got: ${targetType})")
        }
        return tt
    }
    return tt
}

def shellSingleQuote(String value) {
    return "'" + value.replace("'", "'\\''") + "'"
}

def targetFlags(Map cfg) {
    def mode = (cfg.scanMode ?: 'orchestration').toLowerCase()
    def targetType = (cfg.targetType ?: 'repository').toLowerCase()
    def name = cfg.targetName?.toString()?.trim()
    def variant = cfg.targetVariant?.toString()?.trim()
    def alwaysManual = cfg.alwaysManual == true ||
        mode == 'ingestion' ||
        targetType == 'configuration'
    if (name && !variant) {
        steps.error('TARGET_VARIANT is required when TARGET_NAME is set')
    }
    if (!name && variant) {
        steps.error('TARGET_NAME is required when TARGET_VARIANT is set')
    }
    if (alwaysManual || (name && variant)) {
        if (!name || !variant) {
            steps.error('TARGET_NAME and TARGET_VARIANT are required for manual detection / ingestion / configuration')
        }
        return "-e TARGET_DETECTION=MANUAL -e TARGET_NAME=${shellSingleQuote(name)} -e TARGET_VARIANT=${shellSingleQuote(variant)}"
    }
    return '-e TARGET_DETECTION=auto'
}

def credBindings(List kinds) {
    def list = [steps.string(credentialsId: 'harness-pat-token-sto-lab', variable: 'HARNESS_TOKEN')]
    kinds.each { kind ->
        switch (kind) {
            case 'snyk':
                list << steps.string(credentialsId: 'SNYK_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                break
            case 'sonar':
                list << steps.string(credentialsId: 'SONAR_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'SONAR_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'wiz':
                list << steps.string(credentialsId: 'WIZ_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'WIZ_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                break
            case 'blackduck':
                list << steps.string(credentialsId: 'BLACKDUCK_DOMAIN', variable: 'SCANNER_DOMAIN')
                list << steps.string(credentialsId: 'BLACKDUCK_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'BLACKDUCK_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                break
            case 'prisma':
                list << steps.string(credentialsId: 'PRISMA_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'PRISMA_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'PRISMA_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'ghas':
                list << steps.string(credentialsId: 'GHAS_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                break
            case 'semgrep':
                list << steps.string(credentialsId: 'SEMGREP_APP_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                break
            case 'docker':
                list << steps.string(credentialsId: 'PRIVATE_DOCKER_ACCESS_ID', variable: 'IMAGE_ACCESS_ID')
                list << steps.string(credentialsId: 'PRIVATE_DOCKER_ACCESS_TOKEN', variable: 'IMAGE_ACCESS_TOKEN')
                break
            case 'aqua_security':
                list << steps.string(credentialsId: 'AQUA_SECURITY_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'AQUA_SECURITY_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                break
            case 'aws_ecr':
                list << steps.string(credentialsId: 'AWS_ECR_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'AWS_ECR_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                break
            case 'aws_config':
                list << steps.string(credentialsId: 'AWS_ACCESS_KEY_ID', variable: 'CONFIGURATION_ACCESS_ID')
                list << steps.string(credentialsId: 'AWS_SECRET_ACCESS_KEY', variable: 'CONFIGURATION_ACCESS_TOKEN')
                break
            case 'burp':
                list << steps.string(credentialsId: 'BURP_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'BURP_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'checkmarx':
                list << steps.string(credentialsId: 'CHECKMARX_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'CHECKMARX_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'CHECKMARX_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'checkmarx_one':
                list << steps.string(credentialsId: 'CHECKMARX_ONE_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'CHECKMARX_ONE_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'CHECKMARX_ONE_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'cortex':
                list << steps.string(credentialsId: 'CORTEX_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'CORTEX_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'CORTEX_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'mend':
                list << steps.string(credentialsId: 'MEND_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'MEND_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'MEND_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'nexusiq':
                list << steps.string(credentialsId: 'NEXUSIQ_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'NEXUSIQ_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'NEXUSIQ_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'sysdig':
                list << steps.string(credentialsId: 'SYSDIG_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'SYSDIG_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'traceable':
                list << steps.string(credentialsId: 'TRACEABLE_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                list << steps.string(credentialsId: 'TRACEABLE_DOMAIN', variable: 'SCANNER_DOMAIN')
                break
            case 'veracode':
                list << steps.string(credentialsId: 'VERACODE_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'VERACODE_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                break
            case 'anchore':
                list << steps.string(credentialsId: 'ANCHORE_ACCESS_ID', variable: 'SCANNER_ACCESS_ID')
                list << steps.string(credentialsId: 'ANCHORE_ACCESS_TOKEN', variable: 'SCANNER_ACCESS_TOKEN')
                break
        }
    }
    return list
}

def exportScanResult(String outputFile) {
    if (!steps.fileExists(outputFile)) {
        return
    }
    def text = steps.readFile(file: outputFile)
    def vals = [:]
    for (line in text.split('\n')) {
        def idx = line.indexOf('=')
        if (idx > 0) {
            vals[line.substring(0, idx)] = line.substring(idx + 1)
        }
    }
    if (vals.JOB_ID != null) { env.STO_SCAN_ID = vals.JOB_ID }
    if (vals.STATUS != null) { env.STO_SCAN_STATUS = vals.STATUS }
    if (vals.ISSUES_COUNT != null) { env.STO_ISSUES_COUNT = vals.ISSUES_COUNT }
    if (vals.CRITICAL != null) { env.STO_CRITICAL = vals.CRITICAL }
    if (vals.HIGH != null) { env.STO_HIGH = vals.HIGH }
    if (vals.MEDIUM != null) { env.STO_MEDIUM = vals.MEDIUM }
    if (vals.LOW != null) { env.STO_LOW = vals.LOW }
    if (vals.INFO != null) { env.STO_INFO = vals.INFO }
}

def showScanSummary(String outputFile, Map cfg = [:]) {
    def show = cfg.showSummary
    if (show == false || show == 'false') {
        return
    }
    steps.sh(script: """
        if [ -f '${outputFile}' ]; then
            echo "============================================================"
            echo "HARNESS STO SCAN RESULTS"
            echo "============================================================"
            echo ""
            echo "Vulnerability Summary:"
            CRITICAL=\$(grep "^CRITICAL=" '${outputFile}' | cut -d= -f2 || echo "0")
            HIGH=\$(grep "^HIGH=" '${outputFile}' | cut -d= -f2 || echo "0")
            MEDIUM=\$(grep "^MEDIUM=" '${outputFile}' | cut -d= -f2 || echo "0")
            LOW=\$(grep "^LOW=" '${outputFile}' | cut -d= -f2 || echo "0")
            INFO=\$(grep "^INFO=" '${outputFile}' | cut -d= -f2 || echo "0")
            TOTAL=\$(grep "^ISSUES_COUNT=" '${outputFile}' | cut -d= -f2 || echo "0")
            echo "  CRITICAL: \${CRITICAL}"
            echo "  HIGH: \${HIGH}"
            echo "  MEDIUM: \${MEDIUM}"
            echo "  LOW: \${LOW}"
            echo "  INFO: \${INFO}"
            echo "  TOTAL: \${TOTAL}"
            echo ""
            SCAN_ID=\$(grep "^JOB_ID=" '${outputFile}' | cut -d= -f2 || echo "")
            STATUS=\$(grep "^STATUS=" '${outputFile}' | cut -d= -f2 || echo "")
            echo "Scan ID: \${SCAN_ID}"
            echo "Status: \${STATUS}"
            echo "Execution URL: \${BUILD_URL}"
            echo "============================================================"
        fi
    """, returnStatus: true)
}

def addIf(List extra, String envName, def value) {
    if (value != null && value.toString().trim()) {
        extra << "-e ${envName}=${shellSingleQuote(value.toString())}"
    }
}

// Map values (stoScan scanner-access-token, and similar) become container env
// without landing in the sh script text. Unset keys keep the withCredentials / job env.
def bindSecret(List secretEnv, String envName, def value) {
    def v = value?.toString()?.trim()
    if (!v) {
        return false
    }
    secretEnv << "${envName}=${v}"
    return true
}

def scannerEnvBindings(def raw) {
    def lines = []
    def text = raw?.toString()
    if (!text?.trim()) {
        return lines
    }
    for (line in text.split('\n')) {
        def trimmed = line.toString().trim()
        if (!trimmed || trimmed.startsWith('#')) {
            continue
        }
        def eq = trimmed.indexOf('=')
        if (eq <= 0) {
            steps.error("scanner-env lines must be KEY=value (got: ${trimmed})")
        }
        def key = trimmed.substring(0, eq)
        if (!(key ==~ /[A-Za-z_][A-Za-z0-9_]*/)) {
            steps.error("scanner-env key must be an environment variable name (got: ${key})")
        }
        lines << trimmed
    }
    return lines
}

// Template-library mendStep remaps byTokens/byNames for orchestration.
// Pass through for other scanners (NexusIQ, Fortify on Demand).
def remapLookupType(String product, String scanMode, def raw) {
    def lookup = raw?.toString()?.trim()
    if (!lookup) {
        return null
    }
    if (product == 'whitesource') {
        def lc = lookup.toLowerCase()
        def modeLc = (scanMode ?: '').toLowerCase()
        if (modeLc == 'orchestration') {
            if (lc in ['bytokens', 'appendtoproductbytoken']) {
                return 'appendToProductByToken'
            }
            if (lc in ['bynames', 'appendtoproductbyname']) {
                return 'appendToProductByName'
            }
        } else {
            if (lc in ['bytokens', 'appendtoproductbytoken']) {
                return 'byTokens'
            }
            if (lc in ['bynames', 'appendtoproductbyname']) {
                return 'byNames'
            }
        }
    }
    return lookup
}

def pullPolicyFlag(Map cfg) {
    def pullPolicy = (cfg.imagePullPolicy ?: 'missing').toString().trim().toLowerCase()
    if (!(pullPolicy in ['missing', 'always', 'never'])) {
        steps.error("IMAGE_PULL_POLICY must be missing, always, or never (got: ${cfg.imagePullPolicy})")
    }
    return pullPolicy
}

def runtimeControlFlags(Map cfg) {
    def flags = []
    def runAs = cfg.runAsUser?.toString()?.trim()
    if (runAs) {
        flags << "--user ${runAs}"
        steps.echo "Running scanner container as user ${runAs}"
    }
    def mem = cfg.memoryLimit?.toString()?.trim()
    if (mem) {
        flags << "--memory ${mem}"
        steps.echo "Memory limit: ${mem}"
    }
    def cpu = cfg.cpuLimit?.toString()?.trim()
    if (cpu) {
        flags << "--cpus ${cpu}"
        steps.echo "CPU limit: ${cpu}"
    }
    return flags.join(' ')
}

def run(Map cfg) {
    // Lab jobs pass creds: [] or creds: ['wiz'] → bind Jenkins credential IDs.
    // Onboarding / GHA-style omits creds and sets HARNESS_TOKEN + SCANNER_* in environment {}.
    if (cfg.containsKey('creds')) {
        steps.withCredentials(credBindings(cfg.creds ?: [])) {
            runDocker(cfg)
        }
    } else {
        runDocker(cfg)
    }
}

def runDocker(Map cfg) {
        def scanner = cfg.scanner
        def product = productName(scanner)
        def image = jobRunnerImage(scanner, cfg)
        def targetType = coerceTargetType(scanner, cfg.targetType ?: 'repository')
        cfg.targetType = targetType
        def scanMode = (cfg.scanMode ?: 'orchestration').toLowerCase()
        def scanConfig = cfg.scanConfig ?: 'default'
        def workspaceRel = cfg.workspace ?: '.'
        def outputFile = cfg.outputFile ?: 'scan-output.env'
        def hostOutput = "${env.WORKSPACE}/${outputFile}"
        def sourceRoot = cfg.sourceRoot ?: "${env.WORKSPACE}/src"
        def pluginWs = pluginWorkspace(workspaceRel)
        def repoMount = sourceRoot
        def wsRelInside = pluginWs.replaceFirst('^/harness/?', '')
        def hostScanDir = wsRelInside ? "${sourceRoot}/${wsRelInside}" : sourceRoot
        def imageType = (cfg.imageType ?: '').toString()
        def imageTypeLc = imageType.toLowerCase().replace('_', '-')
        // Unset dockerMode keeps the previous auto host-Docker rules.
        def dockerModeRaw = (cfg.dockerMode ?: '').toString().trim().toLowerCase().replace('_', '-')
        def pluginDockerMode = ''
        if (dockerModeRaw in ['with-docker-in-docker', 'docker-in-docker']) {
            pluginDockerMode = 'docker-in-docker'
        } else if (dockerModeRaw == 'without-docker-in-docker') {
            pluginDockerMode = 'without-docker-in-docker'
        } else if (dockerModeRaw) {
            steps.error("docker-mode must be docker-in-docker or without-docker-in-docker (got: ${cfg.dockerMode})")
        }
        def autoHostDocker = targetType == 'container' &&
            scanMode != 'ingestion' &&
            scanMode != 'extraction' &&
            product in ['blackduckhub', 'twistlock', 'cortex_cloud']
        def useHostDocker = cfg.useHostDocker == true ||
            pluginDockerMode == 'docker-in-docker' ||
            imageTypeLc in ['local-image'] ||
            (pluginDockerMode != 'without-docker-in-docker' && autoHostDocker)
        if (useHostDocker && !pluginDockerMode) {
            pluginDockerMode = 'docker-in-docker'
        }
        def sock = dockerSockPath()
        def sockMount = dockerSockMountPath()
        def extra = []
        extra << "-e SCANNER=${product}"
        extra << "-e PRODUCT_NAME=${product}"
        extra << "-e SCAN_MODE=${scanMode}"
        extra << "-e SCAN_CONFIG=${scanConfig}"
        extra << "-e TARGET_TYPE=${targetType}"
        extra << "-e BUILD_ID=${env.BUILD_NUMBER}"
        extra << "-e HARNESS_BUILD_ID=${env.BUILD_NUMBER}"
        // Plugin defaults pipelineId to "_pipeline" when HARNESS_PIPELINE_ID is unset.
        // JOB_NAME is the Jenkins pipeline; STO MachineNamePattern disallows hyphens/slashes.
        def pipelineId = (env.JOB_NAME ?: '').toString().replaceAll(/[^a-zA-Z0-9_]/, '_')
        if (pipelineId) {
            def lead = pipelineId.substring(0, 1)
            def leadOk = lead == '_' || (lead >= 'A' && lead <= 'Z') || (lead >= 'a' && lead <= 'z')
            if (!leadOk) {
                pipelineId = "_${pipelineId}"
            }
        }
        if (pipelineId.length() > 128) {
            pipelineId = pipelineId.substring(0, 128)
        }
        if (pipelineId) {
            extra << "-e HARNESS_PIPELINE_ID=${pipelineId}"
        }
        // Plugin only reads EXECUTION_URL; pass the Jenkins URL as KEY=value.
        addIf(extra, 'EXECUTION_URL', env.BUILD_URL)
        extra << "-e PLUGIN_LOG_LEVEL=${cfg.logLevel ?: 'INFO'}"
        extra << "-e FAIL_ON_SEVERITY=${failOnSeverityEnv(cfg)}"
        extra << "-e SUMMARY_OUTPUT_PATH=/harness-output/${outputFile}"
        extra << "-e WORKSPACE=${pluginWs}"
        extra << targetFlags(cfg)

        def kinds = cfg.creds ?: []
        def tokenKinds = ['snyk', 'sonar', 'wiz', 'blackduck', 'prisma', 'ghas', 'semgrep',
                          'aqua_security', 'aws_ecr', 'burp', 'checkmarx', 'checkmarx_one',
                          'cortex', 'mend', 'nexusiq', 'sysdig', 'traceable', 'veracode', 'anchore']
        def idKinds = ['wiz', 'blackduck', 'prisma', 'aqua_security', 'aws_ecr', 'checkmarx',
                       'checkmarx_one', 'cortex', 'mend', 'nexusiq', 'veracode', 'anchore']
        def domainKinds = ['sonar', 'blackduck', 'prisma', 'burp', 'checkmarx', 'checkmarx_one',
                           'cortex', 'mend', 'nexusiq', 'sysdig', 'traceable']
        def needsToken = cfg.scannerAccessTokenEnv ? true : false
        def needsId = cfg.scannerAccessIdEnv ? true : false
        def needsDomain = cfg.scannerDomainEnv ? true : false
        for (k in kinds) {
            if (k in tokenKinds) { needsToken = true }
            if (k in idKinds) { needsId = true }
            if (k in domainKinds) { needsDomain = true }
        }
        if (env.SCANNER_ACCESS_TOKEN) { needsToken = true }
        if (env.SCANNER_ACCESS_ID) { needsId = true }
        if (env.SCANNER_DOMAIN) { needsDomain = true }
        def secretEnv = []
        if (bindSecret(secretEnv, 'SCANNER_ACCESS_TOKEN', cfg.scannerAccessToken)) { needsToken = true }
        if (bindSecret(secretEnv, 'SCANNER_ACCESS_ID', cfg.scannerAccessId)) { needsId = true }
        if (needsToken) { extra << '-e SCANNER_ACCESS_TOKEN' }
        if (needsId) { extra << '-e SCANNER_ACCESS_ID' }
        if (needsDomain) { extra << '-e SCANNER_DOMAIN' }
        def imageFromCfg = false
        if (bindSecret(secretEnv, 'IMAGE_ACCESS_ID', cfg.imageAccessId)) { imageFromCfg = true }
        if (bindSecret(secretEnv, 'IMAGE_ACCESS_TOKEN', cfg.imageAccessToken)) { imageFromCfg = true }
        if (imageFromCfg || kinds.contains('docker') || cfg.imageAccessEnv || env.IMAGE_ACCESS_TOKEN) {
            extra << '-e IMAGE_ACCESS_ID'
            extra << '-e IMAGE_ACCESS_TOKEN'
        }
        def configFromCfg = false
        if (bindSecret(secretEnv, 'CONFIGURATION_ACCESS_ID', cfg.configurationAccessId)) { configFromCfg = true }
        if (bindSecret(secretEnv, 'CONFIGURATION_ACCESS_TOKEN', cfg.configurationAccessToken)) { configFromCfg = true }
        if (configFromCfg || kinds.contains('aws_config') || env.CONFIGURATION_ACCESS_TOKEN) {
            extra << '-e CONFIGURATION_ACCESS_ID'
            extra << '-e CONFIGURATION_ACCESS_TOKEN'
        }

        addIf(extra, 'SCANNER_AUTH_TYPE', cfg.scannerAuthType)
        addIf(extra, 'SCANNER_API_VERSION', cfg.scannerApiVersion)
        addIf(extra, 'SCANNER_VERIFY_SSL', cfg.scannerVerifySsl)
        addIf(extra, 'SCANNER_PROJECT_NAME', cfg.scannerProjectName)
        addIf(extra, 'SCANNER_PROJECT_VERSION', cfg.scannerProjectVersion)
        addIf(extra, 'SCANNER_REGION', cfg.scannerRegion)
        // SonarQube identity is the project key. Copy the name only when the key is empty.
        def projectKey = cfg.scannerProjectKey?.toString()?.trim()
        if (!projectKey && product == 'sonarqube' && cfg.scannerProjectName?.toString()?.trim()) {
            projectKey = cfg.scannerProjectName.toString().trim()
        }
        addIf(extra, 'SCANNER_PROJECT_KEY', projectKey)
        addIf(extra, 'PRODUCT_ANALYSIS_DETECTION', cfg.productAnalysisDetection)
        addIf(extra, 'SCANNER_BRANCH_NAME', cfg.scannerBranchName)
        addIf(extra, 'SCANNER_EXCLUDE', cfg.scannerExclude)
        addIf(extra, 'SCANNER_JAVA_BINARIES', cfg.scannerJavaBinaries)
        addIf(extra, 'SCANNER_JAVA_LIBRARIES', cfg.scannerJavaLibraries)
        addIf(extra, 'SCANNER_ORGANIZATION_ID', cfg.scannerOrganizationId)
        addIf(extra, 'SCANNER_LOOKUP_TYPE', remapLookupType(product, scanMode, cfg.scannerLookupType))
        addIf(extra, 'SCANNER_INCLUDE', cfg.scannerInclude)
        addIf(extra, 'SCANNER_PRODUCT_NAME', cfg.scannerProductName)
        addIf(extra, 'SCANNER_PRODUCT_TOKEN', cfg.scannerProductToken)
        addIf(extra, 'SCANNER_PROJECT_TOKEN', cfg.scannerProjectToken)
        addIf(extra, 'SCANNER_SUITE_ID', cfg.scannerSuiteId)
        addIf(extra, 'SCANNER_SCAN_ID', cfg.scannerScanId)
        addIf(extra, 'SCANNER_SCAN_NAME', cfg.scannerScanName)
        addIf(extra, 'SCANNER_RUNNER_ID', cfg.scannerRunnerId)
        addIf(extra, 'SCANNER_CONTEXT', cfg.scannerContext)
        def toolArgs = cfg.toolArgs?.toString()?.trim() ?: ''
        def orgId = cfg.scannerOrganizationId?.toString()?.trim()
        if (product == 'sonarqube' && orgId) {
            def padded = " ${toolArgs} "
            if (!padded.contains(' -Dsonar.organization=') && !padded.contains(' sonar.organization=')) {
                toolArgs = toolArgs ? "${toolArgs} -Dsonar.organization=${orgId}" : "-Dsonar.organization=${orgId}"
            }
        }
        addIf(extra, 'TOOL_ARGS', toolArgs)
        addIf(extra, 'INGEST_TOOL_SEVERITY', cfg.ingestToolSeverity)
        addIf(extra, 'INCLUDE_RAW', cfg.includeRaw)
        addIf(extra, 'CONFIGURATION_REGION', cfg.configurationRegion)
        addIf(extra, 'ZAP_PORT', cfg.zapPort)

        if (targetType == 'configuration' || scanner.toLowerCase() == 'prowler') {
            extra << "-e CONFIGURATION_TYPE=${cfg.configurationType ?: 'aws_account'}"
        }

        if (targetType == 'container') {
            addIf(extra, 'IMAGE_NAME', cfg.imageName)
            addIf(extra, 'IMAGE_TAG', cfg.imageTag)
            addIf(extra, 'CONTAINER_DOMAIN', cfg.imageDomain)
            addIf(extra, 'CONTAINER_TYPE', imageType ?: 'docker_v2')
            addIf(extra, 'CONTAINER_REGION', cfg.containerRegion)
        }
        if (targetType == 'instance') {
            addIf(extra, 'INSTANCE_DOMAIN', cfg.instanceDomain)
            addIf(extra, 'INSTANCE_PROTOCOL', cfg.instanceProtocol)
            addIf(extra, 'INSTANCE_PORT', cfg.instancePort)
            addIf(extra, 'INSTANCE_PATH', cfg.instancePath)
        }

        if (pluginDockerMode) {
            extra << "-e DOCKER_MODE=${pluginDockerMode}"
        }
        def hostDockerFlags = ''
        if (useHostDocker) {
            extra << '-e ADDON_PATH=/tmp/sto-addon'
            hostDockerFlags = "--privileged --network host -v ${sockMount}:/var/run/docker.sock -v /tmp/sto-addon:/tmp/sto-addon"
        } else if (cfg.privileged == true) {
            hostDockerFlags = '--privileged'
        }
        def passEnv = scannerEnvBindings(cfg.scannerEnv)
        for (line in passEnv) {
            extra << "-e ${line.substring(0, line.indexOf('='))}"
        }

        def ingestFlags = ''
        def ingestHost = ''
        if (scanMode == 'ingestion') {
            if (!cfg.ingestionFile) {
                steps.error('INGESTION_FILE is required when SCAN_MODE is ingestion')
            }
            ingestHost = cfg.ingestionFile.toString().startsWith('/')
                ? cfg.ingestionFile.toString()
                : "${sourceRoot}/${cfg.ingestionFile}"
            ingestFlags = "-v ${shellSingleQuote(ingestHost)}:/harness-ingestion/results:ro -e INGESTION_FILE=/harness-ingestion/results"
        }

        def preflight = ''
        if (useHostDocker) {
            preflight += """
            if [ ! -S ${shellSingleQuote(sock)} ]; then
              echo "Error: this scan needs the host Docker daemon but ${sock} was not found."
              exit 1
            fi
"""
        }
        if (scanMode == 'ingestion') {
            preflight += """
            if [ ! -f ${shellSingleQuote(ingestHost)} ]; then
              echo "Error: ingestion file not found: ${ingestHost}"
              exit 1
            fi
"""
        }

        def extraStr = extra.join(' \\\n                                ')
        // Plugin maps DRONE_REPO_SCM → gitMetadata.provider → STO "Source" field.
        // Without it, repository targets show Source "Other" in the STO UI.
        // Derive provider from the Jenkins checkout remote (GIT_URL).
        def gitUrl = (env.GIT_URL ?: '').toString().toLowerCase()
        def repoScm = ''
        if (gitUrl.contains('github')) {
            repoScm = 'Github'
        } else if (gitUrl.contains('gitlab')) {
            repoScm = 'Gitlab'
        } else if (gitUrl.contains('bitbucket')) {
            repoScm = 'Bitbucket'
        } else if (gitUrl.contains('dev.azure') || gitUrl.contains('visualstudio')) {
            repoScm = 'Azure'
        }
        def scmFlag = (cfg.targetType == 'repository' && repoScm) ? "-e DRONE_REPO_SCM=${repoScm}" : ''
        def pullPolicy = pullPolicyFlag(cfg)
        def runtimeFlags = runtimeControlFlags(cfg)
        def regUser = (cfg.registryUsername ?: env.REGISTRY_USERNAME)?.toString()?.trim()
        def regToken = (cfg.registryToken ?: env.REGISTRY_TOKEN)?.toString()?.trim()
        def regDomain = (cfg.registryDomain ?: env.REGISTRY_DOMAIN)?.toString()?.trim()
        def loginEnv = []
        if (regUser && regToken) {
            loginEnv = ["STO_REG_USER=${regUser}", "STO_REG_TOKEN=${regToken}"]
            if (regDomain) {
                loginEnv << "STO_REG_DOMAIN=${regDomain}"
            }
        }

        def runScript = """
            set -e
            mkdir -p /tmp/sto-addon
            mkdir -p "${hostScanDir}"
            if [ -n "\${STO_REG_USER:-}" ] && [ -n "\${STO_REG_TOKEN:-}" ]; then
              if [ -n "\${STO_REG_DOMAIN:-}" ]; then
                echo "\$STO_REG_TOKEN" | docker login "\$STO_REG_DOMAIN" --username "\$STO_REG_USER" --password-stdin
              else
                echo "\$STO_REG_TOKEN" | docker login --username "\$STO_REG_USER" --password-stdin
              fi
            fi
            ${preflight}
            set +e
            docker run --rm --pull ${pullPolicy} \\
                ${runtimeFlags} \\
                -v "${repoMount}:/harness" \\
                -v "${env.WORKSPACE}:/harness-output" \\
                ${hostDockerFlags} \\
                ${ingestFlags} \\
                -e HARNESS_DOMAIN \\
                -e HARNESS_TOKEN \\
                -e HARNESS_ACCOUNT_ID \\
                -e HARNESS_ORG_ID \\
                -e HARNESS_PROJECT_ID \\
                -e EXECUTION_SOURCE=jenkins \\
                ${scmFlag} \\
                ${extraStr} \\
                ${image}
            SCAN_EXIT=\$?
            exit \$SCAN_EXIT
        """
        def runEnv = []
        runEnv.addAll(loginEnv)
        runEnv.addAll(secretEnv)
        runEnv.addAll(passEnv)
        def scanStatus
        if (runEnv) {
            steps.withEnv(runEnv) {
                scanStatus = steps.sh(script: runScript, returnStatus: true)
            }
        } else {
            scanStatus = steps.sh(script: runScript, returnStatus: true)
        }
        exportScanResult(outputFile)
        showScanSummary(hostOutput, cfg)
        if (scanStatus != 0) {
            steps.error("STO scan failed with exit code ${scanStatus}")
        }
}

def runFromParams(String scanner, Map opts = [:]) {
    def p = params
    def mode = (p.SCAN_MODE ?: 'orchestration').toString()
    def targetType = coerceTargetType(scanner, (p.TARGET_TYPE ?: (opts.targetType ?: 'repository')).toString())
    def alwaysManual = opts.alwaysManual == true ||
        mode.toLowerCase() == 'ingestion' ||
        targetType == 'configuration'
    def detection = alwaysManual ? 'manual' : (p.TARGET_DETECTION ?: 'auto').toString()
    def cfg = [
        scanner        : scanner,
        scanMode       : mode,
        scanConfig     : p.SCAN_CONFIG ?: 'default',
        targetType     : targetType,
        workspace      : p.WORKSPACE ?: (opts.workspace ?: '.'),
        outputFile     : p.OUTPUT_FILE ?: 'scan-output.env',
        sourceRoot     : "${env.WORKSPACE}/src",
        creds          : opts.creds ?: [],
        alwaysManual   : alwaysManual,
        logLevel       : p.LOG_LEVEL ?: 'INFO',
        failOnSeverity : p.FAIL_ON_SEVERITY ?: 'NONE',
        showSummary    : p.SHOW_SUMMARY,
        dockerImageTag : p.DOCKER_IMAGE_TAG ?: 'dev',
    ] + opts

    if (detection.toLowerCase() == 'manual' || alwaysManual) {
        cfg.targetName = p.TARGET_NAME
        cfg.targetVariant = p.TARGET_VARIANT
    }
    if (targetType == 'container') {
        cfg.imageName = p.IMAGE_NAME
        cfg.imageTag = p.IMAGE_TAG
        cfg.imageType = p.IMAGE_TYPE
        cfg.imageDomain = p.IMAGE_DOMAIN
        if (p.CONTAINER_REGION) { cfg.containerRegion = p.CONTAINER_REGION }
    }
    if (targetType == 'instance') {
        cfg.instanceDomain = p.INSTANCE_DOMAIN
        cfg.instanceProtocol = p.INSTANCE_PROTOCOL ?: 'https'
        cfg.instancePort = p.INSTANCE_PORT
        if (p.INSTANCE_PATH) { cfg.instancePath = p.INSTANCE_PATH }
    }
    if (mode.toLowerCase() == 'ingestion') {
        cfg.ingestionFile = p.INGESTION_FILE
    }
    if (p.SCANNER_AUTH_TYPE) { cfg.scannerAuthType = p.SCANNER_AUTH_TYPE }
    if (p.SCANNER_API_VERSION) { cfg.scannerApiVersion = p.SCANNER_API_VERSION }
    if (p.SCANNER_PROJECT_NAME) { cfg.scannerProjectName = p.SCANNER_PROJECT_NAME }
    if (p.SCANNER_PROJECT_VERSION) { cfg.scannerProjectVersion = p.SCANNER_PROJECT_VERSION }
    if (p.SCANNER_VERIFY_SSL) { cfg.scannerVerifySsl = p.SCANNER_VERIFY_SSL }
    if (p.SCANNER_PROJECT_KEY) { cfg.scannerProjectKey = p.SCANNER_PROJECT_KEY }
    if (p.PRODUCT_ANALYSIS_DETECTION) { cfg.productAnalysisDetection = p.PRODUCT_ANALYSIS_DETECTION }
    if (p.SCANNER_BRANCH_NAME) { cfg.scannerBranchName = p.SCANNER_BRANCH_NAME }
    if (p.SCANNER_EXCLUDE) { cfg.scannerExclude = p.SCANNER_EXCLUDE }
    if (p.SCANNER_JAVA_BINARIES) { cfg.scannerJavaBinaries = p.SCANNER_JAVA_BINARIES }
    if (p.SCANNER_JAVA_LIBRARIES) { cfg.scannerJavaLibraries = p.SCANNER_JAVA_LIBRARIES }
    if (p.SCANNER_ORGANIZATION_ID) { cfg.scannerOrganizationId = p.SCANNER_ORGANIZATION_ID }
    if (p.SCANNER_LOOKUP_TYPE) { cfg.scannerLookupType = p.SCANNER_LOOKUP_TYPE }
    if (p.SCANNER_INCLUDE) { cfg.scannerInclude = p.SCANNER_INCLUDE }
    if (p.SCANNER_PRODUCT_NAME) { cfg.scannerProductName = p.SCANNER_PRODUCT_NAME }
    if (p.SCANNER_PRODUCT_TOKEN) { cfg.scannerProductToken = p.SCANNER_PRODUCT_TOKEN }
    if (p.SCANNER_PROJECT_TOKEN) { cfg.scannerProjectToken = p.SCANNER_PROJECT_TOKEN }
    if (p.SCANNER_SUITE_ID) { cfg.scannerSuiteId = p.SCANNER_SUITE_ID }
    if (p.SCANNER_SCAN_ID) { cfg.scannerScanId = p.SCANNER_SCAN_ID }
    if (p.SCANNER_SCAN_NAME) { cfg.scannerScanName = p.SCANNER_SCAN_NAME }
    if (p.SCANNER_RUNNER_ID) { cfg.scannerRunnerId = p.SCANNER_RUNNER_ID }
    if (p.SCANNER_CONTEXT) { cfg.scannerContext = p.SCANNER_CONTEXT }
    if (p.CONFIGURATION_TYPE) { cfg.configurationType = p.CONFIGURATION_TYPE }
    if (p.CONFIGURATION_REGION) { cfg.configurationRegion = p.CONFIGURATION_REGION }
    if (p.ZAP_PORT) { cfg.zapPort = p.ZAP_PORT }
    if (p.TOOL_ARGS) { cfg.toolArgs = p.TOOL_ARGS }
    if (p.INGEST_TOOL_SEVERITY == true || p.INGEST_TOOL_SEVERITY == 'true') {
        cfg.ingestToolSeverity = 'true'
    }
    if (p.PRIVILEGED == true || p.PRIVILEGED == 'true') { cfg.privileged = true }
    if (p.IMAGE_PULL_POLICY) { cfg.imagePullPolicy = p.IMAGE_PULL_POLICY }
    if (p.RUN_AS_USER) { cfg.runAsUser = p.RUN_AS_USER }
    if (p.MEMORY_LIMIT) { cfg.memoryLimit = p.MEMORY_LIMIT }
    if (p.CPU_LIMIT) { cfg.cpuLimit = p.CPU_LIMIT }
    if (p.REGISTRY_USERNAME) { cfg.registryUsername = p.REGISTRY_USERNAME }
    if (p.REGISTRY_TOKEN) { cfg.registryToken = p.REGISTRY_TOKEN }
    if (p.REGISTRY_DOMAIN) { cfg.registryDomain = p.REGISTRY_DOMAIN }
    if (p.SCANNER_REGION) { cfg.scannerRegion = p.SCANNER_REGION }
    if (p.DOCKER_MODE) { cfg.dockerMode = p.DOCKER_MODE }
    if (p.SCANNER_ENV) { cfg.scannerEnv = p.SCANNER_ENV }
    if (p.INCLUDE_RAW != null && p.INCLUDE_RAW.toString().trim()) {
        cfg.includeRaw = p.INCLUDE_RAW.toString()
    }

    run(cfg)
}

return this