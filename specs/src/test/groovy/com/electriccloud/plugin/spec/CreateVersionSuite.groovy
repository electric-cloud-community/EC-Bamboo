package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Unroll

class CreateVersionSuite extends BambooHelper {
    static procedureName = 'CreateVersion'
    static projectName = "EC-Specs $procedureName"

    static def procedureParams = [
            config               : '',
            deploymentProjectName: '',
            planBuildKey         : '',
            requestVersionName   : '',
            versionName          : '',
            resultFormat         : '',
            resultPropertySheet  : ''
    ]

    static def deploymentProjects = [
            valid     : [
                    name        : 'Deployment Project',
                    id          : '1048577',
                    key         : '1048577',
                    buildProject: 'PROJECT',
                    buildPlan   : 'PLAN',
                    plan        : 'PROJECT-PLAN'
            ],
            unexisting: [
                    name: "__UNEXISTING__"
            ]
    ]

    static String defaultResultPropertyPath = 'version'

    @Shared
    static def buildRunSuccessful
    @Shared
    static String existingVersionName = 'release-1'

    String config = CONFIG_NAME

    @Shared
    String deploymentProjectName, planBuildKey, requestVersionName, versionName, resultFormat, resultPropertySheet

    def doSetupSpec() {
        createConfiguration(CONFIG_NAME)

        // Import procedure project
        importProject(projectName, 'dsl/procedure.dsl', [
                projectName  : projectName,
                procedureName: procedureName,
                resourceName : getResourceName(),
                params       : procedureParams,
        ])

        buildRunSuccessful = runPlan(
                deploymentProjects['valid']['buildProject'],
                deploymentProjects['valid']['buildPlan'],
                [waitForBuild: 1]
        )

    }

    def doCleanupSpec() {
        deleteConfiguration(PLUGIN_NAME, config)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def "#caseId. CreateVersion - Sanity - positive"() {
        given:
        def project = deploymentProjects[deploymentProject]
        deploymentProjectName = project['name']
        planBuildKey = buildRunSuccessful['key']

        resultPropertySheet = ''
        if (resultPropertyPath) {
            resultPropertySheet = '/myJob/' + resultPropertyPath
        } else {
            resultPropertyPath = defaultResultPropertyPath
        }

        def procedureParams = [
                config               : config,
                deploymentProjectName: deploymentProjectName,
                planBuildKey         : planBuildKey,
                requestVersionName   : requestVersionName,
                versionName          : versionName,
                resultFormat         : resultFormat,
                resultPropertySheet  : resultPropertySheet
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == 'success'

        // Check properties
        if (resultFormat == 'propertySheet') {
            def properties = getJobProperties(result.jobId)
            def versionProperties = properties[resultPropertyPath]
            if (versionName) {
                assert versionProperties['name'] == versionName
            }
        }

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        assert outputParameters['version']

        if (versionName) {
            assert outputParameters['version'] == versionName
        }

        where:
        caseId       | deploymentProject | requestVersionName | versionName           | resultPropertyPath
        'CHANGEME_1' | 'valid'           | 1                  | ''                    | ''
        'CHANGEME_2' | 'valid'           | 0                  | randomize("version-") | 'result'
    }


    @Unroll
    def "#caseId. CreateVersion - Sanity - negative"() {
        given:
        def project = deploymentProjects[deploymentProject]
        deploymentProjectName = project['name']
        if (planBuild == 'valid') {
            planBuildKey = buildRunSuccessful['key']
        }
        else if (planBuild == 'unexisting'){
            planBuildKey = "PROJECT-PLAN-9999"
        }

        def procedureParams = [
                config               : config,
                deploymentProjectName: deploymentProjectName,
                planBuildKey         : planBuildKey,
                requestVersionName   : '0',
                versionName          : versionName,
                resultFormat         : 'none',
                resultPropertySheet  : ''
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
        caseId       | deploymentProject | planBuild    | versionName         | expectedOutcome | expectedSummary
        'CHANGEME_1' | 'unexisting'      | 'valid'      | ''                  | 'error'         | ''
        'CHANGEME_2' | 'valid'           | 'valid'      | existingVersionName | 'warning'       | 'This release version is already in use'
        'CHANGEME_3' | 'unexisting'      | 'unexisting' | ''                  | 'error'         | ''
    }


}