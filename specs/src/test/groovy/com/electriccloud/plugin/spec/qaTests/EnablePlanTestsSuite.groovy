package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Unroll

class EnablePlanTestsSuite extends PluginTestHelper{

    static def procedureName = 'EnablePlan'

    static def projectName = 'TestProject: EnablePlan'

    static def runPlanParams = [
            config             : '',
            projectKey         : '',
            planKey            : '',
    ]

    static def TC = [
            C388153: [ids: 'C388153', description: 'Enable disabled plan'],
            C388154: [ids: 'C388154', description: 'Enable enabled plan'],
            C388155: [ids: 'C388155', description: 'empty config'],
            C388156: [ids: 'C388156', description: 'empty projectKey'],
            C388157: [ids: 'C388157', description: 'empty planKey'],
            C388158: [ids: 'C388158', description: 'wrong config'],
            C388159: [ids: 'C388159', description: 'wrong projectKey'],
            C388160: [ids: 'C388160', description: 'wrong planKey'],


    ]

    static def testCaseHelper
    static def bambooClient

    static def expectedSummaries = [
            default:     "Build plan 'PROJECTKEY-PLANKEY' was enabled.",
            error: "Build was not finished successfully",
            noWait: "Build was successfully added to a queue.",
            wrongPoject: "Can't find project by key: wrong",
            notFound: "Plan PROJECTKEY-PLANKEY not found.",
            wrongState: "There is no BuildState called 'wrong'",
            zeroRun: "No results found for plan",
            notStarted: "Build was not started.",
    ]

    static def expectedLogs = [
            default:     'http://bamboo-server:8085/rest/api/latest/plan/PROJECTKEY-PLANKEY/enable',
            defaultError: "Possible exception in logs; check job",
            wrongState: "There is no BuildState called 'wrong'",
            notFound: "Plan \\'PROJECTKEY-PLANKEY\\' was not found",
            zeroRun: "No results found for plan",
    ]


    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: 'EnablePlan', params: runPlanParams]

        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
        bambooClient.createPlan('PROJECT', 'QAENABLEPLAN', 'Enable plan')

    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
        bambooClient.deletePlan('PROJECT', 'QAENABLEPLAN')
    }

    @Sanity
    @Unroll
    def 'GetPlanRuns: Sanity #caseId.ids #caseId.description'() {
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

        assert planInfo.enabled == true

        for (def log in expectedLog) {
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey))
        }

        where:
        caseId     | configName   | projectKey     | planKey         | expectedOutcome | planState | expectedSummary             | expectedLog
        TC.C388153 | CONFIG_NAME  | 'PROJECT'      | 'QAENABLEPLAN'  | 'success'       | 'disabled'| expectedSummaries.default   | expectedLogs.default
    }


    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanRuns: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition('Plan QAENABLEPLAN should exist in project PROJECT')

        if (planState == 'disabled') {
            testCaseHelper.testCasePrecondition('Plan QAENABLEPLAN should be disabled')
            bambooClient.changePlanState(projectKey, planKey, 'disable')
        }
        else{
            testCaseHelper.testCasePrecondition('Plan QAENABLEPLAN should be enabled')
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

        testCaseHelper.addExpectedResult("Bamboo plan should be enabled")
        assert planInfo.enabled == true

        for (def log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $log")
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey))
        }

        where:
        caseId     | configName   | projectKey     | planKey         | expectedOutcome | planState | expectedSummary             | expectedLog
        TC.C388153 | CONFIG_NAME  | 'PROJECT'      | 'QAENABLEPLAN'  | 'success'       | 'disabled'| expectedSummaries.default   | expectedLogs.default
        TC.C388154 | CONFIG_NAME  | 'PROJECT'      | 'QAENABLEPLAN'  | 'success'       | 'enabled' | expectedSummaries.default   | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanRuns: Negative #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition('Plan QAENABLEPLAN should exist in project PROJECT')

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
        caseId     | configName   | projectKey     | planKey         | expectedOutcome | planState | expectedSummary             | expectedLog
        TC.C388155 | ''           | 'PROJECT'      | 'QAENABLEPLAN'  | 'error'         | 'disabled'| null                        | expectedLogs.defaultError
        TC.C388156 | 'wrong'      | 'PROJECT'      | 'QAENABLEPLAN'  | 'error'         | 'disabled'| null                        | expectedLogs.defaultError
        TC.C388157 | CONFIG_NAME  | ''             | 'QAENABLEPLAN'  | 'error'         | 'disabled'| null                        | expectedLogs.defaultError
        TC.C388158 | CONFIG_NAME  | 'PROJECT'      | ''              | 'error'         | 'disabled'| null                        | expectedLogs.defaultError
        TC.C388159 | CONFIG_NAME  | 'WRONG'        | 'QAENABLEPLAN'  | 'error'         | 'disabled'| expectedSummaries.notFound  | expectedLogs.notFound
        TC.C388160 | CONFIG_NAME  | 'PROJECT'      | 'WRONG'         | 'error'         | 'disabled'| expectedSummaries.notFound  | expectedLogs.notFound

    }

}
