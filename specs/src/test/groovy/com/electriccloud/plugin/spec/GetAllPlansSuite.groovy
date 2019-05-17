package com.electriccloud.plugin.spec


import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Unroll

class GetAllPlansSuite extends PluginTestHelper {
    static procedureName = 'GetAllPlans'
    static projectName = "EC-Specs $procedureName"

    static def procedureParams = [
            config             : '',
            projectKey         : '',
            resultFormat       : '',
            resultPropertySheet: ''
    ]

    static bambooProjects = [
            valid     : 'PROJECT',
            unexisting: randomize("UNEXISTING"),
            empty     : ''
    ]

    static defaultResultPropertyPath = 'plans'

    @Shared
    String config, projectKey, resultFormat, resultPropertySheet

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
    def "#caseId. GetAllPlans"() {
        given:
        def project = bambooProjects[projectKey]

        resultPropertySheet = ''
        if (resultPropertyPath) {
            resultPropertySheet = '/myJob/' + resultPropertyPath
        } else {
            resultPropertyPath = defaultResultPropertyPath
        }

        def procedureParams = [
                config             : CONFIG_NAME,
                projectKey         : project,
                resultFormat       : resultFormat,
                resultPropertySheet: resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == 'success'

        // Check logs
        if (project) {
            assert result.logs =~ /Found project: '$project'/
        }

        if (resultFormat == 'propertySheet') {
            // Check properties
            def properties = getJobProperties(result.jobId)
            def resultProperties = properties[resultPropertyPath]
            assert resultProperties['PROJECT-PLAN']['projectKey'] == 'PROJECT'
        }

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        assert outputParameters['planKeys']

        where:
        caseId       | projectKey | resultFormat    | resultPropertyPath
        'CHANGEME_1' | 'empty'    | 'json'          | ''
        'CHANGEME_2' | 'empty'    | 'propertySheet' | ''
        'CHANGEME_3' | 'valid'    | 'propertySheet' | 'result'
    }

}

