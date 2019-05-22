package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Unroll

class GetPlanDetailsSuite extends PluginTestHelper {
    static procedureName = 'GetPlanDetails'
    static projectName = "EC-Specs GetPlanDetails"

    static def procedureParams = [
            config             : '',
            projectKey         : '',
            planKey            : '',
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

    static defaultResultPropertyPath = 'plan'

    // Procedure parameters
    String config = CONFIG_NAME
    @Shared
    String projectKey, planKey, resultFormat, resultPropertySheet

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
    }

    def doCleanupSpec() {
        deleteConfiguration(PLUGIN_NAME, config)
        conditionallyDeleteProject(projectName)
    }


    @Sanity
    @Unroll
    def "#caseId. GetPlanDetails - Sanity - positive"() {
        given:

        projectKey = bambooProjects[project]
        planKey = bambooPlans[plan]


        if (!resultPropertyPath) {
            resultPropertyPath = defaultResultPropertyPath
        } else {
            resultPropertySheet = '/myJob/' + resultPropertyPath
        }

        def procedureParams = [
                config             : config,
                projectKey         : projectKey,
                planKey            : planKey,
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
            assert resultProperties['projectKey'] == projectKey
            assert resultProperties['key'] == projectKey + '-' + planKey
        }

        where:
        caseId       | config      | project | plan      | resultFormat    | resultPropertyPath
        'CHANGEME_1' | CONFIG_NAME | 'valid' | 'valid'   | 'json'          | ''
        'CHANGEME_2' | CONFIG_NAME | 'valid' | 'valid'   | 'propertySheet' | ''
        'CHANGEME_3' | CONFIG_NAME | 'valid' | 'timeout' | 'propertySheet' | 'result'
    }

    @Unroll
    def "#caseId. GetPlanDetails - Sanity - negative"() {
        given:
        resultFormat = 'none'
        resultPropertyPath = ''

        def procedureParams = [
                config             : CONFIG_NAME,
                projectKey         : projectKey,
                planKey            : planKey,
                resultFormat       : resultFormat,
                resultPropertySheet: resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == expectedOutcome

        if (expectedSummary){
            assert getJobStepSummary(procedureName, result.jobId) =~ expectedSummary
        }

        where:
        caseId       | projectKey                   | planKey                   | expectedOutcome | expectedSummary
        'CHANGEME_1' | bambooProjects['valid']      | bambooPlans['empty']      | 'error'         | ""
        'CHANGEME_2' | bambooProjects['valid']      | bambooPlans['unexisting'] | 'warning'       | "Plan '$projectKey-$planKey' was not found"
        'CHANGEME_2' | bambooProjects['unexisting'] | bambooPlans['valid']      | 'warning'       | "Plan '$projectKey-$planKey' was not found"
    }

}