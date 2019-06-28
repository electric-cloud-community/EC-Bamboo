package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*
import groovy.json.JsonSlurper

class GetAllPlansTestsSuite extends PluginTestHelper {

    static def procedureName = 'GetAllPlans'
    static def projectName = 'TestProject: GetAllPlans'
    static def getAllPlansParams = [
            config : '',
            projectKey : '',
            resultFormat : '',
            resultPropertySheet : '',
    ]

    static def TC = [
            C388091: [ids: 'C388091', description: 'GetAllPlans: get plans - property format: json'],
            C388092: [ids: 'C388092', description: 'GetAllPlans: get plans - property format: propertySheet'],
            C388093: [ids: 'C388093', description: 'GetAllPlans: get plans - property format: none'],
            C388096: [ids: 'C388096', description: 'GetAllPlans: get all plans, projectKey - empty '],
            C388097: [ids: 'C388097', description: 'empty config'],
            C388098: [ids: 'C388098', description: 'wrong config'],
            C388099: [ids: 'C388099', description: 'wrong project'],
            C388100: [ids: 'C388100', description: 'wrong result format'],
    ]

    static def testCaseHelper
    static def bambooClient

    static def expectedSummaries = [
            default:     "Found COUNT plan(s).",
            wrongPoject: "Can't find project by key: wrong",
    ]

    static def expectedLogs = [
            default:     "'Found COUNT plan(s).'",
            defaultError:"Possible exception in logs; check job",
            emptyConfig: "Parameter 'config' of procedure 'GetAllPlans' is marked as required, but it does not have a value. Aborting with fatal error.",
            wrongConfig: "Can't get config: Config does not exist",
            wrongPoject: "Can\\'t find project by key: wrong"
    ]




    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
        createConfiguration(PluginTestHelper.CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: procedureName, params: getAllPlansParams]

    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PluginTestHelper.PLUGIN_NAME, PluginTestHelper.CONFIG_NAME)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def 'GetAllPlans: Sanity #caseId.ids #caseId.description'() {

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config : configName,
                projectKey : projectKey,
                resultFormat : resultFormat,
                resultPropertySheet : resultPropertySheet,
        ]

        when: "Run procedure"

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)
        def jsonBamboResponse = bambooClient.getPlans(projectKey)

        def plansInfo = projectKey ? jsonBamboResponse.plans.plan : jsonBamboResponse.projects.project[0].plans.plan

        // procedure doesn't use all fields from response
        // and save only part of them:
        plansInfo.each {
            it.remove('expand')
            it.remove('project')
            it.remove('shortKey')
            it.url = it.link.href.replace(PluginTestHelper.commanderAddress, 'bamboo-server')
            if (!it.description) {
                it.description = null
            }
            it.remove('link')
            it.remove('isFavourite')
            it.remove('isActive')
            it.remove('actions')
            it.stagesSize = it.stages.size
            it.remove('stages')
            it.remove('branches')
            it.remove('planKey')
            it.planDescription = it.description
            it.remove('description')
            if (resultFormat == 'propertySheet') {
                it.averageBuildTimeInSeconds = Math.round(it.averageBuildTimeInSeconds).toString()
                if (!it.planDescription){
                    it.remove('planDescription')
                }
            }
        }

        def plansKey = projectKey ? jsonBamboResponse.plans.plan.collect { it.key} : jsonBamboResponse.projects.project[0].plans.plan.collect { it.key}
        def propertyName = resultPropertySheet.split("/")[2]

        projectKey = '' ?: 'PROJECT'
        then: "Verify results"

        assert result.outcome == 'success'

        assert jobSummary == expectedSummary.replace('COUNT', plansInfo.size().toString())

        assert outputParameters.planKeys.split(', ') - plansKey == []

        if (resultFormat == 'json') {
            assert new JsonSlurper().parseText(jobProperties[propertyName]) == plansInfo
        }

        if (resultFormat == 'propertySheet') {
            def mapPlansInfo = [:]
            plansInfo.each{
                mapPlansInfo[it['key']] = it
            }
            mapPlansInfo.each{ k, v ->
                mapPlansInfo[k].each {k1, v1 ->
                    assert mapPlansInfo[k][k1].toString() == jobProperties[propertyName][k][k1]
                }
            }
            assert jobProperties[propertyName]['keys'].split(',') == plansKey
        }


        assert result.logs.contains("Found project: '$projectKey'")
        plansKey.each {
            assert result.logs.contains("Found plan: '$it'")
        }

        cleanup:

        where:
        caseId     | configName                   | projectKey | resultFormat | resultPropertySheet | expectedSummary
        TC.C388091 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'json'       | '/myJob/plans'      | expectedSummaries.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetAllPlans: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config : configName,
                projectKey : projectKey,
                resultFormat : resultFormat,
                resultPropertySheet : resultPropertySheet,
        ]

        when: "Run procedure"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)
        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)
        def jsonBamboResponse = bambooClient.getPlans(projectKey)

        def plansInfo = projectKey ? jsonBamboResponse.plans.plan : jsonBamboResponse.projects.project[0].plans.plan

        // procedure doesn't use all fields from response
        // and save only part of them:
        plansInfo.each {
            it.remove('expand')
            it.remove('project')
            it.remove('shortKey')
            it.url = it.link.href.replace(PluginTestHelper.commanderAddress, 'bamboo-server')
            if (!it.description) {
                it.description = null
            }
            it.remove('link')
            it.remove('isFavourite')
            it.remove('isActive')
            it.remove('actions')
            it.stagesSize = it.stages.size
            it.remove('stages')
            it.remove('branches')
            it.remove('planKey')
            it.planDescription = it.description
            it.remove('description')
            if (resultFormat == 'propertySheet') {
                it.averageBuildTimeInSeconds = Math.round(it.averageBuildTimeInSeconds).toString()
                if (!it.planDescription){
                    it.remove('planDescription')
                }
            }
        }

        def plansKey = projectKey ? jsonBamboResponse.plans.plan.collect { it.key} : jsonBamboResponse.projects.project[0].plans.plan.collect { it.key}
        def propertyName = resultPropertySheet.split("/")[2]

        projectKey = '' ?: 'PROJECT'
        then: "Verify results"

        testCaseHelper.addExpectedResult("Job status: success")
        assert result.outcome == 'success'

        testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace('COUNT', plansInfo.size().toString())}")
        assert jobSummary == expectedSummary.replace('COUNT', plansInfo.size().toString())

        testCaseHelper.addExpectedResult("OutputParameter planKeys should contans actual value, list of all projects keys : $plansKey")
        assert outputParameters.planKeys.split(', ') - plansKey == []

        if (resultFormat == 'json') {
            testCaseHelper.addExpectedResult("Job property $propertyName: $jsonBamboResponse")
            assert new JsonSlurper().parseText(jobProperties[propertyName]) == plansInfo
        }

        if (resultFormat == 'propertySheet') {
            def mapPlansInfo = [:]
            plansInfo.each{
                mapPlansInfo[it['key']] = it
            }
            mapPlansInfo.each{ k, v ->
                testCaseHelper.addExpectedResult("Job property $propertyName - $k: ${mapPlansInfo[k]}")
                mapPlansInfo[k].each {k1, v1 ->
                    assert mapPlansInfo[k][k1].toString() == jobProperties[propertyName][k][k1]
                }
            }
            testCaseHelper.addExpectedResult("Job property $propertyName - keys: ${jobProperties[propertyName]['keys']}")
            assert jobProperties[propertyName]['keys'].split(',') == plansKey
        }


        testCaseHelper.addExpectedResult("Job logs: Found project: '$projectKey'")
        assert result.logs.contains("Found project: '$projectKey'")
        plansKey.each {
            testCaseHelper.addExpectedResult("Job logs: Found plan: '$it'")
            assert result.logs.contains("Found plan: '$it'")
        }

        cleanup:

        where:
        caseId     | configName                   | projectKey | resultFormat    | resultPropertySheet | expectedSummary
        TC.C388091 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'json'          | '/myJob/plans'      | expectedSummaries.default
        TC.C388092 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'propertySheet' | '/myJob/plans'      | expectedSummaries.default
        TC.C388093 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'none'          | '/myJob/plans'      | expectedSummaries.default
        TC.C388096 | PluginTestHelper.CONFIG_NAME | ''         | 'json'          | '/myJob/plans'      | expectedSummaries.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'GetAllPlans: Negative #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)

        given: "Tests parameters for procedure LTM CreatePoolMemberTests"
        def runParams = [
                config : configName,
                projectKey : projectKey,
                resultFormat : resultFormat,
                resultPropertySheet : resultPropertySheet,
        ]

        when: "Run procedure"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)
        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        then: "Verify results"

        testCaseHelper.addExpectedResult("Job status: error")
        assert result.outcome == 'error'

        testCaseHelper.addExpectedResult("Job Summary: $expectedSummary")
        assert jobSummary == expectedSummary
        testCaseHelper.addExpectedResult("Job status: $expectedLog")
        assert result.logs.contains(expectedLog)
        where:
        caseId     | configName                   | projectKey | resultFormat    | resultPropertySheet | expectedSummary               | expectedLog
        TC.C388097 | ''                           | 'PROJECT'  | 'propertySheet' | '/myJob/plans'      | null                          | expectedLogs.defaultError
        TC.C388098 | 'wrong'                      | 'PROJECT'  | 'propertySheet' | '/myJob/plans'      | null                          | expectedLogs.defaultError
        TC.C388099 | PluginTestHelper.CONFIG_NAME | 'wrong'    | 'propertySheet' | '/myJob/plans'      | expectedSummaries.wrongPoject | expectedLogs.wrongPoject
        TC.C388100 | PluginTestHelper.CONFIG_NAME | 'PROJECT'  | 'wrong'         | '/myJob/plans'      | null                          | ''

    }
}
