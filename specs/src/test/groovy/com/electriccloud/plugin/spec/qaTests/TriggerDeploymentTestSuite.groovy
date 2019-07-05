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
            deploymentReleaseName: "",
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
            requestReleaseName: "",
            resultFormat: "",
            resultPropertySheet: "",
            releaseName: "",
    ]

    static def TC = [
            C388170: [ids: 'C388170', description: 'Trigger deployment - json format'],
            C388171: [ids: 'C388171', description: 'Trigger deployment - property sheet format'],
            C388172: [ids: 'C388172', description: 'Trigger deployment: long time of deployment'],
            C388173: [ids: 'C388173', description: 'Trigger deployment: Wait For Deployment - false'],
            C388174: [ids: 'C388174', description: 'Trigger deployment: Wait Timeout, Deployment is not finished in time'],
            C388175: [ids: 'C388175', description: 'Trigger deployment, don`t save result'],
            C388176: [ids: 'C388176', description: 'Trigger deployment, bamboo deployment is completed with errors'],
            C388177: [ids: 'C388177', description: 'empty required parameter: config'],
            C388178: [ids: 'C388178', description: 'empty required parameter: project Name'],
            C388179: [ids: 'C388179', description: 'empty required parameter: environment Name'],
            C388180: [ids: 'C388180', description: 'empty required parameter: version Name'],
            C388181: [ids: 'C388181', description: 'wrong value for parameter: config'],
            C388182: [ids: 'C388182', description: 'wrong value for parameter: project Name'],
            C388183: [ids: 'C388183', description: 'wrong value for parameter: environment Name'],
            C388184: [ids: 'C388184', description: 'wrong value for parameter: version Name'],
            C388186: [ids: 'C388186', description: 'wrong value for parameter: result format'],
    ]

    static def testCaseHelper
    static def bambooClient
    static def deployProjects = [
            default: 'Deployment Project',
            long: 'Long Deployment project',
            error: 'Failing deployment project',
            wrong: 'wrong',
    ]

    static def deployEnv = [
            default: 'Stage',
            long: 'Production',
            error: 'Production2',
            wrong: 'wrong',
    ]

    static def versionNames = [
            wrong: 'wrong'
    ]

    static def bambooProject = 'PROJECT'
    static def bambooDeployFromPlan = 'PLAN'
    static def successfulPlanRunKey


    static def expectedSummaries = [
            default:     "Finished with Success. Deployment result key: 'KEY'",
            error: "Deployment was not finished successfully.",
            noWait: "Deployment was successfully added to a queue.",
            timeout: "Exceeded the wait timeout while waiting for the deployment to finish.",
            wrongProject: "Can't find deployment project 'wrong'.",
            wrongEnv: "Can't find environment 'wrong'.",
            wrongVersion: "Can't find release 'wrong'.",
            wrongFormat: "Deployment is finished. Requesting result.",
    ]

    static def expectedLogs = [
            default:     ['Request URI: http://bamboo-server:8085/rest/api/latest/deploy/result/KEY',
                          'http://bamboo-server:8085/rest/api/latest/queue/deployment?'],
            defaultError: "Possible exception in logs; check job",
            wrongFormat: "Wrong Result Property Format provided. Has to be one of 'none', 'propertySheet', 'json'",
    ]

    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: procedureName, params: runParams]
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: 'RunPlan', params: runPlanParams]
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: 'CreateRelease', params: createReleaseParams]

        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
        successfulPlanRunKey = runPlan(bambooProject, bambooDeployFromPlan)
        versionNames.default = createRelease(deployProjects.default, successfulPlanRunKey)
        versionNames.long = createRelease(deployProjects.long, successfulPlanRunKey)
        versionNames.error = createRelease(deployProjects.error, successfulPlanRunKey)
    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def 'TriggerDeployment: Sanity #caseId.ids #caseId.description'() {

        given: "Tests parameters for procedure"
        def runParams = [
                config: configName,
                deploymentEnvironmentName: envName,
                deploymentProjectName: deploymentProjectName,
                deploymentReleaseName: deploymentVersionName,
                resultFormat: resultFormat,
                resultPropertySheet: resultPropertySheet,
                waitForDeployment: waitForDeployment,
                waitTimeout: waitTimeout,
        ]

        when: "Run procedure TriggerDeployment"


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
        assert result.outcome == expectedOutcome

        assert jobSummary == expectedSummary.replace('KEY', deploymentInfo.key)

        for (def log in expectedLog) {
            assert result.logs.contains(log
                    .replace("KEY", outputParameters['deploymentResultKey']))
        }

        assert outputParameters['deploymentResultKey']

        assert outputParameters['deploymentResultUrl'] == "http://bamboo-server:8085/deploy/viewDeploymentResult.action?deploymentResultId=${outputParameters['deploymentResultKey']}"

        assert jobProperties['report-urls']['View Deployment Report'] == "http://bamboo-server:8085/deploy/viewDeploymentResult.action?deploymentResultId=${outputParameters['deploymentResultKey']}"

        if (resultFormat == 'json') {
            assert deploymentInfo == new JsonSlurper().parseText(jobProperties[propertyName])
        }

        if (resultFormat == 'propertySheet') {
            assertRecursively(deploymentInfo, jobProperties[propertyName])
        }

        where:
        caseId     | configName   | envName           | deploymentProjectName      | deploymentVersionName | resultFormat    | resultPropertySheet       | waitForDeployment | waitTimeout | expectedOutcome | expectedSummary             | expectedLog
        TC.C388170 | CONFIG_NAME  | deployEnv.default | deployProjects.default     | versionNames.default  | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388176 | CONFIG_NAME  | deployEnv.error   | deployProjects.error       | versionNames.error    | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'warning'       | expectedSummaries.error     | expectedLogs.default
    }


    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'TriggerDeployment: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition("create release ${versionNames.default} in deploy project: ${deployProjects.default}")
        testCaseHelper.testCasePrecondition("create release ${versionNames.long} in deploy project: ${deployProjects.long}")
        testCaseHelper.testCasePrecondition("create release ${versionNames.error} in deploy project: ${deployProjects.error}")

        given: "Tests parameters for procedure"
        def runParams = [
                config: configName,
                deploymentEnvironmentName: envName,
                deploymentProjectName: deploymentProjectName,
                deploymentReleaseName: deploymentVersionName,
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

        testCaseHelper.addExpectedResult("Job property /report-urls/View Deployment Report : http://bamboo-server:8085/deploy/viewDeploymentResult.action?deploymentResultId=${outputParameters['deploymentResultKey']}")
        assert jobProperties['report-urls']['View Deployment Report'] == "http://bamboo-server:8085/deploy/viewDeploymentResult.action?deploymentResultId=${outputParameters['deploymentResultKey']}"

        if (resultFormat == 'json') {
            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}: $deploymentInfo")
            assert deploymentInfo == new JsonSlurper().parseText(jobProperties[propertyName])
        }

        if (resultFormat == 'propertySheet') {
            testCaseHelper.addExpectedResult("Job property  ${jobProperties[propertyName]}: $deploymentInfo")
            assertRecursively(deploymentInfo, jobProperties[propertyName])
        }

        where:
        caseId     | configName   | envName           | deploymentProjectName      | deploymentVersionName | resultFormat    | resultPropertySheet       | waitForDeployment | waitTimeout | expectedOutcome | expectedSummary             | expectedLog
        TC.C388170 | CONFIG_NAME  | deployEnv.default | deployProjects.default     | versionNames.default  | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388171 | CONFIG_NAME  | deployEnv.default | deployProjects.default     | versionNames.default  | 'propertySheet' | '/myJob/deploymentResult' | '1'               | '300'       | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388172 | CONFIG_NAME  | deployEnv.long    | deployProjects.long        | versionNames.long     | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388173 | CONFIG_NAME  | deployEnv.default | deployProjects.default     | versionNames.default  | 'json'          | '/myJob/deploymentResult' | '0'               | '300'       | 'success'       | expectedSummaries.noWait    | expectedLogs.default
        TC.C388175 | CONFIG_NAME  | deployEnv.default | deployProjects.default     | versionNames.default  | 'none'          | '/myJob/deploymentResult' | '1'               | '300'       | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388176 | CONFIG_NAME  | deployEnv.error   | deployProjects.error       | versionNames.error    | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'warning'       | expectedSummaries.error     | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'TriggerDeployment: Negative #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)

        given: "Tests parameters for procedure"
        def runParams = [
                config: configName,
                deploymentEnvironmentName: envName,
                deploymentProjectName: deploymentProjectName,
                deploymentReleaseName: deploymentVersionName,
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

        def propertyName = resultPropertySheet.split("/")[2]

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        if (expectedSummary) {
            testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary}")
            assert jobSummary == expectedSummary
        }

        if (expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $expectedLog")
            assert result.logs.contains(expectedLog)
        }

        if (caseId == TC.C388174) {
            testCaseHelper.addExpectedResult("OutputParameter deploymentResultKey: ${outputParameters['deploymentResultKey']}")
            assert outputParameters['deploymentResultKey']

            testCaseHelper.addExpectedResult("OutputParameter deploymentResultUrl: http://bamboo-server:8085/deploy/viewDeploymentResult.action?deploymentResultId=${outputParameters['deploymentResultKey']}")
            assert outputParameters['deploymentResultUrl'] == "http://bamboo-server:8085/deploy/viewDeploymentResult.action?deploymentResultId=${outputParameters['deploymentResultKey']}"
        }

        where:
        caseId     | configName   | envName           | deploymentProjectName      | deploymentVersionName | resultFormat    | resultPropertySheet       | waitForDeployment | waitTimeout | expectedOutcome | expectedSummary                 | expectedLog
        TC.C388174 | CONFIG_NAME  | deployEnv.long    | deployProjects.long        | versionNames.long     | 'json'          | '/myJob/deploymentResult' | '1'               | '15'        | 'error'         | expectedSummaries.timeout       | expectedSummaries.timeout
        TC.C388177 | ''           | deployEnv.default | deployProjects.default     | versionNames.default  | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'error'         | null                            | expectedLogs.defaultError
        TC.C388178 | CONFIG_NAME  | deployEnv.default | ''                         | versionNames.default  | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'error'         | null                            | expectedLogs.defaultError
        TC.C388179 | CONFIG_NAME  | ''                | deployProjects.default     | versionNames.default  | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'error'         | null                            | expectedLogs.defaultError
        TC.C388180 | CONFIG_NAME  | deployEnv.default | deployProjects.default     | ''                    | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'error'         | null                            | expectedLogs.defaultError
        TC.C388181 | 'wrong'      | deployEnv.default | deployProjects.default     | versionNames.default  | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'error'         | null                            | expectedLogs.defaultError
        TC.C388182 | CONFIG_NAME  | deployEnv.default | deployProjects.wrong       | versionNames.default  | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'error'         | expectedSummaries.wrongProject  | expectedSummaries.wrongProject
        TC.C388183 | CONFIG_NAME  | deployEnv.wrong   | deployProjects.default     | versionNames.default  | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'error'         | expectedSummaries.wrongEnv      | expectedSummaries.wrongEnv
        TC.C388184 | CONFIG_NAME  | deployEnv.default | deployProjects.default     | versionNames.wrong    | 'json'          | '/myJob/deploymentResult' | '1'               | '300'       | 'error'         | expectedSummaries.wrongVersion  | expectedSummaries.wrongVersion
        TC.C388186 | CONFIG_NAME  | deployEnv.default | deployProjects.default     | versionNames.default  | 'wrong'         | '/myJob/deploymentResult' | '1'               | '300'       | 'error'         | expectedSummaries.wrongFormat   | null

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
                requestReleaseName: '0',
                resultFormat: "json",
                resultPropertySheet: "/myJob/version",
                releaseName: versionName,
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
