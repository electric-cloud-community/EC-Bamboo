package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.NewFeature
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
            C388093: [ids: 'C388093', description: 'GetAllPlans: get plans - property format: propertySheet'],

    ]

    static def testCaseHelper
    static def bambooClient
    static def commanderAddress = System.getProperty("COMMANDER_SERVER")


    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        bambooClient = new BambooClient('http', commanderAddress,  '8085', '', BAMBOO_USERNAME, BAMBOO_PASSWORD)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: procedureName, params: getAllPlansParams]

    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
//        conditionallyDeleteProject(projectName)
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'CreatePoolMember: Positive #caseId.ids #caseId.description'() {
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

        def plansInfo = jsonBamboResponse.plans.plan
        // procedure doesn't use all fields from response
        // and save only part of them:
        plansInfo.each {
            it.remove('expand')
            it.remove('project')
            it.remove('shortKey')
            it.url = it.link.href.replace(commanderAddress, 'bamboo-server')
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
            if (resultFormat == 'propertySheet') {
                it.averageBuildTimeInSeconds = Math.round(it.averageBuildTimeInSeconds).toString()
            }
        }

        def plansKey = jsonBamboResponse.plans.plan.collect { it.key}
        def propertyName = resultPropertySheet.split("/")[2]

        then: "Verify results"
        verifyAll {
            testCaseHelper.addExpectedResult("Job status: success")
            result.outcome == 'success'

            testCaseHelper.addExpectedResult("Job Summary: $expectedSummary")
            jobSummary == expectedSummary

            testCaseHelper.addExpectedResult("OutputParameter planKeys should contans actual value, list of all projects keys : $plansKey")
            outputParameters.planKeys.split(', ') - plansKey == []

            if (resultFormat == 'json') {
                testCaseHelper.addExpectedResult("Job property $propertyName: $jsonBamboResponse")
                new JsonSlurper().parseText(jobProperties[propertyName]) == plansInfo
            }

            if (resultFormat == 'propertySheet') {
                def mapPlansInfo = [:]
                plansInfo.each{
//                    if (!it.description){
                        it.remove('description')
//                    }
                    mapPlansInfo[it['key']] = it
                }
                mapPlansInfo.each{ k, v ->
                    testCaseHelper.addExpectedResult("Job property $propertyName - $k: ${mapPlansInfo[k]}")
                    mapPlansInfo[k].each {k1, v1 ->
                        assert mapPlansInfo[k][k1].toString() == jobProperties[propertyName][k][k1]
                    }
                }
                testCaseHelper.addExpectedResult("Job property $propertyName - keys: ${jobProperties[propertyName]['keys']}")
                jobProperties[propertyName]['keys'].split(',') == plansKey
            }


            testCaseHelper.addExpectedResult("Job logs: Found project: '$projectKey'")
            result.logs.contains("Found project: '$projectKey'")
            plansKey.each {
                testCaseHelper.addExpectedResult("Job logs: Found plan: '$it'")
                result.logs.contains("Found plan: '$it'")
            }

        }

        cleanup:

        where:
        caseId     | configName   | projectKey     | resultFormat    | resultPropertySheet  | expectedSummary
        TC.C388091 | CONFIG_NAME  | 'PROJECT'      | 'json'          | '/myJob/plans'       | 'Found 4 plan(s).'
        TC.C388092 | CONFIG_NAME  | 'PROJECT'      | 'propertySheet' | '/myJob/plans'       | 'Found 4 plan(s).'
        TC.C388093 | CONFIG_NAME  | 'PROJECT'      | 'none'          | '/myJob/plans'       | 'Found 4 plan(s).'
    }
}
