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
                resultFormat       : 'none',
                resultPropertySheet: ''
        ]

        // Replace the specified parameters
        parameters.each { k, v ->
            procedureParameters[k] = v
        }

        println("Running build for $projectKey-$planKey")
        def result = _runProcedure('RunPlan', procedureParameters)

        if (parameters['waitForBuild']) {
            println("Waiting 60 seconds for build to finish")
            sleep(60 * 1000)
        }

        assert result.outcome == 'success'

        return result
    }

    def getPlanRuns(String projectKey, String planKey, Map parameters = [:]) {
        Map procedureParameters = [
                config             : CONFIG_NAME,
                projectKey         : projectKey,
                planKey            : planKey,
                buildState         : 'All',
                resultFormat       : 'propertySheet',
                resultPropertySheet: '/myJob/result'
        ]

        // Replace the specified parameters
        parameters.each { k, v ->
            procedureParameters[k] = v
        }

        def result = _runProcedure('RunPlan', procedureParameters)
        assert result.outcome == 'success'
        def properties = getJobProperties(result.jobId)
        return properties['result']
    }

    def enablePlan(String projectKey, String planKey) {
        def result = _runProcedure('EnablePlan', [
                config    : CONFIG_NAME,
                projectKey: projectKey,
                planKey   : planKey,
        ])
        assert result.outcome == 'success'
    }

    def _runProcedure(String procedureName, Map parameters) {
        println("Running procedure $procedureName with params: " + objectToJson(parameters))
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
        def result = runProcedure(helperProjectName, procedureName, parameters)

        // Delete procedure from the project to avoid caching
        dsl "deleteProcedure(projectName: '$helperProjectName', procedureName: '$procedureName')"

        return result
    }

}
