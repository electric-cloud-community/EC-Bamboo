package com.electriccloud.plugin.spec

import com.electriccloud.spec.PluginSpockTestSupport

class PluginTestHelper extends PluginSpockTestSupport {

    static String automationTestsContextRun = System.getenv('AUTOMATION_TESTS_CONTEXT_RUN') ?: 'Sanity'
    static String PLUGIN_NAME = System.getenv('PLUGIN_NAME') ?: 'EC-Bamboo'
    static String PLUGIN_VERSION = System.getenv('PLUGIN_VERSION') ?: '1.5.0'

    static String BAMBOO_URL = getURL()
    static String BAMBOO_USERNAME = getUsername()
    static String BAMBOO_PASSWORD = getPassword()

    static String CONFIG_NAME = 'specConfig'

    def createConfiguration(String configName = CONFIG_NAME, Map props = [:]) {
        String username = BAMBOO_USERNAME
        String password = BAMBOO_PASSWORD
        String endpoint = BAMBOO_URL

        String checkConnection = 1

        if (System.getenv('RECREATE_CONFIG')) {
            props.recreate = true
        }

        createPluginConfiguration(
                PLUGIN_NAME,
                configName,
                [
                        desc           : 'Spec tests configuration',
                        endpoint       : endpoint,
                        credential     : 'credential',
                        debugLevel     : 3,
                        checkConnection: checkConnection,
                ],
                username,
                password,
                props
        )
    }

    static String getAssertedEnvVariable(String varName) {
        String varValue = System.getenv(varName)
        println("VAR: '$varName'" + " VALUE: " + varValue ?: '')
        assert varValue
        return varValue
    }

    static String getURL() { getAssertedEnvVariable("BAMBOO_URL") }
    static String getUsername() { getAssertedEnvVariable("BAMBOO_USERNAME") }
    static String getPassword() { getAssertedEnvVariable("BAMBOO_PASSWORD") }

    static String getResourceName(){
        return 'local'
    }

    def runProcedure(String projectName, String procedureName, Map parameters) {
        // Skip undefined values
        def parametersString = parameters
                .findAll { k, v -> v != null }
                .collect { k, v ->
            v = ((String) v).replace('\'', '\\\'')
            "$k: '''$v'''"
        }.join(', ')

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
            jobCompleted result.jobId
        }
        def logs = getJobLogs(result.jobId)
        def outcome = jobStatus(result.jobId).outcome
        logger.debug("DSL: $dslString")
        logger.debug("Logs: $logs")
        logger.debug("Outcome: $outcome")
        [logs: logs, outcome: outcome, jobId: result.jobId]
    }

    def getStepSummary(def jobId, def stepName) {
        assert jobId
        def summary
        def property = "/myJob/jobSteps/$stepName/summary"
        println "Trying to get the summary for Procedure: checkConnection, property: $property, jobId: $jobId"
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

    def createPluginConfiguration(def configName, Map params, def userName = getUsername(), def password = getPassword()) {
        assert PLUGIN_NAME
        assert configName
        def result = runProcedure("""
            runProcedure(
                projectName: "/plugins/$PLUGIN_NAME/project",
                procedureName: 'CreateConfiguration',
                    credential: [
                    [
                        credentialName: 'credential',
                        userName: '$userName',
                        password: '$password'
                    ],
                ],
                actualParameter: [
                    config           : '${configName}', 
                    desc             : '${params.desc}',
                    endpoint         : '${params.endpoint}',
                    debugLevel       : '${params.debugLevel}',
                    checkConnection  : '${params.checkConnection}',
                    credential       : 'credential',
                ]
            )
            """)

        assert result?.jobId
        waitUntil {
            jobCompleted(result)
        }
        return result
    }

    String getJobStepSummary(String procedureName, String jobId){
       return getJobProperty("/myJob/steps/RunProcedure/steps/$procedureName/summary", jobId)
    }

}