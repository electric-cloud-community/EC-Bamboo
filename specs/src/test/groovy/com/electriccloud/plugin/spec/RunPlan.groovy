package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*

@Stepwise
class RunPlan extends PluginTestHelper {

    static String procedureName = 'RunPlan'
    static String projectName = "EC-Specs $procedureName"

    static def procedureParams = [
            config             : '',
            projectKey         : '',
            planKey            : '',
            waitForBuild       : '',
            waitTimeout        : '',
            resultFormat       : '',
            resultPropertySheet: ''
    ]

    static bambooProjects = [
            valid     : 'PROJECT',
            unexisting: "__UNEXISTING__",
            empty     : ''
    ]

    static bambooPlans = [
            valid     : 'PLAN',
            failing   : 'FAILING',
            unexisting: "__UNEXISTING__",
            empty     : ''
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
                waitTimeout        : '',
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

    @Sanity
    @Unroll
    def "#caseId. RunPlan - Negative"() {
        given:
        def project = bambooProjects[projectKey]
        def plan = bambooPlans[planKey]

        def resultFormat = 'propertySheet'
        def resultPropertyPath = defaultResultPropertyPath

        def procedureParams = [
                config             : CONFIG_NAME,
                projectKey         : project,
                planKey            : plan,
                waitForBuild       : waitForBuild,
                waitTimeout        : waitTimeout,
                resultFormat       : resultFormat,
                resultPropertySheet: resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == expectedOutcome
        assert getJobProperty("/myJob/steps/RunProcedure/steps/$procedureName/summary", result.jobId) =~ expectedSummary
//        assert getJobUpperStepSummary(result.jobId) =~ expectedSummary

        // TODO: Check logs
//        if (project) {
//            assert result.logs =~ /Found project: '$project'/
//        }


        where:
        caseId       | projectKey | planKey      | waitForBuild | waitTimeout | expectedOutcome | expectedSummary
        // Unexisiting plan key
        'CHANGEME_3' | 'valid'    | 'unexisting' | 0            | ''          | 'error'         | 'not found'
        // Timeout
        'CHANGEME_4' | 'valid'    | 'valid'      | 1            | 1           | 'warning'       | 'Exceeded the wait timeout'
        // TODO: run build that will fail
//        'CHANGEME_4' | 'valid'    | 'failing'       | 1            | 300           | 'warning'       | 'bla-bla-bla'
    }

}
