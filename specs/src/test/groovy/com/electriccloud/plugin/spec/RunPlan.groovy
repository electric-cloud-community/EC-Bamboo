package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
class RunPlan extends PluginTestHelper {

    static String procedureName = 'RunPlan'
    static String projectName = "EC-Specs $procedureName"

    static def procedureParams = [
            config                  : '',
            projectKey              : '',
            planKey                 : '',
            customRevision          : '',
            additionalBuildVariables: '',
            waitForBuild            : '',
            waitTimeout             : '',
            resultFormat            : '',
            resultPropertySheet     : ''
    ]

    static def bambooProjects = [
            valid     : 'PROJECT',
            unexisting: "__UNEXISTING__",
            empty     : ''
    ]

    static def bambooPlans = [
            valid       : 'PLAN',
            failing     : 'FAIL',
            timeout     : 'LONG',
            parametrized: 'PARAMS',
            unexisting  : "__UNEXISTING__",
            empty       : ''
    ]

    static def revisions = [
            valid     : 'master',
            valid_hash: '9b75c7ffe8a787cf8f5b879638946ed4be29f2b3',
            mailformed: 'MAILFORMED',
            unexisting: 'unexisiting'
    ]

    static def buildParameters = [
            // Here effective is only second parameter, so we can check parser too
            valid              : "bamboo.variable.TEST_MESSAGE=hello\nbamboo.variable.FAIL_MESSAGE=Expected failure",
            valid_flow_fallback: "bamboo.variable.TEST_MESSAGE=hello;#;#;#bamboo.variable.FAIL_MESSAGE=Expected failure",
            timeout            : "bamboo.variable.TEST_MESSAGE=hello\nbamboo.variable.SLEEP_TIME=35"
    ]

    static defaultResultPropertyPath = 'runResult'

    @Shared
    String config
    @Shared
    String projectKey
    @Shared
    String planKey
    @Shared
    String waitForBuild
    @Shared
    String waitTimeout
    @Shared
    String resultFormat
    @Shared
    String resultPropertySheet

    def doSetupSpec() {
        redirectLogs()

        createConfiguration(CONFIG_NAME)

        // Import procedure project
        importProject(projectName, 'dsl/procedure.dsl', [
                projectName  : projectName,
                procedureName: procedureName,
                resourceName : getResourceName(),
                params       : procedureParams,
        ])
    }

    def doCleanupSpec() {
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def "#caseId. RunPlan - Positive"() {
        given:
        def project = bambooProjects['valid']
        def plan = bambooPlans['valid']

        resultPropertySheet = ''
        if (resultPropertyPath) {
            resultPropertySheet = '/myJob/' + resultPropertyPath
        } else {
            resultPropertyPath = defaultResultPropertyPath
        }

        def procedureParams = [
                config             : CONFIG_NAME,
                projectKey         : project,
                planKey            : plan,
                waitForBuild       : waitForBuild,
                resultFormat       : resultFormat,
                resultPropertySheet: resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == 'success'

        // Check logs
        assert result.logs =~ /Build Result Key: $project-$plan-/

        if (resultFormat == 'propertySheet') {
            // Check properties
            def properties = getJobProperties(result.jobId)
            def resultProperties = properties[resultPropertyPath]
            assert resultProperties['planKey'] == project + '-' + plan
            assert resultProperties['finished'] == 'true'
            assert resultProperties['successful'] == 'true'
        }

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        assert outputParameters['buildUrl']
        assert outputParameters['buildResultKey']

        cleanup:
        // We are limited by one concurrent build, so have to wait for previous one to finish
        if (waitForBuild == '0' && result.outcome == 'success') {
            println("Waiting 60 seconds for previous build to finish")
            sleep(60 * 1000)
        }
        where:
        caseId       | waitForBuild | resultFormat    | resultPropertyPath
        'CHANGEME_1' | 0            | 'json'          | ''
        'CHANGEME_2' | 1            | 'propertySheet' | ''
    }

    @Unroll
    def "#caseId. RunPlan - Negative"() {
        given:
        def project = bambooProjects['valid']
        def plan = bambooPlans[planKey]

        def resultFormat = 'none'
        def procedureParams = [
                config                  : CONFIG_NAME,
                projectKey              : project,
                planKey                 : plan,
                customRevision          : '',
                additionalBuildVariables: '',
                waitForBuild            : waitForBuild,
                waitTimeout             : waitTimeout,
                resultFormat            : resultFormat,
                resultPropertySheet     : resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == expectedOutcome
        assert getProcedureJobStepSummary(procedureName, result.jobId) =~ expectedSummary

        where:
        caseId       | planKey      | waitForBuild | waitTimeout | expectedOutcome | expectedSummary
        // Unexisiting plan key
        'CHANGEME_3' | 'unexisting' | 0            | 0           | 'error'         | 'not found'
        // Timeout
        'CHANGEME_4' | 'timeout'    | 1            | 1           | 'error'         | 'Exceeded the wait timeout'
        // Failing
        'CHANGEME_5' | 'failing'    | 1            | 0           | 'warning'       | 'Build was not finished successfully'
    }

    @Unroll
    def "#caseId. RunPlan - Custom revisions"() {
        given:
        def project = bambooProjects['valid']
        def plan = bambooPlans['valid']
        def revision = revisions[customRevision]

        def procedureParams = [
                config                  : CONFIG_NAME,
                projectKey              : project,
                planKey                 : plan,
                customRevision          : revision,
                additionalBuildVariables: '',
                waitForBuild            : 1,
                waitTimeout             : 0,
                resultFormat            : 'none',
                resultPropertySheet     : resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == expectedOutcome
        assert getProcedureJobStepSummary(procedureName, result.jobId) =~ expectedSummary

        where:
        caseId       | customRevision | expectedOutcome | expectedSummary
        'CHANGEME_6' | 'valid'        | 'success'       | 'Build result information saved to the properties'
        'CHANGEME_7' | 'valid_hash'   | 'success'       | 'Build result information saved to the properties'
        'CHANGEME_8' | 'mailformed'   | 'warning'       | 'Build was not started'
        'CHANGEME_9' | 'unexisting'   | 'warning'       | 'Build was not started'
    }

    @Unroll
    def "#caseId. RunPlan - Additional Parameters"() {
        given:
        def project = bambooProjects['valid']
        def plan = bambooPlans['parametrized']

        def additionalBuildVariables = buildParameters[parametersCase]
        assert additionalBuildVariables

        def procedureParams = [
                config                  : CONFIG_NAME,
                projectKey              : project,
                planKey                 : plan,
                customRevision          : '',
                additionalBuildVariables: additionalBuildVariables,
                waitForBuild            : 1,
                waitTimeout             : 15,
                resultFormat            : 'none',
                resultPropertySheet     : resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == expectedOutcome
        assert getProcedureJobStepSummary(procedureName, result.jobId) =~ expectedSummary

        cleanup:
        if (parametersCase == 'timeout') {
            println("Waiting for the build to finish")
            Thread.sleep(30 * 1000)
        }
        where:
        caseId        | parametersCase        | expectedOutcome | expectedSummary
        'CHANGEME_10' | 'valid'               | 'warning'       | 'Build was not finished successfully'
        'CHANGEME_11' | 'valid_flow_fallback' | 'warning'       | 'Build was not finished successfully'
        'CHANGEME_12' | 'timeout'             | 'error'         | 'Exceeded the wait timeout'
    }

}
