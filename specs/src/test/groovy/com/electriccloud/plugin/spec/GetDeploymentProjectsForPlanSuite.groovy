package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Unroll

class GetDeploymentProjectsForPlanSuite extends PluginTestHelper {
    static procedureName = 'GetDeploymentProjectsForPlan'
    static projectName = "EC-Specs GetDeploymentProjectsForPlan"

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
            valid            : 'PLAN',
            failing          : 'FAIL',
            timeout          : 'LONG',
            parametrized     : 'PARAMS',
            unexisting       : "__UNEXISTING__",
            empty            : '',
            withoutDeployment: 'FAIL'
    ]

    static def deploymentProjects = [
            'Deployment Project': [
                    id  : '1048577',
                    key : '1048577',
                    plan: 'PROJECT-PLAN'
            ]
    ]

    static defaultResultPropertyPath = 'deploymentProjects'

    String config = CONFIG_NAME

    @Shared
    String projectKey, planKey, resultFormat, resultPropertySheet

    @Shared
    String resultPropertyPath, projectCase, planCase

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
    def "#caseId. GetDeploymentProjectsForPlan - Sanity - positive"() {
        given:

        projectKey = bambooProjects['valid']
        planKey = bambooPlans['valid']

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



        // Check properties
        if (resultFormat == 'propertySheet') {

            def properties = getJobProperties(result.jobId)
            def projects = properties[resultPropertyPath]

            // Check we have all deployment projects for plan
            deploymentProjects.each({ name, deploymentProps ->
                String planKey = projectKey + '-' + planKey
                if (deploymentProps['plan'] != planKey) {
                    return true
                }
                assert result.logs =~ /Found deployment project: $name/
                assert projects[deploymentProps['key']]
            })
        }
        else {
            // Check we have all deployment projects for plan
            deploymentProjects.each({ name, deploymentProps ->
                String planKey = projectKey + '-' + planKey
                if (deploymentProps['plan'] != planKey) {
                    return true
                }
                assert result.logs =~ /Found deployment project: $name/
            })
        }

        // Check output parameters
        def outputParameters = getJobOutputParameters(result.jobId, 1)
        assert outputParameters['deploymentProjectKeys']

        where:
        caseId       | resultFormat    | resultPropertyPath
        'CHANGEME_1' | 'json'          | ''
        'CHANGEME_2' | 'propertySheet' | 'result'
    }

    @Unroll
    def "#caseId. GetDeploymentProjectsForPlan - Sanity - negative"() {
        given:

        projectKey = bambooProjects[projectCase]
        planKey = bambooPlans[planCase]

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
        assert result.outcome == expectedOutcome
        assert getJobStepSummary(procedureName, result.jobId) =~ expectedSummary

        where:
        caseId       | projectCase | planCase            | expectedOutcome | expectedSummary
        'CHANGEME_3' | 'valid'     | 'withoutDeployment' | 'warning'       | 'No deployment projects found for '
        'CHANGEME_4' | 'valid'     | 'unexisting'        | 'warning'       | 'No deployment projects found for'
    }

}