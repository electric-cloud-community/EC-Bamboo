package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Unroll

class CreateReleaseSuite extends BambooHelper {
    static procedureName = 'CreateRelease'
    static projectName = "EC-Specs $procedureName"

    static def procedureParams = [
            config               : '',
            deploymentProjectName: '',
            planBuildKey         : '',
            requestReleaseName   : '',
            releaseName          : '',
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

    static String defaultResultPropertyPath = 'release'

    @Shared
    static def buildRunSuccessful

    @Shared
    static String existingVersionName

    String config = CONFIG_NAME

    @Shared
    String deploymentProjectName, planBuildKey, requestReleaseName, releaseName, resultFormat, resultPropertySheet

    def doSetupSpec() {
		createDefaultProject()
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
    def "#caseId. CreateRelease - Sanity - positive"() {
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
                requestReleaseName   : requestReleaseName,
                releaseName          : releaseName,
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
            if (releaseName) {
                assert versionProperties['name'] == releaseName
            }
        }

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        assert outputParameters['release']

        if (releaseName) {
            assert outputParameters['release'] == releaseName
        }

        where:
        caseId       | deploymentProject | requestReleaseName | releaseName           | resultPropertyPath
        'CHANGEME_1' | 'valid'           | 1                  | ''                    | ''
        'CHANGEME_2' | 'valid'           | 0                  | randomize("version-") | 'result'
    }


    @Unroll
    def "#caseId. CreateRelease - Sanity - negative"() {
        given:
        def project = deploymentProjects[deploymentProject]
        deploymentProjectName = project['name']
        if (planBuild == 'valid') {
            planBuildKey = buildRunSuccessful['key']
        } else if (planBuild == 'unexisting') {
            planBuildKey = "PROJECT-PLAN-0"
        }

        if (releaseName == 'existing') {
            if (!existingVersionName) {
                def newVersion = createRelease(
                        deploymentProjects['valid']['name'],
                        (String) buildRunSuccessful['key']
                )
                existingVersionName = newVersion['name']
            }
            releaseName = existingVersionName
        }

        def procedureParams = [
                config               : config,
                deploymentProjectName: deploymentProjectName,
                planBuildKey         : planBuildKey,
                requestReleaseName   : '0',
                releaseName          : releaseName,
                resultFormat         : 'none',
                resultPropertySheet  : ''
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == expectedOutcome
        if (expectedSummary) {
            assert getProcedureJobStepSummary(procedureName, result.jobId) =~ expectedSummary
        }

        where:
        caseId       | deploymentProject | planBuild    | releaseName | expectedOutcome | expectedSummary
        'CHANGEME_3' | 'unexisting'      | 'valid'      | 'valid'     | 'error'         | "Can't find deployment project with name"
        'CHANGEME_4' | 'valid'           | 'unexisting' | 'valid'     | 'warning'       | 'Unable to find result number'
        'CHANGEME_5' | 'valid'           | 'valid'      | 'existing'  | 'warning'       | 'This release version is already in use'
    }


}