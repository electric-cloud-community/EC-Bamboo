package com.electriccloud.plugin.spec.qaTests


import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.utils.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
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
            C388187: [ids: 'C388187', description: 'GetDeploymentProjectsForPlan json format'],
            C388188: [ids: 'C388188', description: 'GetDeploymentProjectsForPlan propertySheet format'],
            C388189: [ids: 'C388189', description: 'GetDeploymentProjectsForPlan don`t save result'],
            C388190: [ids: 'C388190', description: 'GetDeploymentProjectsForPlan empty plan'],

            C388191: [ids: 'C388191', description: 'empty config'],
            C388192: [ids: 'C388192', description: 'empty project'],
            C388193: [ids: 'C388193', description: 'empty plan'],
            C388194: [ids: 'C388194', description: 'wrong config'],
            C388195: [ids: 'C388195', description: 'wrong project'],
            C388196: [ids: 'C388196', description: 'wrong plan'],
            C388197: [ids: 'C388197', description: 'wrong format'],
    ]

    static def testCaseHelper
    static def bambooClient

    static def expectedSummaries = [
            default:     "Deployment projects info saved to property(ies).",
            empty: "No deployment projects found for plan: PROJECT-FAIL",
            error: "No deployment projects found for plan: PROJECTKEY-PLANKEY",
    ]

    static def expectedLogs = [
            default:     'Request URI: http://bamboo-server:8085/rest/api/latest/deploy/project/forPlan?planKey=PROJECTKEY-PLANKEY',
            defaultError: "Possible exception in logs; check job",
            wrongFormat: "Wrong Result Property Format provided. Has to be one of 'none', 'propertySheet', 'json'",
    ]

    def doSetupSpec() {
		createDefaultProject()
        testCaseHelper = new TestCaseHelper(procedureName)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: procedureName, params: runParams]

        bambooClient = initBambooClient()
    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def 'GetDeploymentProjectsForPlan: Sanity #caseId.ids #caseId.description'() {
        given: "Tests parameters for procedure"
        def runParams = [
                config: configName,
                planKey: planKey,
                projectKey: projectKey,
                resultFormat: resultFormat,
                resultPropertySheet: resultPropertySheet,
        ]

        when: "Run procedure TriggerDeployment"

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        def projectsInfo = getDeploymentProjectsForPlan(projectKey, planKey, resultFormat)

        def propertyName = resultPropertySheet.split("/")[2]
        then: "Verify results"
        assert result.outcome == expectedOutcome

        assert jobSummary == expectedSummary

        if (expectedOutcome == 'success') {
            if (resultFormat == 'json') {
                assert outputParameters.deploymentProjectKeys == projectsInfo.collect { it.id }.join(', ')

                assert new JsonSlurper().parseText(jobProperties[propertyName]) == projectsInfo
            }

            if (resultFormat == 'propertySheet') {
                assert outputParameters.deploymentProjectKeys == projectsInfo.keys.replace(',', ', ')

                assert jobProperties[propertyName] == projectsInfo
            }

            if (resultFormat == 'none') {
                assert outputParameters.deploymentProjectKeys == projectsInfo.collect { it.id }.join(', ')

                assert !jobProperties[propertyName]
            }
        }

        if (expectedOutcome == 'warning') {
            assert !outputParameters.deploymentProjectKeys

            assert !jobProperties[propertyName]
        }

        assert result.logs.contains(expectedLog
                .replace('PROJECTKEY', projectKey)
                .replace('PLANKEY', planKey))
        where:
        caseId     | configName   | projectKey        | planKey |  resultFormat    | resultPropertySheet             | expectedOutcome | expectedSummary             | expectedLog
        TC.C388187 | CONFIG_NAME  | 'PROJECT'         | 'PLAN'  | 'json'           | '/myJob/deploymentProjectKeys'  | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388190 | CONFIG_NAME  | 'PROJECT'         | 'FAIL'  | 'json'           | '/myJob/deploymentProjectKeys'  | 'warning'       | expectedSummaries.empty     | expectedLogs.default
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

        def projectsInfo = getDeploymentProjectsForPlan(projectKey, planKey, resultFormat)

        def propertyName = resultPropertySheet.split("/")[2]
        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        testCaseHelper.addExpectedResult("Job Summary: $expectedSummary")
        assert jobSummary == expectedSummary

        if (expectedOutcome == 'success') {
            if (resultFormat == 'json') {
                testCaseHelper.addExpectedResult("OutputParameter deploymentProjectKeys: ${projectsInfo.collect { it.id }.join(', ')}")
                assert outputParameters.deploymentProjectKeys == projectsInfo.collect { it.id }.join(', ')

                testCaseHelper.addExpectedResult("Job property $propertyName : $projectsInfo")
                assert new JsonSlurper().parseText(jobProperties[propertyName]) == projectsInfo
            }

            if (resultFormat == 'propertySheet') {
                testCaseHelper.addExpectedResult("OutputParameter deploymentProjectKeys: ${projectsInfo.keys}")
                assert outputParameters.deploymentProjectKeys == projectsInfo.keys.replace(',', ', ')

                assert jobProperties[propertyName] == projectsInfo
            }

            if (resultFormat == 'none') {
                testCaseHelper.addExpectedResult("OutputParameter deploymentProjectKeys: ${projectsInfo.collect { it.id }.join(', ')}")
                assert outputParameters.deploymentProjectKeys == projectsInfo.collect { it.id }.join(', ')

                testCaseHelper.addExpectedResult("Job property $propertyName shouldn't exist")
                assert !jobProperties[propertyName]
            }
        }

        if (expectedOutcome == 'warning') {
            testCaseHelper.addExpectedResult("OutputParameter deploymentProjectKeys shouldn't exist")
            assert !outputParameters.deploymentProjectKeys

            testCaseHelper.addExpectedResult("Job property shouldn't exist")
            assert !jobProperties[propertyName]
        }

        testCaseHelper.addExpectedResult("Job logs contains: $expectedLog")
        assert result.logs.contains(expectedLog
                .replace('PROJECTKEY', projectKey)
                .replace('PLANKEY', planKey))
        where:
        caseId     | configName   | projectKey        | planKey |  resultFormat    | resultPropertySheet             | expectedOutcome | expectedSummary             | expectedLog
        TC.C388187 | CONFIG_NAME  | 'PROJECT'         | 'PLAN'  | 'json'           | '/myJob/deploymentProjectKeys'  | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388188 | CONFIG_NAME  | 'PROJECT'         | 'PLAN'  | 'propertySheet'  | '/myJob/deploymentProjectKeys'  | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388189 | CONFIG_NAME  | 'PROJECT'         | 'PLAN'  | 'none'           | '/myJob/deploymentProjectKeys'  | 'success'       | expectedSummaries.default   | expectedLogs.default
        TC.C388190 | CONFIG_NAME  | 'PROJECT'         | 'FAIL'  | 'json'           | '/myJob/deploymentProjectKeys'  | 'warning'       | expectedSummaries.empty     | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetDeploymentProjectsForPlan: Negative #caseId.ids #caseId.description'() {
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

        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        if (expectedSummary) {
            testCaseHelper.addExpectedResult("Job Summary: $expectedSummary")
            assert jobSummary == expectedSummary
                    .replace('PROJECTKEY', projectKey)
                    .replace('PLANKEY', planKey)
        }

        testCaseHelper.addExpectedResult("Job logs contains: $expectedLog")
        assert result.logs.contains(expectedLog
                .replace('PROJECTKEY', projectKey)
                .replace('PLANKEY', planKey))
        where:
        caseId     | configName   | projectKey        | planKey |  resultFormat    | resultPropertySheet             | expectedOutcome | expectedSummary             | expectedLog
        TC.C388191 | ''           | 'PROJECT'         | 'PLAN'  | 'json'           | '/myJob/deploymentProjectKeys'  | 'error'         | null                        | expectedLogs.defaultError
        TC.C388192 | CONFIG_NAME  | ''                | 'PLAN'  | 'json'           | '/myJob/deploymentProjectKeys'  | 'error'         | null                        | expectedLogs.defaultError
        TC.C388193 | CONFIG_NAME  | 'PROJECT'         | ''      | 'json'           | '/myJob/deploymentProjectKeys'  | 'error'         | null                        | expectedLogs.defaultError
        TC.C388194 | 'wrong'      | 'PROJECT'         | 'PLAN'  | 'json'           | '/myJob/deploymentProjectKeys'  | 'error'         | null                        | expectedLogs.defaultError
        TC.C388195 | CONFIG_NAME  | 'WRONG'           | 'PLAN'  | 'json'           | '/myJob/deploymentProjectKeys'  | 'warning'       | expectedSummaries.error     | expectedSummaries.error
        TC.C388196 | CONFIG_NAME  | 'PROJECT'         | 'WRONG' | 'json'           | '/myJob/deploymentProjectKeys'  | 'warning'       | expectedSummaries.error     | expectedSummaries.error
        TC.C388197 | CONFIG_NAME  | 'PROJECT'         | 'PLAN'  | 'wrong'          | '/myJob/deploymentProjectKeys'  | 'error'         | null                        | ''

    }

    def getDeploymentProjectsForPlan(def projectKey, def planKey, def resultFormat){
        def projectsInfo = bambooClient.getDeploymentProjectsForPlan(projectKey, planKey)
        if (resultFormat == 'json') {
            for (def i = 0; i < projectsInfo.size(); i++) {
                projectsInfo[i].environmentNames = projectsInfo[i].environments[0].name
                projectsInfo[i].environments[0].key = projectsInfo[i].environments[0].key.key
                projectsInfo[i].environments[0].remove('operations')
                projectsInfo[i].environments[0].remove('description')
                projectsInfo[i].planKey = projectsInfo[i].planKey.key
                projectsInfo[i].key = projectsInfo[i].key.key
                projectsInfo[i].remove('operations')
                projectsInfo[i].remove('oid')
                projectsInfo[i].remove('description')
            }
        }
        if (resultFormat == 'propertySheet') {
            for (def i = 0; i < projectsInfo.size(); i++) {
                projectsInfo[i].environmentNames = projectsInfo[i].environments[0].name
                projectsInfo[i].environments[0].key = projectsInfo[i].environments[0].key.key
                projectsInfo[i].environments[0].remove('operations')
                projectsInfo[i].environments[0].remove('description')
                projectsInfo[i].planKey = projectsInfo[i].planKey.key
                projectsInfo[i].key = projectsInfo[i].key.key
                projectsInfo[i].remove('operations')
                projectsInfo[i].remove('oid')
                projectsInfo[i].remove('description')
            }
            def tmpProjectsInfo = projectsInfo
            projectsInfo = [:]
            tmpProjectsInfo.each{
                projectsInfo[it.id.toString()] = it
                def envs = it.environments
                projectsInfo[it.id.toString()].environments = [:]
                for (def env in envs){
                    projectsInfo[it.id.toString()].environments[env.key.toString()] = env
                }
                projectsInfo[it.id.toString()].environments.keys = projectsInfo[it.id.toString()].environments.collect { k,v -> k} .join(',')
            }
            projectsInfo.keys = projectsInfo.collect {k, v -> k}.join(',')
            convertMapValueToString(projectsInfo)

        }
        return projectsInfo
    }

    def convertMapValueToString(def map){
        for (def entry in map) {
            if (entry.value instanceof Map){
                convertMapValueToString(entry.value)
            }
            else {
                entry.value = entry.value.toString()
            }
        }
    }

}
