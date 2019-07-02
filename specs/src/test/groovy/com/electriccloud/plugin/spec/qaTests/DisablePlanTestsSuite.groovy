package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Unroll

class DisablePlanTestsSuite extends PluginTestHelper{

    static def procedureName = 'DisablePlan'

    static def projectName = 'TestProject: DisablePlan'

    static def runPlanParams = [
            config             : '',
            projectKey         : '',
            planKey            : '',
    ]

    static def TC = [
            C388161: [ids: 'C388161', description: 'DisablePlan enabled plan'],
            C388162: [ids: 'C388162', description: 'DisablePlan disabled plan'],
            C388163: [ids: 'C388163', description: 'empty config'],
            C388164: [ids: 'C388164', description: 'empty projectKey'],
            C388165: [ids: 'C388165', description: 'empty planKey'],
            C388166: [ids: 'C388166', description: 'wrong config'],
            C388167: [ids: 'C388167', description: 'wrong projectKey'],
            C388168: [ids: 'C388168', description: 'wrong planKey'],


    ]

    static def testCaseHelper
    static def bambooClient

    static def expectedSummaries = [
            default:     "Build plan 'PROJECTKEY-PLANKEY' was disabled.",
            error: "Build was not finished successfully",
            noWait: "Build was successfully added to a queue.",
            wrongPoject: "Can't find project by key: wrong",
            notFound: "Plan PROJECTKEY-PLANKEY not found.",
            wrongState: "There is no BuildState called 'wrong'",
            zeroRun: "No results found for plan",
            notStarted: "Build was not started.",
    ]

    static def expectedLogs = [
            default:     ['http://bamboo-server:8085/rest/api/latest/plan/PROJECTKEY-PLANKEY/enable'],
            defaultError: ["Possible exception in logs; check job"],
            wrongState: ["There is no BuildState called 'wrong'"],
            notFound: ["Plan PROJECTKEY-PLANKEY not found"],
            zeroRun: ["No results found for plan"],
    ]


    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: 'DisablePlan', params: runPlanParams]

        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
        bambooClient.createPlan('PROJECT', 'QADISABLEPLAN', 'Disabled plan')

    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
        bambooClient.deletePlan('PROJECT', 'QADISABLEPLAN')
    }

    @Sanity
    @Unroll
    def 'Sanity #caseId.ids #caseId.description'() {

        if (planState == 'disabled') {
            bambooClient.changePlanState(projectKey, planKey, 'disable')
        }

        given: "Tests parameters for procedure"
        def runParams = [
                config                  : configName,
                projectKey              : projectKey,
                planKey                 : planKey,
        ]

        when: "Run procedure"

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def planInfo = bambooClient.getPlanDetails(projectKey, planKey)

        then: "Verify results"
        assert result.outcome == expectedOutcome

        assert jobSummary == expectedSummary
                .replace("PROJECTKEY", projectKey)
                .replace("PLANKEY", planKey)

        assert planInfo.enabled == false

        for (def log in expectedLog) {
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey))
        }

        where:
        caseId     | configName   | projectKey     | planKey          | expectedOutcome | planState | expectedSummary             | expectedLog
        TC.C388161 | CONFIG_NAME  | 'PROJECT'      | 'QADISABLEPLAN'  | 'success'       | 'enabled' | expectedSummaries.default   | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'DisablePlan: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition('Plan QADISABLEPLAN should exist in project PROJECT')

        if (planState == 'disabled') {
            testCaseHelper.testCasePrecondition('Plan QADISABLEPLAN should be enabled')
            bambooClient.changePlanState(projectKey, planKey, 'disable')
        }
        else{
            testCaseHelper.testCasePrecondition('Plan QADISABLEPLAN should be disabled')
        }

        given: "Tests parameters for procedure"
        def runParams = [
                config                  : configName,
                projectKey              : projectKey,
                planKey                 : planKey,
        ]

        when: "Run procedure"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def planInfo = bambooClient.getPlanDetails(projectKey, planKey)

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace("PROJECTKEY", projectKey).replace("PLANKEY", planKey)}")
        assert jobSummary == expectedSummary
                .replace("PROJECTKEY", projectKey)
                .replace("PLANKEY", planKey)

        testCaseHelper.addExpectedResult("Bamboo plan should be disabled")
        assert planInfo.enabled == false

        for (def log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $log")
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey))
        }

        where:
        caseId     | configName   | projectKey     | planKey          | expectedOutcome | planState | expectedSummary             | expectedLog
        TC.C388161 | CONFIG_NAME  | 'PROJECT'      | 'QADISABLEPLAN'  | 'success'       | 'enabled' | expectedSummaries.default   | expectedLogs.default
        TC.C388162 | CONFIG_NAME  | 'PROJECT'      | 'QADISABLEPLAN'  | 'success'       | 'disabled'| expectedSummaries.default   | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'DisablePlan: Negative #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition('Plan QADISABLEPLAN should exist in project PROJECT')

        given: "Tests parameters for procedure"
        def runParams = [
                config                  : configName,
                projectKey              : projectKey,
                planKey                 : planKey,
        ]

        when: "Run procedure"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        if (expectedSummary) {
            testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace("PROJECTKEY", projectKey).replace("PLANKEY", planKey)}")
            assert jobSummary == expectedSummary
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey)
        }


        for (def log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $log")
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey))
        }

        where:
        caseId     | configName   | projectKey     | planKey          | expectedOutcome | planState | expectedSummary             | expectedLog
        TC.C388163 | ''           | 'PROJECT'      | 'QADISABLEPLAN'  | 'error'         | 'disabled'| null                        | expectedLogs.defaultError
        TC.C388164 | 'wrong'      | 'PROJECT'      | 'QADISABLEPLAN'  | 'error'         | 'disabled'| null                        | expectedLogs.defaultError
        TC.C388165 | CONFIG_NAME  | ''             | 'QADISABLEPLAN'  | 'error'         | 'disabled'| null                        | expectedLogs.defaultError
        TC.C388166 | CONFIG_NAME  | 'PROJECT'      | ''               | 'error'         | 'disabled'| null                        | expectedLogs.defaultError
        TC.C388167 | CONFIG_NAME  | 'WRONG'        | 'QADISABLEPLAN'  | 'error'         | 'disabled'| expectedSummaries.notFound  | expectedLogs.notFound
        TC.C388168 | CONFIG_NAME  | 'PROJECT'      | 'WRONG'          | 'error'         | 'disabled'| expectedSummaries.notFound  | expectedLogs.notFound

    }

}
