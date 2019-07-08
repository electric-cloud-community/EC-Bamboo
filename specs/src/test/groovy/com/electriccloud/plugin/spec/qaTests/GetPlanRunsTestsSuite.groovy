package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*
import groovy.json.JsonSlurper


class GetPlanRunsTestsSuite extends PluginTestHelper{

    static def procedureName = 'GetPlanRuns'
    static def projectName = 'TestProject: GetPlanRuns'
    static def getPlanRunsParams = [
            config : '',
            buildState : '',
            maxResults : '',
            planKey : '',
            projectKey : '',
            resultFormat : '',
            resultPropertySheet : '',
    ]

    static def runPlanParams = [
            config             : '',
            projectKey         : '',
            planKey            : '',
            waitForBuild       : '',
            waitTimeout        : '',
            additionalBuildVariables: '',
            resultFormat       : '',
            resultPropertySheet: '',
    ]

    static def TC = [
            C388114: [ids: 'C388114', description: 'GetPlanRuns: json format, plan has 1 run'],
            C388115: [ids: 'C388115', description: 'GetPlanRuns: json format, plan has 2 runs'],
            C388116: [ids: 'C388116', description: 'GetPlanRuns: propertySheet format, plan has 1 run'],
            C388117: [ids: 'C388117', description: 'GetPlanRuns: propertySheet format, plan has 2 runs'],
            C388118: [ids: 'C388118', description: 'GetPlanRuns: propertySheet format, plan has 10 runs'],
            C388119: [ids: 'C388119', description: 'GetPlanRuns: json format, plan has 10 runs'],
            C388121: [ids: 'C388121', description: 'GetPlanRuns: maxResult - 4'],
            C388122: [ids: 'C388122', description: 'GetPlanRuns: buildState - Successful'],
            C388123: [ids: 'C388123', description: 'GetPlanRuns: buildState - Failed'],
            C388124: [ids: 'C388124', description: 'GetPlanRuns: resultFormat - none'],
            C388125: [ids: 'C388125', description: 'empty required field: config'],
            C388126: [ids: 'C388126', description: 'empty required field: projectKey'],
            C388127: [ids: 'C388127', description: 'empty required field: planKey'],
            C388128: [ids: 'C388128', description: 'wrong config'],
            C388129: [ids: 'C388129', description: 'wrong ProjectKey'],
            C388130: [ids: 'C388130', description: 'wrong PlanKey'],
            C388131: [ids: 'C388131', description: 'wrong maxResutls'],
            C388132: [ids: 'C388132', description: 'wrong buildState'],
            C388133: [ids: 'C388133', description: 'wrong resultFormat'],
            C388134: [ids: 'C388134', description: 'plan contains zero runs'],
    ]

    static def testCaseHelper
    static def bambooClient

    static def expectedSummaries = [
            default:     "Plan run(s) information was saved to properties.",
            wrongPoject: "Can't find project by key: wrong",
            notFound: "Plan 'PROJECTKEY-PLANKEY' was not found",
            wrongState: "There is no BuildState called 'wrong'",
            zeroRun: "No results found for plan",
    ]

    static def expectedLogs = [
            default:     ["http://bamboo-server:8085/rest/api/latest/result/PROJECTKEY-PLANKEY?expand=results.result.artifacts%2Cresults.result.labels&max-results=MAXRESULT", "Found build result: 'PROJECTKEY-PLANKEY"],
            state:       ["http://bamboo-server:8085/rest/api/latest/result/PROJECTKEY-PLANKEY?expand=results.result.artifacts%2Cresults.result.labels&buildstate=STATE&max-results=MAXRESULT", "Found build result: 'PROJECTKEY-PLANKEY"],
            defaultError: "Possible exception in logs; check job",
            wrongState: "There is no BuildState called 'wrong'",
            notFound: "Plan \\'PROJECTKEY-PLANKEY\\' was not found",
            zeroRun: "No results found for plan",

    ]

    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        createConfiguration(PluginTestHelper.CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: procedureName, params: getPlanRunsParams]
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: 'RunPlan', params: runPlanParams]

        bambooClient = initBambooClient()
        bambooClient.createPlan('PROJECT', 'QARUN0', 'Plan without runs', 1)
        bambooClient.createPlanForRun('PROJECT', 'QARUN1', 'QA project for runs1', ['jar', 'xml'])
        bambooClient.createPlanForRun('PROJECT', 'QARUN2', 'QA project for runs2')
        bambooClient.createPlanForRun('PROJECT', 'QARUN3', 'QA project for runs10')
        runPlan('PROJECT', 'QARUN1')
        runPlan('PROJECT', 'QARUN2', 2)
        runPlan('PROJECT', 'QARUN3', 3)
        runPlan('PROJECT', 'QARUN3', 6, 'FAIL_MESSAGE=1')
        runPlan('PROJECT', 'QARUN3', 1)

    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PluginTestHelper.PLUGIN_NAME, PluginTestHelper.CONFIG_NAME)
        conditionallyDeleteProject(projectName)
        bambooClient.deletePlan('PROJECT', 'QARUN0')
        bambooClient.deletePlan('PROJECT', 'QARUN1')
        bambooClient.deletePlan('PROJECT', 'QARUN2')
        bambooClient.deletePlan('PROJECT', 'QARUN3')
    }

    @Sanity
    @Unroll
    def 'GetPlanRuns: Sanity #caseId.ids #caseId.description'() {
        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config : configName,
                buildState : buildState,
                maxResults : maxResults,
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



        def planRunsInfo = bambooClient.getPlanRuns(projectKey, planKey, maxResults, buildState).results.result
        for (def i=0; i<planRunsInfo.size(); i++) {
            def artifacts = planRunsInfo[i].artifacts
            if (resultFormat == 'propertySheet') {
                planRunsInfo[i].artifacts = [:]
                for (def j=0; j<artifacts.artifact.size(); j++) {
                    planRunsInfo[i].artifacts["$j"] = [:]
                    planRunsInfo[i].artifacts["$j"].shared = artifacts.artifact[j].shared
                    planRunsInfo[i].artifacts["$j"].size = artifacts.artifact[j].size
                    planRunsInfo[i].artifacts["$j"].producerJobKey = artifacts.artifact[j].producerJobKey
                    planRunsInfo[i].artifacts["$j"].prettySizeDescription = artifacts.artifact[j].prettySizeDescription
                    planRunsInfo[i].artifacts["$j"].name = artifacts.artifact[j].name
                    planRunsInfo[i].artifacts["$j"].link = artifacts.artifact[j].link.href
                }
                planRunsInfo[i].artifacts.count = artifacts.artifact.size().toString()
            }
            if (resultFormat == 'json') {
                planRunsInfo[i].artifacts = []
                for (def j=0; j<artifacts.artifact.size(); j++) {
                    planRunsInfo[i].artifacts[j] = [:]
                    planRunsInfo[i].artifacts[j].shared = artifacts.artifact[j].shared
                    planRunsInfo[i].artifacts[j].size = artifacts.artifact[j].size
                    planRunsInfo[i].artifacts[j].producerJobKey = artifacts.artifact[j].producerJobKey
                    planRunsInfo[i].artifacts[j].prettySizeDescription = artifacts.artifact[j].prettySizeDescription
                    planRunsInfo[i].artifacts[j].name = artifacts.artifact[j].name
                    planRunsInfo[i].artifacts[j].link = artifacts.artifact[j].link.href
                }
            }
            planRunsInfo[i].totalTestsCount = planRunsInfo[i].successfulTestCount + planRunsInfo[i].failedTestCount + planRunsInfo[i].quarantinedTestCount + planRunsInfo[i].skippedTestCount
            planRunsInfo[i].url = planRunsInfo[i].link.href.replace(PluginTestHelper.commanderAddress, 'bamboo-server')
            planRunsInfo[i].planKey = planRunsInfo[i].plan.key

            planRunsInfo[i].remove("expand")
            planRunsInfo[i].remove("plan")
            planRunsInfo[i].remove("link")
            planRunsInfo[i].remove("buildResultKey")
            planRunsInfo[i].remove("id")
            planRunsInfo[i].remove("prettyBuildStartedTime")
            planRunsInfo[i].remove("buildCompletedDate")
            planRunsInfo[i].remove("prettyBuildCompletedTime")
            planRunsInfo[i].remove("buildDurationDescription")
            planRunsInfo[i].remove("buildRelativeTime")
            planRunsInfo[i].remove("vcsRevisions")
            planRunsInfo[i].remove("continuable")
            planRunsInfo[i].remove("onceOff")
            planRunsInfo[i].remove("restartable")
            planRunsInfo[i].remove("notRunYet")
            planRunsInfo[i].remove("reasonSummary")
            planRunsInfo[i].remove("comments")
            planRunsInfo[i].remove("labels")
            planRunsInfo[i].remove("jiraIssues")
            planRunsInfo[i].remove("variables")
            planRunsInfo[i].remove("stages")
            planRunsInfo[i].remove("planResultKey")
            planRunsInfo[i].remove("state")
            planRunsInfo[i].remove("number")
            if (planRunsInfo[i].successful == false){
                planRunsInfo[i].remove("totalTestsCount")
            }
        }

        def propertyName = resultPropertySheet.split("/")[2]

        then: "Verify results"
        assert result.outcome == 'success'

        assert jobSummary == expectedSummary

        if (maxResults != '25') {
            assert planRunsInfo.collect { it.key }.size().toString() == maxResults
        }

        planRunsInfo.each {
            // because JSON contains boolean value
            // but propertySheet contains string
            if (buildState == 'Failed') {
                assert it.successful.toString() == 'false'
            }
            if (buildState == 'Successful'){
                assert it.successful.toString() == 'true'
            }
        }

        if (resultFormat == 'json') {
            assert new JsonSlurper().parseText(jobProperties[propertyName]) == planRunsInfo
        }

        if (resultFormat == 'propertySheet') {
            for (def i=0; i<planRunsInfo.size(); i++) {
                def currentKey = planRunsInfo[i].key
                assertRecursively(planRunsInfo[i], jobProperties[propertyName][currentKey])
            }
            assert jobProperties[propertyName]['keys'].split(',') == planRunsInfo.collect{ it.key}

        }

        if (resultFormat == 'none'){
            assert !jobProperties[propertyName]
        }

        assert outputParameters.resultKeys.split(', ') == planRunsInfo.collect{ it.key }
        assert outputParameters.latestResultKey == planRunsInfo[0].key


        for (log in expectedLog) {
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey)
                    .replace('MAXRESULT', maxResults)
                    .replace("STATE", buildState))
        }

        where:
        caseId     | configName                   | projectKey | planKey  | buildState | maxResults | resultFormat | resultPropertySheet | expectedSummary           | expectedLog
        TC.C388123 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN3' | 'Failed'   | '4'        | 'json'       | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.state
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanRuns: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition('Plan QARUN1 should exist in project PROJECT, contains 1 run')
        testCaseHelper.testCasePrecondition('Plan QARUN2 should exist in project PROJECT, contains 2 runs')
        testCaseHelper.testCasePrecondition('Plan QARUN3 should exist in project PROJECT, contains 10 runs (3 - successful, 6 - fail, 1 successful')

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config : configName,
                buildState : buildState,
                maxResults : maxResults,
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



        def planRunsInfo = bambooClient.getPlanRuns(projectKey, planKey, maxResults, buildState).results.result
        for (def i=0; i<planRunsInfo.size(); i++) {
            def artifacts = planRunsInfo[i].artifacts
            if (resultFormat == 'propertySheet') {
                planRunsInfo[i].artifacts = [:]
                for (def j=0; j<artifacts.artifact.size(); j++) {
                    planRunsInfo[i].artifacts["$j"] = [:]
                    planRunsInfo[i].artifacts["$j"].shared = artifacts.artifact[j].shared
                    planRunsInfo[i].artifacts["$j"].size = artifacts.artifact[j].size
                    planRunsInfo[i].artifacts["$j"].producerJobKey = artifacts.artifact[j].producerJobKey
                    planRunsInfo[i].artifacts["$j"].prettySizeDescription = artifacts.artifact[j].prettySizeDescription
                    planRunsInfo[i].artifacts["$j"].name = artifacts.artifact[j].name
                    planRunsInfo[i].artifacts["$j"].link = artifacts.artifact[j].link.href
                }
                planRunsInfo[i].artifacts.count = artifacts.artifact.size().toString()
            }
            if (resultFormat == 'json') {
                planRunsInfo[i].artifacts = []
                for (def j=0; j<artifacts.artifact.size(); j++) {
                    planRunsInfo[i].artifacts[j] = [:]
                    planRunsInfo[i].artifacts[j].shared = artifacts.artifact[j].shared
                    planRunsInfo[i].artifacts[j].size = artifacts.artifact[j].size
                    planRunsInfo[i].artifacts[j].producerJobKey = artifacts.artifact[j].producerJobKey
                    planRunsInfo[i].artifacts[j].prettySizeDescription = artifacts.artifact[j].prettySizeDescription
                    planRunsInfo[i].artifacts[j].name = artifacts.artifact[j].name
                    planRunsInfo[i].artifacts[j].link = artifacts.artifact[j].link.href
                }
            }
            planRunsInfo[i].totalTestsCount = planRunsInfo[i].successfulTestCount + planRunsInfo[i].failedTestCount + planRunsInfo[i].quarantinedTestCount + planRunsInfo[i].skippedTestCount
            planRunsInfo[i].url = planRunsInfo[i].link.href.replace(PluginTestHelper.commanderAddress, 'bamboo-server')
            planRunsInfo[i].planKey = planRunsInfo[i].plan.key

            planRunsInfo[i].remove("expand")
            planRunsInfo[i].remove("plan")
            planRunsInfo[i].remove("link")
            planRunsInfo[i].remove("buildResultKey")
            planRunsInfo[i].remove("id")
            planRunsInfo[i].remove("prettyBuildStartedTime")
            planRunsInfo[i].remove("buildCompletedDate")
            planRunsInfo[i].remove("prettyBuildCompletedTime")
            planRunsInfo[i].remove("buildDurationDescription")
            planRunsInfo[i].remove("buildRelativeTime")
            planRunsInfo[i].remove("vcsRevisions")
            planRunsInfo[i].remove("continuable")
            planRunsInfo[i].remove("onceOff")
            planRunsInfo[i].remove("restartable")
            planRunsInfo[i].remove("notRunYet")
            planRunsInfo[i].remove("reasonSummary")
            planRunsInfo[i].remove("comments")
            planRunsInfo[i].remove("labels")
            planRunsInfo[i].remove("jiraIssues")
            planRunsInfo[i].remove("variables")
            planRunsInfo[i].remove("stages")
            planRunsInfo[i].remove("planResultKey")
            planRunsInfo[i].remove("state")
            planRunsInfo[i].remove("number")
            if (planRunsInfo[i].successful == false){
                planRunsInfo[i].remove("totalTestsCount")
            }
        }

        def propertyName = resultPropertySheet.split("/")[2]

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: success")
        assert result.outcome == 'success'

        testCaseHelper.addExpectedResult("Job Summary: $expectedSummary")
        assert jobSummary == expectedSummary

        if (maxResults != '25') {
            testCaseHelper.addExpectedResult("Count of received of plans should be $maxResults")
            assert planRunsInfo.collect { it.key }.size().toString() == maxResults
        }

        planRunsInfo.each {
            // because JSON contains boolean value
            // but propertySheet contains string
            if (buildState == 'Failed') {
                testCaseHelper.addExpectedResult("Job should contains info only about plan runs with buildStatus false")
                assert it.successful.toString() == 'false'
            }
            if (buildState == 'Successful'){
                testCaseHelper.addExpectedResult("Job should contains info only about plan runs with buildStatus true")
                assert it.successful.toString() == 'true'
            }
        }

        if (resultFormat == 'json') {
            testCaseHelper.addExpectedResult("Job property $propertyName: $planRunsInfo")
            assert new JsonSlurper().parseText(jobProperties[propertyName]) == planRunsInfo
        }

        if (resultFormat == 'propertySheet') {
            for (def i=0; i<planRunsInfo.size(); i++) {
                def currentKey = planRunsInfo[i].key
                testCaseHelper.addExpectedResult("Job property ${planRunsInfo[i]}")
                assertRecursively(planRunsInfo[i], jobProperties[propertyName][currentKey])
            }
            testCaseHelper.addExpectedResult("Job property 'keys': ${planRunsInfo.collect{it.key}}")
            assert jobProperties[propertyName]['keys'].split(',') == planRunsInfo.collect{ it.key}

        }

        if (resultFormat == 'none'){
            assert !jobProperties[propertyName]
        }

        testCaseHelper.addExpectedResult("Output parameter resultKeys: ${planRunsInfo.collect{it.key}}")
        assert outputParameters.resultKeys.split(', ') == planRunsInfo.collect{ it.key }
        testCaseHelper.addExpectedResult("Output parameter latestResultKey: ${planRunsInfo[0].key}")
        assert outputParameters.latestResultKey == planRunsInfo[0].key


        for (log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs: $log")
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey)
                    .replace('MAXRESULT', maxResults)
                    .replace("STATE", buildState))
        }

        where:
        caseId     | configName                   | projectKey | planKey  | buildState   | maxResults | resultFormat    | resultPropertySheet | expectedSummary           | expectedLog
        TC.C388114 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN1' | 'All'        | '25'       | 'json'          | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.default
        TC.C388115 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN2' | 'All'        | '25'       | 'json'          | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.default
        TC.C388116 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN1' | 'All'        | '25'       | 'propertySheet' | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.default
        TC.C388117 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN2' | 'All'        | '25'       | 'propertySheet' | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.default
// C388118 runs to long
//        TC.C388118 | CONFIG_NAME  | 'PROJECT'      | 'QARUN3'  | 'All'       | '25'       | 'propertySheet' | '/myJob/PlanRun'     | expectedSummaries.default   | expectedLogs.default
        TC.C388119 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN3' | 'All'        | '25'       | 'json'          | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.default
        TC.C388121 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN3' | 'All'        | '4'        | 'propertySheet' | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.default
        TC.C388122 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN3' | 'Successful' | '25'       | 'propertySheet' | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.state
        TC.C388123 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN3' | 'Failed'     | '25'       | 'json'          | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.state
        TC.C388124 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN1' | 'All'        | '25'       | 'none'          | '/myJob/PlanRun'    | expectedSummaries.default | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanRuns: Negative #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config : configName,
                buildState : buildState,
                maxResults : maxResults,
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
        testCaseHelper.addExpectedResult("Job status: error")
        assert result.outcome == expectedOutcome


        where:
        caseId     | configName                   | projectKey | planKey  | buildState | maxResults | resultFormat | resultPropertySheet | expectedOutcome | expectedSummary              | expectedLog
        TC.C388125 | ''                           | 'PROJECT'  | 'QARUN1' | 'All'      | '25'       | 'json'       | '/myJob/PlanRun'    | 'error'         | null                         | expectedLogs.defaultError
        TC.C388126 | PluginTestHelper.CONFIG_NAME | ''         | 'QARUN1' | 'All'      | '25'       | 'json'       | '/myJob/PlanRun'    | 'error'         | null                         | expectedLogs.defaultError
        TC.C388127 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | ''       | 'All'      | '25'       | 'json'       | '/myJob/PlanRun'    | 'error'         | null                         | expectedLogs.defaultError
        TC.C388128 | 'wrong'                      | 'PROJECT'  | 'QARUN1' | 'All'      | '25'       | 'json'       | '/myJob/PlanRun'    | 'error'         | null                         | expectedLogs.defaultError
        TC.C388129 | PluginTestHelper.CONFIG_NAME | 'WRONG'    | 'QARUN1' | 'All'      | '25'       | 'json'       | '/myJob/PlanRun'    | 'error'         | expectedSummaries.notFound   | expectedLogs.notFound
        TC.C388130 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'WRONG'  | 'All'      | '25'       | 'json'       | '/myJob/PlanRun'    | 'error'         | expectedSummaries.notFound   | expectedLogs.notFound
        TC.C388131 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN1' | 'All'      | 'wrong'    | 'json'       | '/myJob/PlanRun'    | 'error'         | expectedSummaries.notFound   | expectedLogs.notFound
        TC.C388132 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN1' | 'wrong'    | '25'       | 'json'       | '/myJob/PlanRun'    | 'error'         | expectedSummaries.wrongState | expectedLogs.wrongState
        TC.C388133 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN1' | 'All'      | '25'       | 'wrong'      | '/myJob/PlanRun'    | 'error'         | null                         | expectedLogs.defaultError
        TC.C388134 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'QARUN0' | 'All'      | '25'       | 'json'       | '/myJob/PlanRun'    | 'warning'       | expectedSummaries.zeroRun    | expectedSummaries.zeroRun

    }

    def runPlan(def projectKey, def planKey, def countOfRun=1, def additionalBuildVariables = ''){
        def runParams = [
                config             : PluginTestHelper.CONFIG_NAME,
                projectKey         : projectKey,
                planKey            : planKey,
                waitForBuild       : 1,
                waitTimeout        : 300,
                additionalBuildVariables : additionalBuildVariables,
                resultFormat       : 'propertySheet',
                resultPropertySheet: '/myJob/result'
        ]
        for (def i=0; i<countOfRun; i++) {
            runProcedure(projectName, 'RunPlan', runParams)
        }
    }

    def assertRecursively(def map, def map2){
        for (def entry in map) {
            if (entry.value instanceof Map){
                assertRecursively(entry.value, map2[entry.key])
            }
            else{
                testCaseHelper.addExpectedResult("----Job property $entry.key: $entry.value")
                assert entry.value.toString() == map2[entry.key]
            }
        }
    }

}
