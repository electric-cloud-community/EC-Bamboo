package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import groovy.json.JsonSlurper
import spock.lang.IgnoreRest
import spock.lang.Unroll

class RunPlanTestSuite extends PluginTestHelper{

    static def procedureName = 'RunPlan'
    static def projectName = 'TestProject: GetPlanRuns'

    static def runPlanParams = [
            config             : '',
            customRevision     : '',
            projectKey         : '',
            planKey            : '',
            waitForBuild       : '',
            waitTimeout        : '',
            additionalBuildVariables: '',
            resultFormat       : '',
            resultPropertySheet: '',
    ]

    static def TC = [
            C388135: [ids: 'C388135', description: 'Run plan, default values, resultFormat - json'],
            C388136: [ids: 'C388136', description: 'Run plan, default values, resultFormat - propertySheet'],
            C388137: [ids: 'C388137', description: 'Run plan, customRevision: master branch'],
            C388138: [ids: 'C388138', description: 'Run plan, customRevision: commitHash from release branch'],
            C388139: [ids: 'C388139', description: 'Run plan with var in  Additional Variables'],
            C388140: [ids: 'C388140', description: 'Run plan with some vars in Additional Variables'],
            C388141: [ids: 'C388141', description: 'Run plan, plan ends with error'],
            C388142: [ids: 'C388142', description: 'Run plan, "wait build" false - json'],
            C388143: [ids: 'C388143', description: 'Run plan, "wait build" false - propertySheet'],
            C388144: [ids: 'C388144', description: 'Run plan - all values'],
            C388145: [ids: 'C388145', description: ''],
    ]

    static def testCaseHelper
    static def bambooClient

    static def expectedSummaries = [
            default:     "Completed with Success. Build Result Key: 'PLANRUNKEY'",
            error: "Build was not finished successfully",
            noWait: "Build was successfully added to a queue.",
            wrongPoject: "Can't find project by key: wrong",
            notFound: "Plan PROJECTKEY-PLANKEY not found.",
            wrongState: "There is no BuildState called 'wrong'",
            zeroRun: "No results found for plan",
            notStarted: "Build was not started.",
    ]

    static def expectedLogs = [
            default:     ["Request URI: http://bamboo-server:8085/rest/api/latest/result/PLANRUNKEY?expand=results.result.artifacts%2Cresults.result.labels",
                            "http://bamboo-server:8085/rest/api/latest/queue/PROJECTKEY-PLANKEY"],
            customRevision: ["Request URI: http://bamboo-server:8085/rest/api/latest/result/PLANRUNKEY?expand=results.result.artifacts%2Cresults.result.labels",
                          "http://bamboo-server:8085/rest/api/latest/queue/PROJECTKEY-PLANKEY?customRevision=CUSTOMREVISION&executeAllStages=true"],
            vars: ["Request URI: http://bamboo-server:8085/rest/api/latest/result/PLANRUNKEY?expand=results.result.artifacts%2Cresults.result.labels"],
            defaultError: "Possible exception in logs; check job",
            wrongState: "There is no BuildState called 'wrong'",
            notFound: "Plan \\'PROJECTKEY-PLANKEY\\' was not found",
            zeroRun: "No results found for plan",
    ]

    static def vars = [
            one: 'TEST_MESSAGE=QAtestMessage',
            two: 'TEST_MESSAGE=QAtestMessage\nSLEEP_TIME=11',
            error: 'FAIL_MESSAGE=1'
    ]

    // https://github.com/horodchukanton/gradle-test-build/commit/3b04da7ddc5a2e8e61020f1886f3e4e5a07b7688
    static def commitHash = '3b04da7ddc5a2e8e61020f1886f3e4e5a07b7688'

    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: 'RunPlan', params: runPlanParams]

        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
        bambooClient.createPlanForRun('PROJECT', 'QARUNPLAN', 'QA project for runs1', ['jar', 'xml'])

    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
        bambooClient.deletePlan('PROJECT', 'QARUNPLAN')
    }

    @Sanity
    @Unroll
    def 'GetPlanRuns: Sanity #caseId.ids #caseId.description'() {

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config                  : configName,
                customRevision          : customRevision,
                projectKey              : projectKey,
                planKey                 : planKey,
                waitForBuild            : waitForBuild,
                waitTimeout             : waitTimeout,
                additionalBuildVariables: additionalBuildVariables,
                resultFormat            : resultFormat,
                resultPropertySheet     : resultPropertySheet,
        ]

        when: "Run procedure"

        def lastPlanRunNameBeforeProcedureStarted
        try {
            lastPlanRunNameBeforeProcedureStarted = bambooClient.getPlanRuns(projectKey, planKey, 1, 'All').results.result[0].key
        }
        catch (NullPointerException exception){
            lastPlanRunNameBeforeProcedureStarted = null
        }

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        def propertyName = resultPropertySheet.split("/")[2]

        def lastPlanRunName = bambooClient.getPlanRuns(projectKey, planKey, 1, 'All').results.result[0].key
        def planRunInfo = bambooClient.getPlanRunInfo(lastPlanRunName)

        planRunInfo.url = planRunInfo.link.href.replace(commanderAddress, 'bamboo-server')
        planRunInfo.planKey = planRunInfo.planResultKey.entityKey.key
        planRunInfo.totalTestsCount = planRunInfo.successfulTestCount + planRunInfo.failedTestCount + planRunInfo.quarantinedTestCount + planRunInfo.skippedTestCount
        ["expand", "link", "plan", "buildResultKey", "id", "buildCompletedDate", "prettyBuildCompletedTime",
         "buildRelativeTime", "vcsRevisions", "continuable", "onceOff", "restartable", "notRunYet", "reasonSummary",
         "comments", "labels", "jiraIssues", "variables", "stages", "changes", "metadata", "planResultKey", "state", "number", "prettyBuildStartedTime", "buildDurationDescription"].each {
            planRunInfo.remove(it)
        }
        def bambooRunVars = bambooClient.getPlanRunVars(planRunInfo.key).variables.variable

        then: "Verify results"
        assert result.outcome == expectedOutcome

        assert jobSummary == expectedSummary.replace('PLANRUNKEY', lastPlanRunName)

        assert lastPlanRunName != lastPlanRunNameBeforeProcedureStarted

        assert outputParameters.buildResultKey == lastPlanRunName

        assert outputParameters.buildUrl == "http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}"

        assert jobProperties['report-urls']['View Build Report'] == "http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}"

        for (def log in expectedLog) {
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey)
                    .replace("PLANRUNKEY", lastPlanRunName)
                    .replace("CUSTOMREVISION", customRevision))
        }

        if (additionalBuildVariables) {
            for (def var in additionalBuildVariables.split("\n")){
                assert bambooRunVars.any {
                    it.name == var.split('=')[0] && it.value == var.split('=')[1]
                }
            }

        }

        if (resultFormat == 'json'){
            if (customRevision) {
                assert new JsonSlurper().parseText(jobProperties[propertyName])['buildReason'] =~ /Custom build by.*$customRevision/
            }
            else {
                assert new JsonSlurper().parseText(jobProperties[propertyName])['buildReason'] =~ "Manual run by"
            }
//            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}: $planRunInfo")
//            assert new JsonSlurper().parseText(jobProperties[propertyName]) == planRunInfo
        }

        if (resultFormat == 'propertySheet'){
            if (customRevision) {
                assert jobProperties[propertyName]['buildReason'] =~ /Custom build by.*$customRevision/
            }
            else {
                assert jobProperties[propertyName]['buildReason'] =~ "Manual run by"
            }
//            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}:")
//            assertRecursively(planRunInfo, jobProperties[propertyName] )
        }


        where:
        caseId     | configName   | projectKey     | planKey      | additionalBuildVariables | waitForBuild | waitTimeout | customRevision | resultFormat    | resultPropertySheet  | expectedOutcome | expectedSummary             | expectedLog
        TC.C388135 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388144 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | vars.error               | '1'          | '300'       | commitHash     | 'json'          | '/myJob/runResult'   | 'warning'       | expectedSummaries.error     | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanRuns: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition('Plan QARUN1 should exist in project PROJECT, contains 1 run')

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config                  : configName,
                customRevision          : customRevision,
                projectKey              : projectKey,
                planKey                 : planKey,
                waitForBuild            : waitForBuild,
                waitTimeout             : waitTimeout,
                additionalBuildVariables: additionalBuildVariables,
                resultFormat            : resultFormat,
                resultPropertySheet     : resultPropertySheet,
        ]

        when: "Run procedure"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)
        def lastPlanRunNameBeforeProcedureStarted
        try {
            lastPlanRunNameBeforeProcedureStarted = bambooClient.getPlanRuns(projectKey, planKey, 1, 'All').results.result[0].key
        }
        catch (NullPointerException exception){
            lastPlanRunNameBeforeProcedureStarted = null
        }

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        def propertyName = resultPropertySheet.split("/")[2]

        def lastPlanRunName = bambooClient.getPlanRuns(projectKey, planKey, 1, 'All').results.result[0].key
        def planRunInfo = bambooClient.getPlanRunInfo(lastPlanRunName)

        planRunInfo.url = planRunInfo.link.href.replace(commanderAddress, 'bamboo-server')
        planRunInfo.planKey = planRunInfo.planResultKey.entityKey.key
        planRunInfo.totalTestsCount = planRunInfo.successfulTestCount + planRunInfo.failedTestCount + planRunInfo.quarantinedTestCount + planRunInfo.skippedTestCount
        ["expand", "link", "plan", "buildResultKey", "id", "buildCompletedDate", "prettyBuildCompletedTime",
         "buildRelativeTime", "vcsRevisions", "continuable", "onceOff", "restartable", "notRunYet", "reasonSummary",
         "comments", "labels", "jiraIssues", "variables", "stages", "changes", "metadata", "planResultKey", "state", "number", "prettyBuildStartedTime", "buildDurationDescription"].each {
            planRunInfo.remove(it)
        }
        def bambooRunVars = bambooClient.getPlanRunVars(planRunInfo.key).variables.variable

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace('PLANRUNKEY', lastPlanRunName)}")
        assert jobSummary == expectedSummary.replace('PLANRUNKEY', lastPlanRunName)

        testCaseHelper.addExpectedResult("Procedure should trigger plan run, new plan run should appear")
        assert lastPlanRunName != lastPlanRunNameBeforeProcedureStarted

        testCaseHelper.addExpectedResult("Job output parameter 'buildResultKey': $lastPlanRunName")
        assert outputParameters.buildResultKey == lastPlanRunName

        testCaseHelper.addExpectedResult("Job output parameter 'buildUrl': http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}")
        assert outputParameters.buildUrl == "http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}"

        testCaseHelper.addExpectedResult("Job property   / report-urls / View Build Report: http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}")
        assert jobProperties['report-urls']['View Build Report'] == "http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}"

        for (def log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $log")
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey)
                    .replace("PLANRUNKEY", lastPlanRunName)
                    .replace("CUSTOMREVISION", customRevision))
        }

        if (additionalBuildVariables) {
            for (def var in additionalBuildVariables.split("\n")){
                testCaseHelper.addExpectedResult("Bamboo run should have variable ${var.split('=')[0]}: ${var.split('=')[1]}")
                assert bambooRunVars.any {
                    it.name == var.split('=')[0] && it.value == var.split('=')[1]
                }
            }

        }

        if (resultFormat == 'json'){
            if (customRevision) {
                testCaseHelper.addExpectedResult("Job property: $propertyName /'buildReason' should contain text: Custom build by .* $customRevision")
                assert new JsonSlurper().parseText(jobProperties[propertyName])['buildReason'] =~ /Custom build by.*$customRevision/
            }
            else {
                testCaseHelper.addExpectedResult("Job property: $propertyName /'buildReason' should contain text: Manual run by ...")
                assert new JsonSlurper().parseText(jobProperties[propertyName])['buildReason'] =~ "Manual run by"
            }
//            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}: $planRunInfo")
//            assert new JsonSlurper().parseText(jobProperties[propertyName]) == planRunInfo
        }

        if (resultFormat == 'propertySheet'){
            if (customRevision) {
                testCaseHelper.addExpectedResult("Job property: $propertyName /'buildReason' should contain text: Custom build by .* $customRevision")
                assert jobProperties[propertyName]['buildReason'] =~ /Custom build by.*$customRevision/
            }
            else {
                testCaseHelper.addExpectedResult("Job property: $propertyName /'buildReason' should contain text: Manual run by ...")
                assert jobProperties[propertyName]['buildReason'] =~ "Manual run by"
            }
//            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}:")
//            assertRecursively(planRunInfo, jobProperties[propertyName] )
        }


        where:
        caseId     | configName   | projectKey     | planKey      | additionalBuildVariables | waitForBuild | waitTimeout | customRevision | resultFormat    | resultPropertySheet  | expectedOutcome | expectedSummary             | expectedLog
        TC.C388135 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388136 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '1'          | '300'       | ''             | 'propertySheet' | '/myJob/runResult'   | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388137 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '1'          | '300'       | 'master'       | 'json'          | '/myJob/runResult'   | 'success'       | expectedSummaries.default   | expectedLogs.customRevision
        TC.C388138 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '1'          | '300'       | commitHash     | 'propertySheet' | '/myJob/runResult'   | 'success'       | expectedSummaries.default   | expectedLogs.customRevision
        TC.C388139 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | vars.one                 | '1'          | '300'       | ''             | 'propertySheet' | '/myJob/runResult'   | 'success'       | expectedSummaries.default   | expectedLogs.vars
        TC.C388140 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | vars.two                 | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'success'       | expectedSummaries.default   | expectedLogs.vars
        TC.C388141 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | vars.error               | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'warning'       | expectedSummaries.error     | expectedLogs.default
        TC.C388144 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | vars.error               | '1'          | '300'       | commitHash     | 'json'          | '/myJob/runResult'   | 'warning'       | expectedSummaries.error     | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanRuns: Positive Don`t wait #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition('Plan QARUN1 should exist in project PROJECT, contains 1 run')

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config                  : configName,
                customRevision          : customRevision,
                projectKey              : projectKey,
                planKey                 : planKey,
                waitForBuild            : waitForBuild,
                waitTimeout             : waitTimeout,
                additionalBuildVariables: additionalBuildVariables,
                resultFormat            : resultFormat,
                resultPropertySheet     : resultPropertySheet,
        ]

        when: "Run procedure"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)
        def lastPlanRunNameBeforeProcedureStarted
        try {
            lastPlanRunNameBeforeProcedureStarted = bambooClient.getPlanRuns(projectKey, planKey, 1, 'All').results.result[0].key
        }
        catch (NullPointerException exception){
            lastPlanRunNameBeforeProcedureStarted = null
        }

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        def propertyName = resultPropertySheet.split("/")[2]

        // need some time until run plan will appear in Bamboo
        sleep(4000)
        def lastPlanRunName = bambooClient.getPlanRuns(projectKey, planKey, 1, 'All').results.result[0].key
        def planRunInfo = bambooClient.getPlanRunInfo(lastPlanRunName)

        planRunInfo.url = planRunInfo.link.href.replace(commanderAddress, 'bamboo-server')
        planRunInfo.planKey = planRunInfo.planResultKey.entityKey.key
        planRunInfo.totalTestsCount = planRunInfo.successfulTestCount + planRunInfo.failedTestCount + planRunInfo.quarantinedTestCount + planRunInfo.skippedTestCount
        ["expand", "link", "plan", "buildResultKey", "id", "buildCompletedDate", "prettyBuildCompletedTime",
         "buildRelativeTime", "vcsRevisions", "continuable", "onceOff", "restartable", "notRunYet", "reasonSummary",
         "comments", "labels", "jiraIssues", "variables", "stages", "changes", "metadata", "planResultKey", "state", "number", "prettyBuildStartedTime", "buildDurationDescription"].each {
            planRunInfo.remove(it)
        }

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace('PLANRUNKEY', lastPlanRunName)}")
        assert jobSummary == expectedSummary.replace('PLANRUNKEY', lastPlanRunName)

        testCaseHelper.addExpectedResult("Procedure should trigger plan run, new plan run should appear")
        assert lastPlanRunName != lastPlanRunNameBeforeProcedureStarted

        testCaseHelper.addExpectedResult("Job output parameter 'buildResultKey': $lastPlanRunName")
        assert outputParameters.buildResultKey == lastPlanRunName

        testCaseHelper.addExpectedResult("Job output parameter 'buildUrl': http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}")
        assert outputParameters.buildUrl == "http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}"

        testCaseHelper.addExpectedResult("Job property   / report-urls / View Build Report: http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}")
        assert jobProperties['report-urls']['View Build Report'] == "http://bamboo-server:8085/chain/result/viewChainResult.action?planKey=$projectKey-$planKey&buildNumber=${planRunInfo.buildNumber}"

        for (def log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $log")
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey)
                    .replace("PLANRUNKEY", lastPlanRunName)
                    .replace("CUSTOMREVISION", customRevision))
        }

        if (additionalBuildVariables) {
            for (def var in additionalBuildVariables.split("\n")){
                testCaseHelper.addExpectedResult("Bamboo run should have variable ${var.split('=')[0]}: ${var.split('=')[1]}")
                assert bambooRunVars.any {
                    it.name == var.split('=')[0] && it.value == var.split('=')[1]
                }
            }

        }

        if (resultFormat == 'json'){
            if (customRevision) {
                testCaseHelper.addExpectedResult("Job property: $propertyName /'buildReason' should contain text: Custom build by .* $customRevision")
                assert new JsonSlurper().parseText(jobProperties[propertyName])['buildReason'] =~ /Custom build by.*$customRevision/
            }
            else {
                testCaseHelper.addExpectedResult("Job property: $propertyName /'buildReason' should contain text: Manual run by ...")
                assert new JsonSlurper().parseText(jobProperties[propertyName])['buildReason'] =~ "Manual run by"
            }
//            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}: $planRunInfo")
//            assert new JsonSlurper().parseText(jobProperties[propertyName]) == planRunInfo
        }

        if (resultFormat == 'propertySheet'){
            if (customRevision) {
                testCaseHelper.addExpectedResult("Job property: $propertyName /'buildReason' should contain text: Custom build by .* $customRevision")
                assert jobProperties[propertyName]['buildReason'] =~ /Custom build by.*$customRevision/
            }
            else {
                testCaseHelper.addExpectedResult("Job property: $propertyName /'buildReason' should contain text: Manual run by ...")
                assert jobProperties[propertyName]['buildReason'] =~ "Manual run by"
            }
//            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}:")
//            assertRecursively(planRunInfo, jobProperties[propertyName] )
        }
        cleanup:
        bambooClient.waitUntiPlanFinished(planRunInfo.key)
        where:
        caseId     | configName   | projectKey     | planKey      | additionalBuildVariables | waitForBuild | waitTimeout | customRevision | resultFormat    | resultPropertySheet  | expectedOutcome | expectedSummary             | expectedLog
        TC.C388142 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '0'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'success'       | expectedSummaries.noWait    | expectedLogs.default
        TC.C388143 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '0'          | '300'       | ''             | 'propertySheet' | '/myJob/runResult'   | 'success'       | expectedSummaries.noWait    | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetPlanRuns: Negative #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition('Plan QARUN1 should exist in project PROJECT, contains 1 run')

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config                  : configName,
                customRevision          : customRevision,
                projectKey              : projectKey,
                planKey                 : planKey,
                waitForBuild            : waitForBuild,
                waitTimeout             : waitTimeout,
                additionalBuildVariables: additionalBuildVariables,
                resultFormat            : resultFormat,
                resultPropertySheet     : resultPropertySheet,
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
            testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace("PROJECTKEY", projectKey).replace("PLANKEY", planKey)}")
            assert jobSummary == expectedSummary
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey)
        }

        for (def log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $log")
            assert result.logs.contains(log
                    .replace("PROJECTKEY", projectKey)
                    .replace("PLANKEY", planKey)
                    .replace("CUSTOMREVISION", customRevision))
        }

        where:
        caseId     | configName   | projectKey     | planKey      | additionalBuildVariables | waitForBuild | waitTimeout | customRevision | resultFormat    | resultPropertySheet  | expectedOutcome | expectedSummary    | expectedLog
        TC.C388145 | ''           | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'error'         | null               | expectedLogs.defaultError
        TC.C388145 | CONFIG_NAME  | ''             | 'QARUNPLAN'  | ''                       | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'error'         | null               | expectedLogs.defaultError
        TC.C388145 | CONFIG_NAME  | 'PROJECT'      | ''           | ''                       | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'error'         | null               | expectedLogs.defaultError
        TC.C388145 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | ''                       | ''           | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'error'         | null               | expectedLogs.defaultError
        TC.C388145 | 'wrong'      | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'error'         | null               | expectedLogs.defaultError
        TC.C388145 | CONFIG_NAME  | 'wrong'        | 'QARUNPLAN'  | ''                       | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'error'         | expectedSummaries.notFound   | expectedLogs.notFound
        TC.C388145 | CONFIG_NAME  | 'PROJECT'      | 'wrong'      | ''                       | '1'          | '300'       | ''             | 'json'          | '/myJob/runResult'   | 'error'         | expectedSummaries.notFound   | expectedLogs.notFound
        TC.C388145 | CONFIG_NAME  | 'PROJECT'      | 'QARUNPLAN'  | ''                       | '1'          | '300'       | 'wrong'             | 'json'          | '/myJob/runResult'   | 'warning'  | expectedSummaries.notStarted   | expectedSummaries.notStarted
    }

    def assertRecursively(def map, def map2){
        for (def entry in map) {
            if (entry.value instanceof Map){
                assertRecursively(entry.value, map2[entry.key])
            }
            else{
                testCaseHelper.addExpectedResult("--Job property $entry.key: $entry.value")
                assert entry.value.toString() == map2[entry.key]
            }
        }
    }

}
