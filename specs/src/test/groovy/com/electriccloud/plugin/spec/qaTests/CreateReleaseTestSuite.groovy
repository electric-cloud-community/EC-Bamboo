package com.electriccloud.plugin.spec.qaTests

import com.electriccloud.plugin.spec.BambooClient
import com.electriccloud.plugin.spec.PluginTestHelper
import com.electriccloud.plugin.spec.TestCaseHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import groovy.json.JsonSlurper
import spock.lang.Unroll

class CreateReleaseTestSuite extends PluginTestHelper{

    static def procedureName = 'CreateRelease'

    static def projectName = 'TestProject: CreateRelease'

    static def runParams = [
            config: "",
            deploymentProjectName: "",
            planBuildKey: "",
            releaseName: "",
            requestReleaseName: "",
            resultFormat: "",
            resultPropertySheet: "",
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
            C388198: [ids: 'C388198', description: 'Create release specify release Name, result format json format'],
            C388199: [ids: 'C388199', description: 'Create release specify release Name, result format property format'],
            C388200: [ids: 'C388200', description: 'Create release receive release Name from bamboo, result format json format'],
            C388201: [ids: 'C388201', description: 'Create release receive release Name from bamboo, result format property format'],
            C388202: [ids: 'C388202', description: 'Create release specify release Name, don`t save result'],

            C388203: [ids: 'C388203', description: 'empty config'],
            C388204: [ids: 'C388204', description: 'empty deploymentProjectName'],
            C388205: [ids: 'C388205', description: 'empty planBuildKey'],
            C388206: [ids: 'C388206', description: 'wrong config'],
            C388207: [ids: 'C388207', description: 'wrong deploymentProjectName'],
            C388208: [ids: 'C388208', description: 'wrong Format of planBuildKey'],
            C388211: [ids: 'C388211', description: 'wrong planBuildKey'],
            C388209: [ids: 'C388209', description: 'wrong format'],
            C388210: [ids: 'C388210', description: 'releaseName already exists'],
    ]

    static def testCaseHelper
    static def bambooClient
    static def bambooProject = 'PROJECT'
    static def bambooDeployFromPlan = 'PLAN'
    static def bambooDeployProject = 'Deployment Project'
    static def successfulPlanRunKey

    static def expectedSummaries = [
            default:     "Created release: RELEASENAME",
            wrongProject: "Can't find deployment project with name 'wrong'",
            wrongPlanFormat: "No specific error message was returned. Check logs for details",
            wrongPlan: "Can't find deployment project with name '$bambooDeployProject'",
            alreadyExists: "Request errors: \n" +
                    " versionName : This release version is already in use, please select another."
    ]

    static def expectedLogs = [
            default:     ['Request URI: http://bamboo-server:8085/rest/api/latest/deploy/project/forPlan?planKey=PROJECT-PLAN',
                          'Request URI: http://bamboo-server:8085/rest/api/latest/deploy/project/DEPLOYID/version'],
            defaultError: "Possible exception in logs; check job",
            wrongPlanFormat: "Please provide a valid plan key",
            wrongFormat: "Wrong Result Property Format provided. Has to be one of 'none', 'propertySheet', 'json'",
            alreadyExists: "Received error while performing the request.",
    ]

    def doSetupSpec() {
        testCaseHelper = new TestCaseHelper(procedureName)
        createConfiguration(CONFIG_NAME)
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: procedureName, params: runParams]
        dslFile "dsl/procedure.dsl", [projectName: projectName, resName: 'local', procedureName: 'RunPlan', params: runPlanParams]

        bambooClient = initBambooClient()
        successfulPlanRunKey = runPlan(bambooProject, bambooDeployFromPlan)
    }

    def doCleanupSpec() {
        testCaseHelper.createTestCases()
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def 'CreateReleaseTestSuite: Sanity #caseId.ids #caseId.description'() {
        given: "Tests parameters for procedure"
        def runParams = [
                config               : configName,
                deploymentProjectName: deploymentProjectName,
                planBuildKey         : planBuildKey,
                releaseName          : releaseName,
                requestReleaseName   : requestReleaseName,
                resultFormat         : resultFormat,
                resultPropertySheet  : resultPropertySheet,
        ]

        when: "Run procedure TriggerDeployment"

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        def deployProjectId
        bambooClient.getDeploymentProjectsForPlan(planBuildKey.split('-')[0], planBuildKey.split('-')[1]).each {
            if (it.name == deploymentProjectName){
                deployProjectId = it.id
            }
        }
        def releaseInfo = bambooClient.getReleaseInfo(deployProjectId, releaseName)
        (releaseInfo.collect{ it.key } - ["creatorDisplayName", "creationDate", "planBranchName", "name", "id"]).each {
            releaseInfo.remove(it)
        }
        if (!releaseName){
            releaseName = releaseInfo.name
        }

        def propertyName = resultPropertySheet.split("/")[2]
        then: "Verify results"
        assert result.outcome == expectedOutcome

        assert jobSummary == expectedSummary
                .replace('RELEASENAME', releaseName)

        for (def log in expectedLog) {
            assert result.logs.contains(log
                    .replace('DEPLOYID', deployProjectId.toString() ))
        }

        assert outputParameters['release'] == releaseName

        if (resultFormat == 'json') {
            assert releaseInfo == new JsonSlurper().parseText(jobProperties[propertyName])
        }

        if (resultFormat == 'propertySheet') {
            assert releaseInfo.collectEntries { k, v -> [(k): v.toString()] } == jobProperties[propertyName]
        }

        if (resultFormat == 'none') {
            assert !jobProperties[propertyName]
        }

        where:
        caseId     | configName  | deploymentProjectName | planBuildKey         | releaseName            | requestReleaseName | resultFormat       | resultPropertySheet | expectedOutcome | expectedSummary           | expectedLog
        TC.C388198 | CONFIG_NAME | bambooDeployProject   | successfulPlanRunKey | randomize('QArelease') | '0'                | 'json'             | '/myJob/release'    | 'success'       | expectedSummaries.default | expectedLogs.default
        TC.C388201 | CONFIG_NAME | bambooDeployProject   | successfulPlanRunKey | ''                     | '1'                | 'propertySheet'    | '/myJob/release'    | 'success'       | expectedSummaries.default | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'CreateReleaseTestSuite: Positive #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition("Build $planBuildKey should exist")
        testCaseHelper.testCasePrecondition("Deployment project $deploymentProjectName should exist")

        given: "Tests parameters for procedure"
        def runParams = [
                config               : configName,
                deploymentProjectName: deploymentProjectName,
                planBuildKey         : planBuildKey,
                releaseName          : releaseName,
                requestReleaseName   : requestReleaseName,
                resultFormat         : resultFormat,
                resultPropertySheet  : resultPropertySheet,
        ]

        when: "Run procedure TriggerDeployment"

        testCaseHelper.addStepContent("Run procedure $procedureName with parameters:", runParams)

        def result = runProcedure(projectName, procedureName, runParams)
        def jobSummary = getStepSummary(result.jobId, procedureName)

        def outputParameters = getJobOutputParameters(result.jobId, 1)
        def jobProperties = getJobProperties(result.jobId)

        def deployProjectId
        bambooClient.getDeploymentProjectsForPlan(planBuildKey.split('-')[0], planBuildKey.split('-')[1]).each {
            if (it.name == deploymentProjectName){
                deployProjectId = it.id
            }
        }
        def releaseInfo = bambooClient.getReleaseInfo(deployProjectId, releaseName)
        (releaseInfo.collect{ it.key } - ["creatorDisplayName", "creationDate", "planBranchName", "name", "id"]).each {
            releaseInfo.remove(it)
        }
        if (!releaseName){
            releaseName = releaseInfo.name
        }

        def propertyName = resultPropertySheet.split("/")[2]
        then: "Verify results"
        testCaseHelper.addExpectedResult("Job status: $expectedOutcome")
        assert result.outcome == expectedOutcome

        testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace('RELEASENAME', releaseName)}")
        assert jobSummary == expectedSummary
                .replace('RELEASENAME', releaseName)

        for (def log in expectedLog) {
            testCaseHelper.addExpectedResult("Job logs contains: $log")
            assert result.logs.contains(log
                    .replace('DEPLOYID', deployProjectId.toString() ))
        }

        testCaseHelper.addExpectedResult("OutputParameter release: $releaseName")
        assert outputParameters['release'] == releaseName

        if (resultFormat == 'json') {
            testCaseHelper.addExpectedResult("Job property  $propertyName: $releaseInfo")
            assert releaseInfo == new JsonSlurper().parseText(jobProperties[propertyName])
        }

        if (resultFormat == 'propertySheet') {
            jobProperties[propertyName].each{
                testCaseHelper.addExpectedResult("Job property  ${it.key}: ${it.value}")
            }
            assert releaseInfo.collectEntries { k, v -> [(k): v.toString()] } == jobProperties[propertyName]
        }

        if (resultFormat == 'none') {
            testCaseHelper.addExpectedResult("Job property  $propertyName shouldn't exist")
            assert !jobProperties[propertyName]
        }

        where:
        caseId     | configName  | deploymentProjectName | planBuildKey         | releaseName            | requestReleaseName | resultFormat       | resultPropertySheet | expectedOutcome | expectedSummary           | expectedLog
        TC.C388198 | CONFIG_NAME | bambooDeployProject   | successfulPlanRunKey | randomize('QArelease') | '0'                | 'json'             | '/myJob/release'    | 'success'       | expectedSummaries.default | expectedLogs.default
        TC.C388199 | CONFIG_NAME | bambooDeployProject   | successfulPlanRunKey | randomize('QArelease') | '0'                | 'propertySheet'    | '/myJob/release'    | 'success'       | expectedSummaries.default | expectedLogs.default
        TC.C388200 | CONFIG_NAME | bambooDeployProject   | successfulPlanRunKey | ''                     | '1'                | 'json'             | '/myJob/release'    | 'success'       | expectedSummaries.default | expectedLogs.default
        TC.C388201 | CONFIG_NAME | bambooDeployProject   | successfulPlanRunKey | ''                     | '1'                | 'propertySheet'    | '/myJob/release'    | 'success'       | expectedSummaries.default | expectedLogs.default
        TC.C388202 | CONFIG_NAME | bambooDeployProject   | successfulPlanRunKey | randomize('QArelease') | '0'                | 'none'             | '/myJob/release'    | 'success'       | expectedSummaries.default | expectedLogs.default
    }

    @NewFeature(pluginVersion = "1.5.0")
    @Unroll
    def 'CreateReleaseTestSuite: Negative #caseId.ids #caseId.description'() {
        testCaseHelper.createNewTestCase(caseId.ids, caseId.description)
        testCaseHelper.testCasePrecondition("Build $planBuildKey should exist")
        testCaseHelper.testCasePrecondition("Deployment project $deploymentProjectName should exist")

        if (releaseName == 'ALREADY_EXISTS'){
            testCaseHelper.testCasePrecondition("release name should exist")

            def deployProjectId
            bambooClient.getDeploymentProjectsForPlan(planBuildKey.split('-')[0], planBuildKey.split('-')[1]).each {
                if (it.name == deploymentProjectName){
                    deployProjectId = it.id
                }
            }
            releaseName = bambooClient.getReleaseInfo(deployProjectId, 'LAST').name
        }

        given: "Tests parameters for procedure"
        def runParams = [
                config               : configName,
                deploymentProjectName: deploymentProjectName,
                planBuildKey         : planBuildKey,
                releaseName          : releaseName,
                requestReleaseName   : requestReleaseName,
                resultFormat         : resultFormat,
                resultPropertySheet  : resultPropertySheet,
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
            testCaseHelper.addExpectedResult("Job Summary: ${expectedSummary.replace('RELEASENAME', releaseName)}")
            assert jobSummary == expectedSummary
                    .replace('RELEASENAME', releaseName)
        }

        testCaseHelper.addExpectedResult("Job logs contains: $expectedLog")
        assert result.logs.contains(expectedLog)

        if (caseId == TC.C388210){
            testCaseHelper.addExpectedResult("OutputParameter release: $releaseName")
            assert outputParameters['release'] == releaseName
        }
        else {
            testCaseHelper.addExpectedResult("OutputParameter release: $releaseName")
            assert !outputParameters['release']
        }

        testCaseHelper.addExpectedResult("Job property  $propertyName shouldn't exist")
        assert !jobProperties[propertyName]
        where:
        caseId     | configName  | deploymentProjectName | planBuildKey         | releaseName            | requestReleaseName | resultFormat       | resultPropertySheet | expectedOutcome | expectedSummary                   | expectedLog
        TC.C388203 | ''          | bambooDeployProject   | successfulPlanRunKey | randomize('QArelease') | '0'                | 'json'             | '/myJob/release'    | 'error'         | null                              | expectedLogs.defaultError
        TC.C388204 | CONFIG_NAME | ''                    | successfulPlanRunKey | randomize('QArelease') | '0'                | 'json'             | '/myJob/release'    | 'error'         | null                              | expectedLogs.defaultError
        TC.C388205 | CONFIG_NAME | bambooDeployProject   | ''                   | randomize('QArelease') | '0'                | 'json'             | '/myJob/release'    | 'error'         | null                              | expectedLogs.defaultError
        TC.C388206 | 'wrong'     | bambooDeployProject   | successfulPlanRunKey | randomize('QArelease') | '0'                | 'json'             | '/myJob/release'    | 'error'         | null                              | expectedLogs.defaultError
        TC.C388207 | CONFIG_NAME | 'wrong'               | successfulPlanRunKey | randomize('QArelease') | '0'                | 'json'             | '/myJob/release'    | 'error'         | expectedSummaries.wrongProject    | expectedSummaries.wrongProject
        TC.C388208 | CONFIG_NAME | bambooDeployProject   | 'wrong'              | randomize('QArelease') | '0'                | 'json'             | '/myJob/release'    | 'error'         | expectedSummaries.wrongPlanFormat | expectedLogs.wrongPlanFormat
        TC.C388211 | CONFIG_NAME | bambooDeployProject   | 'WRONG-PLAN-11'      | randomize('QArelease') | '0'                | 'json'             | '/myJob/release'    | 'error'         | expectedSummaries.wrongPlan       | expectedSummaries.wrongPlan
        TC.C388209 | CONFIG_NAME | bambooDeployProject   | successfulPlanRunKey | randomize('QArelease') | '0'                | 'wrong'            | '/myJob/release'    | 'error'         | null                              | ''
        TC.C388210 | CONFIG_NAME | bambooDeployProject   | successfulPlanRunKey | 'ALREADY_EXISTS'       | '0'                | 'json'             | '/myJob/release'    | 'warning'       | expectedSummaries.alreadyExists   | expectedLogs.alreadyExists

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
