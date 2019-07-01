package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import groovy.json.JsonSlurper
import spock.lang.Unroll

class TriggerDeploymentTestSuite  extends PluginTestHelper{

    static def procedureName = 'TriggerDeployment'

    static def projectName = 'TestProject: TriggerDeployment'

    static def runParams = [
            config: "",
            deploymentEnvironmentName: "",
            deploymentProjectName: "",
            deploymentVersionName: "",
            resultFormat: "",
            resultPropertySheet: "",
            waitForDeployment: "",
            waitTimeout: "",
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

    static def createReleaseParams = [
            config: "",
            deploymentProjectName: "",
            planBuildKey: "",
            requestVersionName: "",
            resultFormat: "",
            resultPropertySheet: "",
            versionName: "",
    ]

    static def TC = [
            C388153: [ids: 'C388153', description: 'Enable disabled plan'],
    ]

    static def testCaseHelper
    static def bambooClient
    static def bambooDeployFromProject = 'Deployment Project'
    static def bambooProject = 'PROJECT'
    static def bambooDeployFromPlan = 'PLAN'
    static def successfulPlanRunKey
    static def versionName

    static def expectedSummaries = [
            default:     "Finished with Success. Deployment result key: 'KEY'",
            error: "Build was not finished successfully",
            noWait: "Deployment was successfully added to a queue.",
            wrongPoject: "Can't find project by key: wrong",
            notFound: "Plan PROJECTKEY-PLANKEY not found.",
            wrongState: "There is no BuildState called 'wrong'",
            zeroRun: "No results found for plan",
            notStarted: "Build was not started.",
    ]

    static def expectedLogs = [
            default:     ['Request URI: http://bamboo-server:8085/rest/api/latest/deploy/result/KEY',
                          'http://bamboo-server:8085/rest/api/latest/queue/deployment?'],
            defaultError: ["Possible exception in logs; check job"],
            wrongState: ["There is no BuildState called 'wrong'"],
            notFound: ["Plan PROJECTKEY-PLANKEY not found"],
            zeroRun: ["No results found for plan"],
    ]

    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: procedureName, params: runParams]
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: 'RunPlan', params: runPlanParams]
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: 'CreateRelease', params: createReleaseParams]

        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
//        successfulPlanRunKey = runPlan(bambooProject, bambooDeployFromPlan)
//        versionName = createRelease(bambooDeployFromProject, successfulPlanRunKey)
        versionName = 'release_80c96cb5-d07a-4945-bbdf-080b5297f616'
    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
//        conditionallyDeleteProject(projectName)
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'TriggerDeployment: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)

        given: "Tests parameters for procedure"
        def runParams = [
                config: configName,
                deploymentEnvironmentName: envName,
                deploymentProjectName: deploymentProjectName,
                deploymentVersionName: deploymentVersionName,
                resultFormat: resultFormat,
                resultPropertySheet: resultPropertySheet,
                waitForDeployment: waitForDeployment,
                waitTimeout: waitTimeout,
        ]

        when: "Run procedure TriggerDeployment"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        def deploymentInfo = bambooClient.getDeployment(outputParameters['deploymentResultKey'])
        deploymentInfo.agentId = deploymentInfo.agent.id
        deploymentInfo.agentName = deploymentInfo.agent.name
        deploymentInfo.key = deploymentInfo.key.key
        ['deploymentVersion', 'startedDate', 'queuedDate', 'executedDate', 'finishedDate', 'reasonSummary', 'agent', 'operations'].each{
            deploymentInfo.remove(it)
        }

        def propertyName = resultPropertySheet.split("/")[2]

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace('KEY', deploymentInfo.key)}")
        assert jobSummary == expectedSummary.replace('KEY', deploymentInfo.key)

        for (def log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $log")
            assert result.logs.contains(log
                    .replace("KEY", outputParameters['deploymentResultKey']))
        }

        testCaseHelper.addExpectedResult("OutputParameter deploymentResultKey: ${outputParameters['deploymentResultKey']}")
        assert outputParameters['deploymentResultKey']

        testCaseHelper.addExpectedResult("OutputParameter deploymentResultUrl: http://bamboo-server:8085/deploy/viewDeploymentResult.action?deploymentResultId=${outputParameters['deploymentResultKey']}")
        assert outputParameters['deploymentResultUrl'] == "http://bamboo-server:8085/deploy/viewDeploymentResult.action?deploymentResultId=${outputParameters['deploymentResultKey']}"

        if (resultFormat == 'json') {
            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}: $deploymentInfo")
            assert deploymentInfo == new JsonSlurper().parseText(jobProperties[propertyName])
        }

        if (resultFormat == 'propertySheet') {
            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}: $deploymentInfo")
            assertRecursively(deploymentInfo, jobProperties[propertyName])
        }

        where:
        caseId     | configName   | envName   | deploymentProjectName | deploymentVersionName | resultFormat    | resultPropertySheet       | waitForDeployment | waitTimeout | expectedOutcome | expectedSummary             | expectedLog
        TC.C388153 | CONFIG_NAME  | 'Stage 1' | 'Deployment Project'  | versionName           | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388153 | CONFIG_NAME  | 'Stage 1' | 'Deployment Project'  | versionName           | 'propertySheet' | '/myJob/deploymentResult' | '1'               | '300'       | 'success'       | expectedSummaries.default   | expectedLogs.default

    }

    def runPlan(def projectKey, def planKey, def additionalBuildVariables = ''){
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
        def result = runProcedure(projectName, 'RunPlan', runParams)
        return getJobProperties(result.jobId)['result']['key']
    }

    def createRelease(def deploymentProjetct, def planBuildKey){
        def versionName = randomize('release')
        def runParams = [
                config: PluginTestHelper.CONFIG_NAME,
                deploymentProjectName: deploymentProjetct,
                planBuildKey: planBuildKey,
                requestVersionName: '0',
                resultFormat: "json",
                resultPropertySheet: "/myJob/version",
                versionName: versionName,
        ]
        runProcedure(projectName, 'CreateRelease', runParams)
        return versionName
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
