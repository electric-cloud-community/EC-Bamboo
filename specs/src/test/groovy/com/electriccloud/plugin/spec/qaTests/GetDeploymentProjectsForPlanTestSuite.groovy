package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import groovy.json.JsonSlurper
import spock.lang.Unroll

class GetDeploymentProjectsForPlanTestSuite extends PluginTestHelper{

    static def procedureName = 'GetDeploymentProjectsForPlan'

    static def projectName = 'TestProject: GetDeploymentProjectsForPlan'

    static def runParams = [
            config: "",
            planKey: "",
            projectKey: "",
            resultFormat: "",
            resultPropertySheet: "",
    ]

    static def TC = [
            C388170: [ids: 'C388170', description: 'Trigger deployment - json format'],
    ]

    static def testCaseHelper
    static def bambooClient

    static def expectedSummaries = [
            default:     "Deployment projects info saved to property(ies).",
            error: "Deployment was not finished successfully.",
            noWait: "Deployment was successfully added to a queue.",
            timeout: "Exceeded the wait timeout while waiting for the deployment to finish.",
            wrongProject: "Can't find deployment project 'wrong'.",
            wrongEnv: "Can't find environment 'wrong'.",
            wrongVersion: "Can't find version 'wrong'.",
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

        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
//        conditionallyDeleteProject(projectName)
    }


    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetDeploymentProjectsForPlan: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)

        given: "Tests parameters for procedure"
        def runParams = [
                config: configName,
                planKey: planKey,
                projectKey: projectKey,
                resultFormat: resultFormat,
                resultPropertySheet: resultPropertySheet,
        ]

        when: "Run procedure TriggerDeployment"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        def propertyName = resultPropertySheet.split("/")[2]

        def projectsInfo = bambooClient.getDeploymentProjectsForPlan(projectKey, planKey)
        for (def i=0; i<projectsInfo.size(); i++) {
            projectsInfo[i].environmentNames = projectsInfo[i].environments[0].name
            projectsInfo[i].environments[0].key = projectsInfo[i].environments[0].key.key
            projectsInfo[i].environments[0].remove('operations')
            projectsInfo[i].planKey = projectsInfo[i].planKey.key
            projectsInfo[i].key = projectsInfo[i].key.key
            projectsInfo[i].remove('operations')
            projectsInfo[i].remove('oid')
        }

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        testCaseHelper.addExpectedResult("Job Summary: $expectedSummary")
        assert jobSummary == expectedSummary

        if (resultPropertySheet == 'json') {
            testCaseHelper.addExpectedResult("Job property propertyName : $expectedOutcome")
            assert new JsonSlurper().parseText(jobProperties[propertyName]) == projectsInfo
        }

        where:
        caseId     | configName   | projectKey        | planKey |  resultFormat    | resultPropertySheet             | expectedOutcome | expectedSummary             | expectedLog
        TC.C388170 | CONFIG_NAME  | 'PROJECT'         | 'PLAN'  | 'json'           | '/myJob/deploymentProjectKeys'  | 'success'       | expectedSummaries.default   | expectedLogs.default
//        TC.C388170 | CONFIG_NAME  | 'PROJECT'         | 'PLAN'  | 'propertySheet'  | '/myJob/deploymentProjectKeys ' | 'success'       | expectedSummaries.default   | expectedLogs.default
    }

}
