package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*
import groovy.json.JsonSlurper


class GetPlanDetailsTestSuite extends PluginTestHelper{

    static def procedureName = 'GetPlanDetails'
    static def projectName = 'TestProject: GetPlanDetails'
    static def getAllPlansParams = [
            config : '',
            planKey : '',
            projectKey : '',
            resultFormat : '',
            resultPropertySheet : '',
    ]

    static def TC = [
            C388101: [ids: 'C388101', description: 'GetPlanDetails, Plan has 1 stage, property format: json'],
            C388102: [ids: 'C388102', description: 'GetPlanDetails, Plan has 1 stage, property format: propertySheet'],
            C388103: [ids: 'C388103', description: 'GetPlanDetails, Plan has 2 stages, property format: propertySheet'],
            C388104: [ids: 'C388104', description: 'GetPlanDetails, Plan without stages, property format: propertySheet'],
            C388105: [ids: 'C388105', description: 'GetPlanDetails, Plan has 2 stages, property format: json'],
            C388106: [ids: 'C388106', description: 'GetPlanDetails, Plan without stages, property format: json'],
            C388107: [ids: 'C388107', description: 'empty config'],
            C388108: [ids: 'C388108', description: 'empty project key'],
            C388109: [ids: 'C388109', description: 'empty plan key'],
            C388110: [ids: 'C388110', description: 'wrong config'],
            C388111: [ids: 'C388111', description: 'wrong project key'],
            C388112: [ids: 'C388112', description: 'wrong plan key'],
            C388113: [ids: 'C388113', description: 'wrong result format'],
    ]

    static def testCaseHelper
    static def bambooClient

    static def expectedSummaries = [
            default:     "Received info about PROJECTKEY-PLANKEY.",
            wrongPoject: "Can't find project by key: wrong",
            notFound: "Plan 'PROJECTKEY-PLANKEY' was not found",
    ]

    static def expectedLogs = [
            default:     ["http://bamboo-server:8085/rest/api/latest/plan/PROJECTKEY-PLANKEY?expand=stages.stage", "Found plan: 'PROJECTKEY-PLANKEY'"],
            defaultError: "Possible exception in logs; check job",
            notFound: "Plan \\'PROJECTKEY-PLANKEY\\' was not found"


    ]

    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
        bambooClient.createPlan('PROJECT', 'QA0', 'Plan without stages', 0)
        bambooClient.createPlan('PROJECT', 'QA2', 'Plan with some (2) stages', 2)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: procedureName, params: getAllPlansParams]

    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        bambooClient.deletePlan('PROJECT', 'QA0')
        bambooClient.deletePlan('PROJECT', 'QA2')
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def 'GetPlanDetails: Sanity #caseId.ids #caseId.description'() {
        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config : configName,
                projectKey : projectKey,
                planKey: planKey,
                resultFormat : resultFormat,
                resultPropertySheet : resultPropertySheet,
        ]

        when: "Run procedure"

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        // procedure doesn't use all fields from response
        // and save only part of them:
        // <-------------------->
        def plansInfo = bambooClient.getPlanDetails(projectKey, planKey)
        plansInfo.remove('expand')
        plansInfo.remove('project')
        plansInfo.remove('shortKey')
        plansInfo.url = plansInfo.link.href.replace(commanderAddress, 'bamboo-server')
        if (!plansInfo.description) {
            plansInfo.description = null
        }
        plansInfo.remove('link')
        plansInfo.remove('isFavourite')
        plansInfo.remove('isActive')
        plansInfo.remove('actions')
        plansInfo.planDescription = plansInfo.description
        plansInfo.remove('description')
        plansInfo.averageBuildTimeInSeconds = Math.round(plansInfo.averageBuildTimeInSeconds)
        plansInfo.stagesSize = plansInfo.stages.size
        if (!plansInfo.planDescription && resultFormat == 'propertySheet'){
            plansInfo.remove('planDescription')
        }

        if (plansInfo.stages.size) {
            plansInfo.stageNames = plansInfo.stages.stage.collect { it.name }

            if (resultFormat == 'propertySheet') {
                plansInfo.stageNames = plansInfo.stageNames.collect{ "'$it'"}.join(', ')
                def tmpStages = plansInfo.stages.stage
                def stageNumber = 0
                plansInfo.stages = [:]
                for (def stage in tmpStages) {
                    plansInfo.stages[stageNumber.toString()] = [:]
                    if (stage.description) {
                        plansInfo.stages[stageNumber.toString()].stageDescription  = stage.description
                    }
                    plansInfo.stages[stageNumber.toString()].name = stage.name

                    stageNumber++
                }
                plansInfo.stages.count = plansInfo.stagesSize
            }

            if (resultFormat == 'json') {
                plansInfo.stageNames = plansInfo.stageNames.collect { "'$it'"}.join(', ')
                plansInfo.stages = plansInfo.stages.stage.collect {
                    [name: it.name, stageDescription: it.description]
                }
            }
        }
        if (plansInfo.stages.size == 0){
            plansInfo.remove('stages')
            plansInfo.remove('size')
        }

        plansInfo.remove('branches')
        plansInfo.remove('planKey')
        plansInfo.remove('variableContext')
        // <-------------------->

        def propertyName = resultPropertySheet.split("/")[2]

        then: "Verify results"
        assert result.outcome == 'success'

        assert jobSummary == expectedSummary
                .replace('PROJECTKEY', projectKey)
                .replace('PLANKEY', planKey)


        if (resultFormat == 'json') {
            assert new JsonSlurper().parseText(jobProperties[propertyName]) == plansInfo
        }

        if (resultFormat == 'propertySheet') {
            assertRecursively(plansInfo, jobProperties[propertyName])
        }

        for (log in expectedLog) {
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey))
        }

        cleanup:

        where:
        caseId     | configName   | projectKey     | planKey | resultFormat    | resultPropertySheet  | expectedSummary             | expectedLog
        TC.C388101 | CONFIG_NAME  | 'PROJECT'      | 'PLAN'  | 'json'          | '/myJob/plan'        | expectedSummaries.default   | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanDetails: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition('QA0 - plan without stages')
        testCaseHelper.testCasePrecondition('PLAN - plan which contains 1 stage')
        testCaseHelper.testCasePrecondition('QA2 - plan which contains 2 stages')

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config : configName,
                projectKey : projectKey,
                planKey: planKey,
                resultFormat : resultFormat,
                resultPropertySheet : resultPropertySheet,
        ]

        when: "Run procedure"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)
        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        // procedure doesn't use all fields from response
        // and save only part of them:
        // <-------------------->
        def plansInfo = bambooClient.getPlanDetails(projectKey, planKey)
        plansInfo.remove('expand')
        plansInfo.remove('project')
        plansInfo.remove('shortKey')
        plansInfo.url = plansInfo.link.href.replace(commanderAddress, 'bamboo-server')
        if (!plansInfo.description) {
            plansInfo.description = null
        }
        plansInfo.remove('link')
        plansInfo.remove('isFavourite')
        plansInfo.remove('isActive')
        plansInfo.remove('actions')
        plansInfo.planDescription = plansInfo.description
        plansInfo.remove('description')
        plansInfo.averageBuildTimeInSeconds = Math.round(plansInfo.averageBuildTimeInSeconds)
        plansInfo.stagesSize = plansInfo.stages.size
        if (!plansInfo.planDescription && resultFormat == 'propertySheet'){
            plansInfo.remove('planDescription')
        }

        if (plansInfo.stages.size) {
            plansInfo.stageNames = plansInfo.stages.stage.collect { it.name }

            if (resultFormat == 'propertySheet') {
                plansInfo.stageNames = plansInfo.stageNames.collect{ "'$it'"}.join(', ')
                def tmpStages = plansInfo.stages.stage
                def stageNumber = 0
                plansInfo.stages = [:]
                for (def stage in tmpStages) {
                    plansInfo.stages[stageNumber.toString()] = [:]
                    if (stage.description) {
                        plansInfo.stages[stageNumber.toString()].stageDescription = stage.description
                    }
                    plansInfo.stages[stageNumber.toString()].name = stage.name

                    stageNumber++
                }
                plansInfo.stages.count = plansInfo.stagesSize
            }

            if (resultFormat == 'json') {
                plansInfo.stageNames = plansInfo.stageNames.collect { "'$it'"}.join(', ')
                plansInfo.stages = plansInfo.stages.stage.collect {
//                    it.description ? [name: it.name, stageDescription: it.description] : [name: it.name]
                    [name: it.name, stageDescription: it.description]

                }
            }
        }
        if (plansInfo.stages.size == 0){
            plansInfo.remove('stages')
            plansInfo.remove('size')
        }

        plansInfo.remove('branches')
        plansInfo.remove('planKey')
        plansInfo.remove('variableContext')
        // <-------------------->

        def propertyName = resultPropertySheet.split("/")[2]

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: success")
        assert result.outcome == 'success'

        testCaseHelper.addExpectedResult("Job Summary: $expectedSummary")
        assert jobSummary == expectedSummary
                .replace('PROJECTKEY', projectKey)
                .replace('PLANKEY', planKey)


        if (resultFormat == 'json') {
            testCaseHelper.addExpectedResult("Job property $propertyName: $plansInfo")
            assert new JsonSlurper().parseText(jobProperties[propertyName]) == plansInfo
        }

        if (resultFormat == 'propertySheet') {
            assertRecursively(plansInfo, jobProperties[propertyName])
        }

        for (log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs: $log")
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey))
        }

        cleanup:

        where:
        caseId     | configName   | projectKey     | planKey | resultFormat    | resultPropertySheet  | expectedSummary             | expectedLog
        TC.C388101 | CONFIG_NAME  | 'PROJECT'      | 'PLAN'  | 'json'          | '/myJob/plan'        | expectedSummaries.default   | expectedLogs.default
        TC.C388102 | CONFIG_NAME  | 'PROJECT'      | 'PLAN'  | 'propertySheet' | '/myJob/plan'        | expectedSummaries.default   | expectedLogs.default
        TC.C388103 | CONFIG_NAME  | 'PROJECT'      | 'QA2'   | 'propertySheet' | '/myJob/plan'        | expectedSummaries.default   | expectedLogs.default
        TC.C388104 | CONFIG_NAME  | 'PROJECT'      | 'QA0'   | 'propertySheet' | '/myJob/plan'        | expectedSummaries.default   | expectedLogs.default
        TC.C388105 | CONFIG_NAME  | 'PROJECT'      | 'QA2'   | 'json'          | '/myJob/plan'        | expectedSummaries.default   | expectedLogs.default
        TC.C388106 | CONFIG_NAME  | 'PROJECT'      | 'QA0'   | 'json'          | '/myJob/plan'        | expectedSummaries.default   | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanDetails: Negative #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config : configName,
                projectKey : projectKey,
                planKey: planKey,
                resultFormat : resultFormat,
                resultPropertySheet : resultPropertySheet,
        ]

        when: "Run procedure"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)
        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        def propertyName = resultPropertySheet.split("/")[2]
        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        if (expectedSummary) {
            testCaseHelper.addExpectedResult("Job Summary: $expectedSummary")
            assert jobSummary == expectedSummary
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey)
        }

        testCaseHelper.addExpectedResult("Job status: $expectedLog")
        assert result.logs.contains(expectedLog.replace("PROJECTKEY", projectKey).replace("PLANKEY", planKey))
        where:
        caseId     | configName   | projectKey     | planKey | resultFormat    | resultPropertySheet  | expectedOutcome | expectedSummary             | expectedLog
        TC.C388107 | ''           | 'PROJECT'      | 'PLAN'  | 'json'          | '/myJob/plan'        | 'error'         | null                        | expectedLogs.defaultError
        TC.C388108 | CONFIG_NAME  | ''             | 'PLAN'  | 'json'          | '/myJob/plan'        | 'error'         | null                        | expectedLogs.defaultError
        TC.C388109 | CONFIG_NAME  | 'PROJECT'      | ''      | 'json'          | '/myJob/plan'        | 'error'         | null                        | expectedLogs.defaultError
        TC.C388110 | 'wrong'      | 'PROJECT'      | 'PLAN'  | 'json'          | '/myJob/plan'        | 'error'         | null                        | expectedLogs.defaultError
        TC.C388111 | CONFIG_NAME  | 'WRONG'        | 'PLAN'  | 'json'          | '/myJob/plan'        | 'error'         | expectedSummaries.notFound  | expectedLogs.notFound
        TC.C388112 | CONFIG_NAME  | 'PROJECT'      | 'WRONG' | 'json'          | '/myJob/plan'        | 'error'         | expectedSummaries.notFound  | expectedLogs.notFound
        TC.C388113 | CONFIG_NAME  | 'PROJECT'      | 'PLAN'  | 'wrong'         | '/myJob/plan'        | 'error'         | null                        | ''

    }

    def assertRecursively(def map, def map2){
        for (def entry in map) {
            if (entry.value instanceof Map){
                assertRecursively(entry.value, map2[entry.key])
            }
            else{
                testCaseHelper.addExpectedResult("Job property $entry.key: $entry.value")
                assert entry.value.toString() == map2[entry.key]
            }
        }
    }

}
