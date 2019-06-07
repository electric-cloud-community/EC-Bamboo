package com.electriccloud.plugin.spec

class BambooHelper extends PluginTestHelper {

    static String helperProjectName = 'EC-Bamboo Specs Helper'

    def runPlan(String projectKey, String planKey, def parameters = [:]) {
        def procedureParameters = [
                config             : CONFIG_NAME,
                projectKey         : projectKey,
                planKey            : planKey,
                waitForBuild       : 0,
                waitTimeout        : 300,
                resultFormat       : 'propertySheet',
                resultPropertySheet: '/myJob/result'
        ]

        // Replace the specified parameters
        parameters.each { k, v ->
            procedureParameters[k] = v
        }

        logger.info("Running build for $projectKey-$planKey")
        def result = _runProcedure('RunPlan', procedureParameters)

        if (!parameters['waitForBuild']) {
            logger.info("Waiting 60 seconds for build to finish")
            sleep(60 * 1000)
        }

        assert result.outcome == 'success'

        def properties = getJobProperties(result.jobId)
        return properties['result']
    }

    def getPlanDetails(String projectKey, String planKey, Map parameters = [:]) {
        Map procedureParameters = [
                config             : CONFIG_NAME,
                projectKey         : projectKey,
                planKey            : planKey,
                resultFormat       : 'propertySheet',
                resultPropertySheet: '/myJob/result'
        ]

        // Replace the specified parameters
        parameters.each { k, v ->
            procedureParameters[k] = v
        }

        def result = _runProcedure('GetPlanDetails', procedureParameters)
        assert result.outcome == 'success'

        if (procedureParameters.resultFormat == 'propertySheet'){
            def properties = getJobProperties(result.jobId)
            return properties['result']
        }
        return true
    }

    def enablePlan(String projectKey, String planKey) {
        def result = _runProcedure('EnablePlan', [
                config    : CONFIG_NAME,
                projectKey: projectKey,
                planKey   : planKey,
        ])
        assert result.outcome == 'success'
    }

    def createRelease(String deploymentProjectName, String buildResultKey, String version = randomize('release')) {
        def procedureParams = [
                config               : CONFIG_NAME,
                deploymentProjectName: deploymentProjectName,
                planBuildKey         : buildResultKey,
                requestVersionName   : '',
                versionName          : version,
                resultFormat         : 'propertySheet',
                resultPropertySheet  : '/myJob/result'
        ]

        def result = _runProcedure('CreateRelease', procedureParams)

        def properties = getJobProperties(result.jobId)
        return properties['result']
    }

    def _runProcedure(String procedureName, Map parameters) {
        logger.debug("Running procedure $procedureName with params: " + objectToJson(parameters))
        // Create map for procedure import
        def procedureParams = [:]
        parameters.each { k, v ->
            procedureParams[k] = ''
        }

        // Import project
        importProject(helperProjectName, 'dsl/procedure.dsl', [
                projectName  : helperProjectName,
                procedureName: procedureName,
                resourceName : getResourceName(),
                params       : procedureParams,
        ])

        // Run procedure with params
        def helperProcedureResult = runProcedure(helperProjectName, procedureName, parameters)

        println "HELPER Job Link for $procedureName:" + getJobLink(helperProcedureResult.jobId)

        // Delete procedure from the project to avoid caching
        dsl "deleteProcedure(projectName: '$helperProjectName', procedureName: '$procedureName')"

        return helperProcedureResult
    }

}
