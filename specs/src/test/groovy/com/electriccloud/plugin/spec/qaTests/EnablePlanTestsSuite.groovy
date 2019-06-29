package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import groovy.json.JsonSlurper
import spock.lang.IgnoreRest
import spock.lang.Unroll

class EnablePLanTestsSuite extends PluginTestHelper{

    static def procedureName = 'EnablePlan'

    static def projectName = 'TestProject: EnablePlan'

    static def runPlanParams = [
            config             : '',
            projectKey         : '',
            planKey            : '',
    ]

    static def TC = [
            C388135: [ids: 'C388135', description: 'Enable disabled plan'],
            C388136: [ids: 'C388136', description: 'Enable enabled plan'],
            C388137: [ids: 'C388137', description: 'empty config'],
            C388138: [ids: 'C388138', description: 'empty projectKey'],
            C388139: [ids: 'C388139', description: 'empty planKey'],
            C388140: [ids: 'C388137', description: 'wrong config'],
            C388141: [ids: 'C388138', description: 'wrong projectKey'],
            C388142: [ids: 'C388139', description: 'wrong planKey'],


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
//        bambooClient.createPlan('PROJECT', 'QAENABLEPLAN', 'Enable plan')

    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
//        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
//        conditionallyDeleteProject(projectName)
//        bambooClient.deletePlan('PROJECT', 'QAENABLEPLAN')
    }


    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanRuns: Positive #caseId.ids #caseId.description'() {
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

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)


        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace('PLANRUNKEY', lastPlanRunName)}")
        assert jobSummary == expectedSummary.replace('PLANRUNKEY', lastPlanRunName)

        for (def log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $log")
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey))
        }

        where:
        caseId     | configName   | projectKey     | planKey         | expectedOutcome | expectedSummary             | expectedLog
        TC.C388135 | CONFIG_NAME  | 'PROJECT'      | 'QAENABLEPLAN'  | 'success'       | expectedSummaries.default   | expectedLogs.default
    }


}
