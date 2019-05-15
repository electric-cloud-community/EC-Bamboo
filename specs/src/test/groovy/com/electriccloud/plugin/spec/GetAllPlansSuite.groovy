package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.*
import spock.lang.*

class GetAllPlansSuite extends PluginTestHelper {
    static procedureName = 'GetAllPlans'
    static projectName = "EC-Specs $procedureName"

    static def procedureParams = [
            config: '',
            projectKey : '',
            resultFormat: '',
            resultPropertySheet: ''
    ]

    static bambooProjects = [
            valid     : 'PROJECT',
            unexisting: randomize("UNEXISTING"),
            empty     : ''
    ]

    @Shared
    String config, projectKey, resultFormat, resultPropertySheet

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
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    def "#caseId. GetAllPlans"() {
        given:

        def project = bambooProjects[projectKey]

        def procedureParams = [
                config             : config,
                projectKey         : project,
                resultFormat       : resultFormat,
                resultPropertySheet: resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        assert result.outcome == 'success'

        // Check logs
        assert result.logs =~ /Found project: '$project'/

        // Check properties
        def resultProperty = getJobProperty('response', result.jobId)
        assert resultProperty


        where:
        caseId       | projectKey | resultFormat    | resultPropertySheet
        'CHANGEME_1' | 'empty'    | 'json'          | ''
//        'CHANGEME_2' | 'empty'    | 'propertySheet' | ''
        'CHANGEME_3' | 'valid'    | 'json'          | '/myJob/result'
//        'CHANGEME_4' | 'valid'    | 'propertySheet' | '/myJob/result'
    }

}