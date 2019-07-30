package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
class TriggerDeploymentSuite extends BambooHelper {

    static String procedureName = 'TriggerDeployment'
    static String projectName = "EC-Specs $procedureName"

    static def procedureParams = [
            config                   : '',
            deploymentProjectName    : '',
            deploymentEnvironmentName: '',
            deploymentReleaseName    : '',
            waitForDeployment        : '',
            waitTimeout              : '',
            resultFormat             : '',
            resultPropertySheet      : ''
    ]

    // All deployment projects are linked to the Valid Plan
    static def bambooDeploymentProjects = [
            valid     : 'Deployment Project',
            long      : 'Long Deployment project',
            failing   : 'Failing deployment project',
            unexisting: randomize('unexisting')
    ]

    // Every deployment project is linked to different environment
    static def bambooEnvironments = [
            'valid'  : 'Stage',
            'long'   : 'Production',
            'failing': 'Production2',
            unexisting: randomize('unexisting')
    ]

    // All deployment projects are linked to successful plan
    static def bambooDeployFromProject = 'PROJECT'
    static def bambooDeployFromPlan = 'PLAN'

    @Shared
    static def successfulBuildRun

    // Caching versions (so version is created once for the deployment project)
    static Map existingVersions = [:]

    static defaultResultPropertyPath = 'deploymentResult'

    @Shared
    String config
    @Shared
    String deploymentProjectName
    @Shared
    String deploymentEnvironmentName
    @Shared
    String deploymentReleaseName
    @Shared
    String waitForDeployment
    @Shared
    String waitTimeout
    @Shared
    String resultFormat
    @Shared
    String resultPropertySheet

    def doSetupSpec() {
		createDefaultProject()
        redirectLogs()

        createConfiguration(CONFIG_NAME)

        // Import procedure project
        importProject(projectName, 'dsl/procedure.dsl', [
                projectName  : projectName,
                procedureName: procedureName,
                resourceName : getResourceName(),
                params       : procedureParams,
        ])

        successfulBuildRun = runPlan(bambooDeployFromProject, bambooDeployFromPlan, [waitForBuild: 1])
    }

    def doCleanupSpec() {
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def "#caseId. TriggerDeployment - Positive"() {
        given:
        deploymentProjectName = bambooDeploymentProjects['valid']
        deploymentEnvironmentName = bambooEnvironments['valid']
        deploymentReleaseName = getVersionNameForDeploymentProject(deploymentProjectName)

        resultPropertySheet = ''
        if (resultPropertyPath) {
            resultPropertySheet = '/myJob/' + resultPropertyPath
        } else {
            resultPropertyPath = defaultResultPropertyPath
        }

        def procedureParams = [
                config                   : CONFIG_NAME,
                deploymentProjectName    : deploymentProjectName,
                deploymentEnvironmentName: deploymentEnvironmentName,
                deploymentReleaseName    : deploymentReleaseName,
                waitForDeployment        : waitForDeployment,
                waitTimeout              : waitTimeout,
                resultFormat             : resultFormat,
                resultPropertySheet      : resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == 'success'

        // Check logs
//        assert result.logs =~ /Build Result Key: $project-$plan-/

        if (resultFormat == 'propertySheet') {
            // Check properties
            def properties = getJobProperties(result.jobId)
            def resultProperties = properties[resultPropertyPath]
            assert resultProperties['lifeCycleState'] == 'FINISHED'
            assert resultProperties['deploymentVersionName'] == deploymentReleaseName
        }

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        assert outputParameters['deploymentResultUrl']
        assert outputParameters['deploymentResultKey']

        cleanup:
        // We are limited by one concurrent build, so have to wait for previous one to finish
        if (waitForDeployment == '0' && result.outcome == 'success') {
            println("Waiting 30 seconds for operation to finish")
            sleep(30 * 1000)
        }
        where:
        caseId       | waitForDeployment | resultFormat    | resultPropertyPath
        'CHANGEME_1' | 0                 | 'json'          | ''
        'CHANGEME_2' | 1                 | 'propertySheet' | 'result'
    }

    @Unroll
    def "#caseId. TriggerDeployment - Negative"() {
        given:
        resultFormat == 'none'
        waitForDeployment = 1

        deploymentProjectName = bambooDeploymentProjects[projectCase]

        if (environmentCase == 'valid') {
            deploymentEnvironmentName = bambooEnvironments[projectCase]
        } else if (environmentCase == 'anotherProject') {
            def anotherEnvCase = (deploymentProjectName == 'failing') ? 'valid' : 'failing'
            deploymentEnvironmentName = bambooEnvironments[anotherEnvCase]
        }

        if (versionCase == 'unexisting' || projectCase == 'unexisting') {
            deploymentReleaseName = randomize("unexisting")
        } else {
            deploymentReleaseName = getVersionNameForDeploymentProject(deploymentProjectName)
        }

        def resultFormat = 'none'
        def procedureParams = [
                config                   : CONFIG_NAME,
                deploymentProjectName    : deploymentProjectName,
                deploymentEnvironmentName: deploymentEnvironmentName,
                deploymentReleaseName    : deploymentReleaseName,
                waitForDeployment        : waitForDeployment,
                waitTimeout              : waitTimeout,
                resultFormat             : resultFormat,
                resultPropertySheet      : resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == expectedOutcome
        assert getProcedureJobStepSummary(procedureName, result.jobId) =~ expectedSummary

        where:
        caseId       | projectCase  | environmentCase  | versionCase  | waitTimeout | expectedOutcome | expectedSummary
        // Failing
        'CHANGEME_3' | 'failing'    | 'valid'          | 'valid'      | 0           | 'warning'       | 'not finished successfully'
        // Timeout
        'CHANGEME_4' | 'long'       | 'valid'          | 'valid'      | 1           | 'error'         | 'Exceeded the wait timeout'
        // Wrong environment
        'CHANGEME_5' | 'valid'      | 'anotherProject' | 'valid'      | 0           | 'error'         | "Can't find environment "
        // Unexisting version
        'CHANGEME_6' | 'valid'      | 'valid'          | 'unexisting' | 0           | 'error'         | "Can't find release "
        // Unexisting project
        'CHANGEME_7' | 'unexisting' | 'valid'          | 'valid'      | 0           | 'error'         | "Can't find deployment project "
    }

    /**
     * Returns cached or new version name for the deployment project
     *
     * @param deploymentProjectName
     * @return String - releaseName
     */
    String getVersionNameForDeploymentProject(String deploymentProjectName) {
        if (existingVersions[deploymentProjectName] == null) {
            existingVersions[deploymentProjectName] = createRelease(deploymentProjectName, (String) successfulBuildRun['key'])
        }

        return existingVersions[deploymentProjectName]['name']
    }
}
