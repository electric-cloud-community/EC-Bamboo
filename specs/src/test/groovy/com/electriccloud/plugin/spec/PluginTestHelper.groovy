package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.utils.BambooClient
import com.electriccloud.spec.PluginSpockTestSupport

class PluginTestHelper extends PluginSpockTestSupport {

    static String automationTestsContextRun = System.getenv('AUTOMATION_TESTS_CONTEXT_RUN') ?: 'Sanity'
    static String PLUGIN_NAME = System.getenv('PLUGIN_NAME') ?: 'EC-Bamboo'
    static String PLUGIN_VERSION = System.getenv('PLUGIN_VERSION') ?: '1.5.0'

    static String BAMBOO_URL = getURL()
    static String BAMBOO_USERNAME = getUsername()
    static String BAMBOO_PASSWORD = getPassword()
    static String CONFIG_NAME = 'specConfig'

    // 0.0.0.0 is required for the BambooClient to connect to the docker container
    // because the container hostname does not work for it
    static String commanderAddress = System.getenv('COMMANDER_SERVER') ?: '0.0.0.0'

    def createDefaultProject(){
        def bambooClient = initBambooClient()
        def projectExist = true
        try {
            bambooClient.getPlans('PROJECT')
        }
        catch (Exception e){
            projectExist = false
        }
        if (!projectExist) {
            bambooClient.createDefaultPlan()
        }
    }

    def createConfiguration(String configName = CONFIG_NAME, Map props = [:]) {
        String username = BAMBOO_USERNAME
        String password = BAMBOO_PASSWORD
        String endpoint = BAMBOO_URL

        if (System.getenv('RECREATE_CONFIG')) {
            props.recreate = true
        }

        def params = [
                desc            : 'Spec tests configuration',
                endpoint        : endpoint,
                basic_credential: configName,
                proxy_credential: configName + '_proxy_credential',
                debugLevel      : 2,
                httpProxyUrl    : isProxyAvailable() ? getProxyURL() : ''
        ]

        def credentials = [
                [
                        credentialName: configName,
                        userName      : username,
                        password      : password
                ], [
                        credentialName: configName + '_proxy_credential',
                        userName      : isProxyAvailable() ? getProxyUsername() : '',
                        password      : isProxyAvailable() ? getProxyPassword() : ''
                ]
        ]

        return createPluginConfiguration(
                PLUGIN_NAME,
                configName,
                params,
                credentials,
                props
        )
    }

    def createPluginConfiguration(String pluginName, String configName, Map params = [:], List credentials = [], Map opts = [:]) {
        def confPath = opts.confPath ?: 'ec_plugin_cfgs'

        if (doesConfExist("/plugins/$pluginName/project/$confPath", configName)) {
            if (opts.recreate) {
                deleteConfiguration(pluginName, configName)
            } else {
                println "Configuration $configName exists"
                return
            }
        }

        def credentialDsl = credentials.collect {
            "[credentialName: '${it.credentialName}', userName: '${it.userName}', password: '${it.password}']"
        }.join(',')

        params.config = configName
        // Parameters
        def paramLines = params.collect { key, value ->
            "$key: '''$value'''"
        }
        def actualParameter = '[' + paramLines.join(', ') + ']'

        def result = dsl("""
            runProcedure(
                projectName: "/plugins/$pluginName/project",
                procedureName: 'CreateConfiguration',
                credential: [ $credentialDsl ],
                actualParameter: $actualParameter
            )
        """)

        assert result?.jobId
        def poll = createPoll(120)
        poll.eventually {
            jobCompleted(result)
        }
        assert jobStatus(result.jobId).outcome == 'success'
    }

    static String getAssertedEnvVariable(String varName) {
        String varValue = System.getenv(varName)
        logger.debug("VAR: '$varName'" + " VALUE: " + varValue ?: '')
        assert varValue
        return varValue
    }

    static String getURL() { getAssertedEnvVariable("BAMBOO_URL") }

    static String getUsername() { getAssertedEnvVariable("BAMBOO_USERNAME") }

    static String getPassword() { getAssertedEnvVariable("BAMBOO_PASSWORD") }

    static String getResourceName() {
        return 'local'
    }

    static boolean isProxyAvailable() {
        def value = System.getenv("IS_PROXY_AVAILABLE")
        if (value != null && value != '' && value != 'true') {
            logger.warn("Value for IS_PROXY_AVAILABLE should be 'true' or empty.")
        }
        return value == 'true'
    }

    static String getProxyURL() { getAssertedEnvVariable('EF_PROXY_URL') }

    static String getProxyUsername() { getAssertedEnvVariable('EF_PROXY_USERNAME') }

    static String getProxyPassword() { getAssertedEnvVariable('EF_PROXY_PASSWORD') }

    def stringifyParameters(Map parameters) {
        return parameters
        // Skip undefined values
                .findAll { k, v -> v != null }
                .collect { k, v ->
                    v = ((String) v).replace('\'', '\\\'')
                    "$k: '''$v'''"
                }.join(', ')
    }

    def runProcedure(String projectName, String procedureName, Map parameters) {
        def parametersString = stringifyParameters(parameters)

        def code = """
            runProcedure(
                projectName: '$projectName',
                procedureName: '$procedureName',
                actualParameter: [
                    $parametersString                 
                ]
            )
        """

        return runProcedureDsl(code)
    }

    def runSchedule(String projectName, String scheduleName, String procedureName, Map parameters) {
        def parametersString = stringifyParameters(parameters)

        def code = """
            runProcedure(
                projectName: '$projectName',
                procedureName: '$procedureName',
                scheduleName: '$scheduleName',
                actualParameter: [
                    $parametersString                 
                ]
            )
        """

        return runProcedureDsl(code)
    }

    def redirectLogs(String parentProperty = '/myJob') {
        def propertyLogName = parentProperty + '/debug_logs'
        dsl """
            setProperty(
                propertyName: "/plugins/${PLUGIN_NAME}/project/ec_debug_logToProperty",
                value: "$propertyLogName"
            )
        """
        return propertyLogName
    }

    String redirectLogsToPipeline() {
        String propertyName = '/myPipelineRuntime/debugLogs'
        dsl """
            setProperty(
                propertyName: "/plugins/${PLUGIN_NAME}/project/ec_debug_logToProperty",
                value: "$propertyName"
            )
        """
        propertyName
    }

    String getJobLogs(def jobId) {
        assert jobId
        def logs
        try {
            logs = getJobProperty("/myJob/debug_logs", jobId)
        } catch (Throwable e) {
            logs = "Possible exception in logs; check job"
        }
        logs
    }

    def getPipelineLogs(flowRuntimeId) {
        assert flowRuntimeId
        getPipelineProperty('/myPipelineRuntime/debugLogs', flowRuntimeId)
    }

    def flexibleIgnore(name = null) {
        def ignoreVar = System.getenv('SPEC_CASE')
        println ignoreVar
        if (!ignoreVar) {
            return false
        }
        if (!name) {
            return true
        }
        return name =~ /$ignoreVar/ ? false : true
    }

    def runProcedureDsl(dslString) {
        redirectLogs()
        assert dslString

        def result = dsl(dslString)
        assert result.jobId

        waitUntil {
            jobCompleted((String) result.jobId)
        }
        def logs = getJobLogs(result.jobId)
        def outcome = jobStatus(result.jobId).outcome
        logger.debug("DSL: $dslString")
        logger.debug("Logs: $logs")
        logger.debug("Outcome: $outcome")
        [logs: logs, outcome: outcome, jobId: result.jobId]
    }

    def jobCompleted(String jobId) {
        def res = dsl("waitForCompletion jobId:'$jobId'", null, [timeout: 900])
        return true
    }

    def getStepSummary(def jobId, def stepName) {
        assert jobId
        def summary
        def property = "/myJob/jobSteps/RunProcedure/steps/$stepName/summary"
        try {
            summary = getJobProperty(property, jobId)
        } catch (Throwable e) {
            logger.debug("Can't retrieve Upper Step Summary from the property: '$property'; check job: " + jobId)
        }
        return summary
    }

    def createResource(def resName) {
        dsl """
            createResource(
                resourceName: '$resName',
                hostName: '127.0.0.1',
                port: '7800'
            )
        """
    }


    def getJobStepDetails(def jobStepId) {
        assert jobStepId
        def stepId
        try {
            stepId = dsl """
            getJobStepDetails(
                jobStepId: '$jobStepId'
                )
          """
        } catch (Throwable e) {
            logger.debug("Can't retrieve job Step Details for job Step: " + jobStepId)
        }
        return stepId
    }

    def getJobStepProperty(def jobStepId, def propertyName) {
        assert jobStepId
        def propertyValue
        try {
            propertyValue = dsl """
            getProperty(
            jobStepId: '$jobStepId',
            propertyName: '$propertyName'
            )
        """
        } catch (Throwable e) {
            logger.debug("Can't retrieve property name $propertyName for jobStepIS: $jobStepId")
        }
        return propertyValue
    }

    String getProcedureJobStepSummary(String procedureName, String jobId) {
        return getJobProperty("/myJob/steps/RunProcedure/steps/$procedureName/summary", jobId)
    }



    def runPipeline(projectName, pipelineName, stagesToRun = [], int timeout = 300){
        def stagesDsl = ''
        if (stagesToRun.size() > 0){
            stagesDsl = ", stagesToRun: '" + stagesToRun.join(',') + "'"
        }

        logger.info("Running pipeline: \"$pipelineName\" with stages: $stagesToRun")

        def resp = dsl """
                    runPipeline(
                        projectName: '$projectName',
                        pipelineName: '$pipelineName',
                        $stagesDsl
                    )
            """
        waitUntil( {
            pipelineCompleted(resp)
        }, timeout, 0)

        // Display pipeline stages and tasks logs
        stagesToRun.each { stage ->
            def tasks = getPipelineTasks(stage, resp.flowRuntime.flowRuntimeId)
            def delimeter1 = "**************" * 5
            def delimeter2 = "==============" * 5
            logger.info("$delimeter1")
            logger.info("STAGE: $stage:")
            logger.info("$delimeter1\n")
            logger.info("\ttasks:".toUpperCase())
            tasks.each { task ->
                logger.info("$delimeter2")
                logger.info("\t$task.taskName:")
                logger.info("$delimeter2")
                logger.info(getJobLogs(task.jobId) as String)
            }

        }
        return resp
    }

    def initBambooClient(){
        URI bambooURI = new URI(BAMBOO_URL)

        def host = commanderAddress// bambooURI.getHost()

        def scheme = bambooURI.getScheme() ?: 'http'
        def port = bambooURI.getPort()
        def urlPath = bambooURI.getPath()

        return new BambooClient(scheme, host, port, urlPath, BAMBOO_USERNAME, BAMBOO_PASSWORD)
    }

}