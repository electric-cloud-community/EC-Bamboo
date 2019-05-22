package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
class GetPlanRunsSuite extends BambooHelper {
    static procedureName = 'GetPlanRuns'
    static projectName = "EC-Specs GetPlanRuns"

    static def procedureParams = [
            config             : '',
            projectKey         : '',
            planKey            : '',
            buildState         : '',
            resultFormat       : '',
            resultPropertySheet: ''
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

    static defaultResultPropertyPath = 'planRuns'

    // Procedure parameters
    String config = CONFIG_NAME
    @Shared
    String projectKey, planKey, buildState, resultFormat, resultPropertySheet

    // Test variables
    @Shared
    String resultPropertyPath

    def doSetupSpec() {
        createConfiguration(CONFIG_NAME)

        // Import procedure project
        importProject(projectName, 'dsl/procedure.dsl', [
                projectName  : projectName,
                procedureName: procedureName,
                resourceName : getResourceName(),
                params       : procedureParams,
        ])

        // Need at least one run in this plan
        // TODO: deal with the timeout issues
//        runPlan(bambooProjects['valid'], bambooPlans['valid'], [waitForBuild: 1])
    }

    def doCleanupSpec() {
        deleteConfiguration(PLUGIN_NAME, config)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def "#caseId. GetPlanRuns - Sanity - positive"() {
        given:

        projectKey = bambooProjects['valid']
        planKey = bambooPlans['valid']

        if (!resultPropertyPath) {
            resultPropertyPath = defaultResultPropertyPath
        } else {
            resultPropertySheet = '/myJob/' + resultPropertyPath
        }

        def procedureParams = [
                config             : CONFIG_NAME,
                projectKey         : projectKey,
                planKey            : planKey,
                buildState         : 'All',
                resultFormat       : resultFormat,
                resultPropertySheet: resultPropertySheet
        ]


        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == 'success'

        if (resultFormat == 'propertySheet') {
            // Check properties
            def properties = getJobProperties(result.jobId)
            def resultProperties = properties[resultPropertyPath]
            assert resultProperties['keys'] =~ projectKey
        }

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        assert outputParameters['resultKeys'] =~ projectKey
        assert outputParameters['latestResultKey'] =~ projectKey

        where:
        caseId       | resultFormat    | resultPropertyPath
        'CHANGEME_1' | 'json'          | ''
        'CHANGEME_2' | 'propertySheet' | 'result'
    }

    @Unroll
    def "#caseId. GetPlanRuns - Sanity - negative"() {
        given:
        resultFormat = 'none'
        resultPropertyPath = ''

        def procedureParams = [
                config             : CONFIG_NAME,
                projectKey         : projectKey,
                planKey            : planKey,
                buildState         : buildState,
                resultFormat       : resultFormat,
                resultPropertySheet: resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == expectedOutcome

        if (expectedSummary) {
            assert getJobStepSummary(procedureName, result.jobId) =~ expectedSummary
        }

        where:
        caseId       | projectKey                   | planKey                   | buildState   | expectedOutcome | expectedSummary
        'CHANGEME_2' | bambooProjects['valid']      | bambooPlans['unexisting'] | 'All'        | 'warning'       | "Plan '$projectKey-$planKey' was not found"
        'CHANGEME_2' | bambooProjects['unexisting'] | bambooPlans['valid']      | 'All'        | 'warning'       | "Plan '$projectKey-$planKey' was not found"
        'CHANGEME_2' | bambooProjects['valid']      | bambooPlans['failing']    | 'Successful' | 'warning'       | "No results found for plan"
    }

}