package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import spock.lang.Unroll

class CreateReleaseTestSuite extends PluginTestHelper{

    static def procedureName = 'CreateRelease'

    static def projectName = 'TestProject: CreateRelease'

    static def runParams = [
            config: "",
            deploymentProjectName: "",
            planBuildKey: "",
            releaseName: "",
            requestReleaseName: "",
            resultFormat: "",
            resultPropertySheet: "",
    ]

    static def TC = [
            C388198: [ids: 'C388198', description: 'GetDeploymentProjectsForPlan json format'],
            C388199: [ids: 'C388199', description: 'GetDeploymentProjectsForPlan propertySheet format'],
            C388200: [ids: 'C388200', description: 'GetDeploymentProjectsForPlan don`t save result'],
            C388201: [ids: 'C388201', description: 'GetDeploymentProjectsForPlan empty plan'],

    ]

    static def testCaseHelper
    static def bambooClient

    static def expectedSummaries = [
            default:     "Deployment projects info saved to property(ies).",
    ]

    static def expectedLogs = [
            default:     'Request URI: http://bamboo-server:8085/rest/api/latest/deploy/project/forPlan?planKey=PROJECTKEY-PLANKEY',
            defaultError: "Possible exception in logs; check job",
            wrongFormat: "Wrong Result Property Format provided. Has to be one of 'none', 'propertySheet', 'json'",
    ]

    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: procedureName, params: runParams]

        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'CreateReleaseTestSuite: Negative #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)

        given: "Tests parameters for procedure"
        def runParams = [
                config: configName,
                deploymentProjectName: deploymentProjectName,
                planBuildKey: planBuildKey,
                releaseName: releaseName,
                requestReleaseName: requestReleaseName,
                resultFormat: resultFormat,
                resultPropertySheet: resultPropertySheet,
        ]

        when: "Run procedure TriggerDeployment"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        where:
        caseId     | configName   | deploymentProjectName | planBuildKey | releaseName | requestReleaseName | resultFormat | resultPropertySheet | expectedOutcome | expectedSummary             | expectedLog
        TC.C388191 | CONFIG_NAME  | ''                    | ''           | ''          | ''                 | ''           | ''                  |  'error'        | null                        | expectedLogs.defaultError
    }

}
