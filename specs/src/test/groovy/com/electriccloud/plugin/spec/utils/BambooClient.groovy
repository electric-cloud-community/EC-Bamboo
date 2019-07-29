package com.electriccloud.plugin.spec.utils

import com.atlassian.bamboo.specs.api.BambooSpec
import com.atlassian.bamboo.specs.api.builders.Variable
import com.atlassian.bamboo.specs.api.builders.deployment.Deployment
import com.atlassian.bamboo.specs.api.builders.deployment.Environment
import com.atlassian.bamboo.specs.api.builders.deployment.ReleaseNaming
import com.atlassian.bamboo.specs.api.builders.plan.Job
import com.atlassian.bamboo.specs.api.builders.plan.Plan
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier
import com.atlassian.bamboo.specs.api.builders.plan.Stage
import com.atlassian.bamboo.specs.api.builders.plan.artifact.Artifact
import com.atlassian.bamboo.specs.api.builders.project.Project
import com.atlassian.bamboo.specs.builders.repository.git.GitRepository
import com.atlassian.bamboo.specs.builders.task.ArtifactDownloaderTask
import com.atlassian.bamboo.specs.builders.task.CleanWorkingDirectoryTask
import com.atlassian.bamboo.specs.builders.task.CommandTask
import com.atlassian.bamboo.specs.builders.task.DownloadItem
import com.atlassian.bamboo.specs.builders.task.ScriptTask
import com.atlassian.bamboo.specs.builders.task.TestParserTask
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask
import com.atlassian.bamboo.specs.util.BambooServer
import com.atlassian.bamboo.specs.util.SendQueue
import groovy.json.JsonBuilder
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.DELETE

/**
 * https://docs.atlassian.com/atlassian-bamboo/REST/6.8.0/
 * https://docs.atlassian.com/bamboo-specs-docs/6.8.1/specs-java.html
 */
@BambooSpec
class BambooClient {

    def endpoint
    def userName
    def password

    private def http
    private static final def SOCKET_TIMEOUT = 20 * 1000
    private static final def CONNECTION_TIMEOUT = 5 * 1000

    BambooClient(def protocol, def host, def port, def urlPath, def userName, def password) {
        def endpoint = "$protocol://$host:$port"

        if (urlPath) {
            endpoint += "/$urlPath"
        }

        this.endpoint = endpoint

        this.userName = userName
        this.password = password

        this.http = new HTTPBuilder(new URI(endpoint))
        this.http.ignoreSSLIssues()
    }

    def doHttpRequest(def method, def requestUri, def queryArgs = null) {

        http.request(method, JSON) { req ->
            uri.path = requestUri

            uri.query = queryArgs

            headers.'Authorization' =
                    "Basic ${"${this.userName}:${this.password}".bytes.encodeBase64().toString()}"
            req.getParams().setParameter("http.connection.timeout", CONNECTION_TIMEOUT)
            req.getParams().setParameter("http.socket.timeout", SOCKET_TIMEOUT)

            response.success = { resp, json ->
                println "[DEBUG] Request for '${requestUri}' was successful ${resp.statusLine}, code: ${resp.status}: $json"
                return json
            }

            response.failure = { resp ->
                throw new Exception("[ERROR] Request for '${requestUri}' failed with ${resp.statusLine}, code: ${resp.status}, ${resp}");
            }
        }
    }

    def getReleaseInfo(def projectDeployId, def releaseName){
        def result
        def request = doHttpRequest(GET, "/rest/api/latest/deploy/project/$projectDeployId/versions")
        if (releaseName) {
            if (releaseName == 'LAST'){
                return request.versions[0]
            }
            request.versions.each {
                if (it.name == releaseName) {
                    result = it
                }
            }
            return result
        }
        else {
            return request.versions[0]
        }
    }

    def getDeploymentProjectsForPlan(def project, def plan){
        def query = [planKey: "$project-$plan"]
        def projectResults = doHttpRequest(GET, "/rest/api/latest/deploy/project/forPlan", query)
        def projectIds = projectResults.collect{ it.id}
        def projectsInfo = []
        projectIds.each { id ->
            def projectInfo = doHttpRequest(GET, "/rest/api/latest/deploy/project/$id")
            projectsInfo += projectInfo
        }
        return projectsInfo

    }

    def getDeployment(def id){
        def result = doHttpRequest(GET, "/rest/api/latest/deploy/result/$id")
        return result
    }

    def getPlanRuns(def project, def plan, def maxResult, def buildState){
        def query = [expand: "results.result.artifacts,results.result.labels",
                     'max-results': maxResult]
        if (buildState != 'All'){
            query += [buildstate: buildState]
        }
        def result = doHttpRequest(GET, "/rest/api/latest/result/$project-$plan", query)
        return result
    }

    def getPlanRunLogs(def runName){
        def jobName = runName.split('-')
        jobName = (jobName[0,1] + 'JOB1' + jobName[-1]).join('-')
        def query = [expand: "logEntries"]
        def result = doHttpRequest(GET, "/rest/api/latest/result/$jobName", query)
        return result
    }

    def getPlanRunVars(def runName){
        def query = [expand: "variables"]
        def result = doHttpRequest(GET, "/rest/api/latest/result/$runName", query)
        return result
    }

    def getPlanRunInfo(def runPlanName){
        def query = [expand: "artifacts,labels"]
        def result = doHttpRequest(GET, "/rest/api/latest/result/$runPlanName", query)
        return result
    }

    def waitUntiPlanFinished(def runPlanName){
        for (def i=0; i<20; i++){
            def status = getPlanRunInfo(runPlanName).buildState
            if (status == 'Successful') {
                return status
            }
            sleep(5000)
        }

    }

    def changePlanState(def projectKey, def planKey, def state){
        if (state == 'enable') {
            def result = doHttpRequest(POST, "/rest/api/latest/plan/$projectKey-$planKey/enable")
            return result
        }
        if (state == 'disable') {
            def result = doHttpRequest(DELETE, "/rest/api/latest/plan/$projectKey-$planKey/enable")
            return result
        }
    }

    def getPlans(def project){
        def query = project ? [expand: "plans.plan"] : [expand: "projects.project.plans.plan"]
        def result = doHttpRequest(GET, "/rest/api/latest/project/$project", query)
        return result
    }

    def getPlanDetails(def project, def plan){
        def query = [expand: "stages.stage"]
        def result = doHttpRequest(GET, "/rest/api/latest/plan/$project-$plan", query)
        return result
    }

    def createPlan(def projectKey, def planKey, def planName = 'Test QA plan', def countOfStages=0){
        def bambooServer = new BambooServer(this.endpoint)
        Stage[] stagesArray = []

        def task1 = new ScriptTask()
                .description("Running a simple command")
                .inlineBody("#!/bin/bash\necho \'Hello Bamboo!\'")

        for (def i=0; i<countOfStages; i++){
            def job = new Job("Job$i", "JOB$i")
                    .tasks(task1)

            def stage = new Stage("Stage$i")
                    .description("Stage $i")
                    .jobs(job)

            stagesArray += stage
        }

        def project = new Project().key(projectKey)
        def plan = new Plan(project, planName, planKey)
                .stages(stagesArray)
        bambooServer.publish(plan)
        return plan
    }

    def createDefaultPlan(){
        def projectKey='PROJECT'
        def planKey='PLAN'
        def planName='Test Plan'
        def listArtifact = ['jar']

        def planFailKey='FAIL'
        def planFailName='Failing'

        def planLongKey='LONG'
        def planLongName='Long running'

        def planParamsKey='PARAMS'
        def planParamsName='Parametrized message build'


        def bambooServer = new BambooServer(this.endpoint)
        def project = new Project().key(projectKey).name('Test project')

        def artifact = new Artifact("simplejar")
                .location("build/libs/")
                .copyPattern("*.jar")
                .required(true)
                .shared(true)

        def artifactForFailingPlan = new Artifact("simplejar")
                .location("build/libs/")
                .copyPattern("*.jar")
                .required(true)
                .shared(false)

        Artifact[] artifactsArray = []
        if ('jar' in listArtifact){
            artifactsArray += artifact
        }

        def gitRepository = new GitRepository()
                .name("Sample Gradle build1")
                .url('https://github.com/horodchukanton/gradle-test-build.git')

        def plan = new Plan(project, planName, planKey)
                .variables(
                        new Variable('FAIL_MESSAGE', ''),
                        new Variable('SLEEP_TIME', ''),
                        new Variable('TEST_MESSAGE', '')
                )
                .planRepositories(gitRepository)

        def planFail = new Plan(project, planFailName, planFailKey)
                .planRepositories(gitRepository)

        def planLong = new Plan(project, planLongName, planLongKey)
                .planRepositories(gitRepository)

        def planParams = new Plan(project, planParamsName, planParamsKey)
                .variables(
                        new Variable('FAIL_MESSAGE', ''),
                        new Variable('SLEEP_TIME', ''),
                        new Variable('TEST_MESSAGE', '')
                )
                .planRepositories(gitRepository)

        bambooServer.publish(plan)
        bambooServer.publish(planFail)
        bambooServer.publish(planLong)
        bambooServer.publish(planParams)

        def taskSourceCodeCheckout = new VcsCheckoutTask()
                .description('Checkout')
                .addCheckoutOfRepository('Sample Gradle build1')


        def taskGradlew = new CommandTask()
                .description('Gradle Build')
                .executable('gradlew')
                .argument('build')

        def taskGradlewFail = new CommandTask()
                .description('Gradle Build')
                .executable('gradlew')
                .argument('build -PfailMessage="Expected failure"')

        def taskGradlewSuccessfull = new CommandTask()
                .description('Gradle Build')
                .executable('gradlew')
                .argument('build -PtestMessage="I am never supposed to be shown"')

        def taskGradlewLong = new CommandTask()
                .description('Gradle Build')
                .executable('gradlew')
                .argument('build -PsleepTime=30')

        def taskGradlewParam= new CommandTask()
                .description('Gradle Build')
                .executable('gradlew')
                .argument('build -PvariablesSource=environment')
                .environmentVariables('TEST_MESSAGE=${bamboo.TEST_MESSAGE} SLEEP_TIME=${bamboo.SLEEP_TIME} FAIL_MESSAGE=${bamboo.FAIL_MESSAGE}')

        def taskJUnitParser = TestParserTask.createJUnitParserTask()
                .description("Collect JUnit results")
                .resultDirectories("**/build/test-results/test/*.xml")

        def taskClean1 = new CleanWorkingDirectoryTask()
                .description("")
                .enabled(true)

        def taskDownLoadArtifacts = new ArtifactDownloaderTask()
                .description('Download release contents')
                .artifacts(new DownloadItem()
                        .artifact('simplejar')
                        .path("/tmp/"))

        def taskCommandEcho = new CommandTask()
                .description('echo hello')
                .executable('echo')
                .argument('hello')

        def taskError = new CommandTask()
                .description('Exit with error')
                .executable('bash')
                .argument('exit')

        def taskSleep = new CommandTask()
                .description('sleep')
                .executable('sleep')
                .argument('30')

        def job = new Job("Default Job", "JOB1")
                .tasks(taskSourceCodeCheckout, taskGradlew)
                .artifacts(artifactsArray)

        def jobForFailingPlan = new Job("Default Job", "JOB1")
                .tasks(taskSourceCodeCheckout, taskGradlewFail)
                .artifacts(artifactsArray)

        def jobSuccessfulNeverShown = new Job("Successful job in failing", "SJIF")
                .tasks(taskSourceCodeCheckout, taskGradlewSuccessfull)
                .artifacts(artifactForFailingPlan)

        def jobLong = new Job("Default Job", "JOB1")
                .tasks(taskSourceCodeCheckout, taskGradlewLong)
                .artifacts(artifactsArray)

        def jobParam = new Job("Default Job", "JOB1")
                .tasks(taskClean1, taskSourceCodeCheckout, taskGradlewParam, taskJUnitParser)
                .artifacts(artifactsArray)

        def stage = new Stage("Default Stage")
                .description("Default Stage")
                .jobs(job)

        def stageForFailingProject = new Stage("Default Stage")
                .description("Default Stage")
                .jobs(jobForFailingPlan, jobSuccessfulNeverShown)

        def stageLong = new Stage("Default Stage")
                .description("Default Stage")
                .jobs(jobLong)

        def stageParam = new Stage("Default Stage")
                .description("Default Stage")
                .jobs(jobParam)


        plan.stages(stage)
        planFail.stages(stageForFailingProject)
        planLong.stages(stageLong)
        planParams.stages(stageParam)

        def deployment = new Deployment(new PlanIdentifier(projectKey, planKey), "Deployment Project")
                .releaseNaming(new ReleaseNaming("release-3")
                        .autoIncrement(true))
                .environments(new Environment("Stage")
                        .tasks(taskClean1, taskDownLoadArtifacts, taskCommandEcho
                        )
                )

        def deploymentFail = new Deployment(new PlanIdentifier(projectKey, planKey), "Failing deployment project")
                .releaseNaming(new ReleaseNaming("release-4")
                        .autoIncrement(true))
                .environments(new Environment("Production2")
                        .tasks(taskClean1, taskDownLoadArtifacts, taskError
                        )
                )

        def deploymentLong = new Deployment(new PlanIdentifier(projectKey, planKey), "Long Deployment project")
                .releaseNaming(new ReleaseNaming("release-5")
                        .autoIncrement(true))
                .environments(new Environment("Production")
                        .tasks(taskClean1, taskDownLoadArtifacts, taskSleep
                        )
                )

        bambooServer.publish(plan)
        bambooServer.publish(planFail)
        bambooServer.publish(planLong)
        bambooServer.publish(planParams)
        bambooServer.publish(deployment)
        bambooServer.publish(deploymentFail)
        bambooServer.publish(deploymentLong)
    }

    def createPlanForRun(def projectKey, def planKey, def planName, def listArtifact = ['jar'] ){
        def bambooServer = new BambooServer(this.endpoint)
        def taskClean1 = new CleanWorkingDirectoryTask()
                .description("Clean")
                .enabled(true)
        def project = new Project().key(projectKey)

        def artifact = new Artifact("simplejar")
                .location("build/libs/")
                .copyPattern("*.jar")
                .required(true)
                .shared(true)

        def artifactXML = new Artifact("simpleXML")
                .location("build/test-results/test/")
                .copyPattern("*.xml")
                .required(true)
                .shared(true)

        Artifact[] artifactsArray = []
        if ('jar' in listArtifact){
            artifactsArray += artifact
        }
        if ('xml' in listArtifact){
            artifactsArray += artifactXML
        }

        def plan = new Plan(project, planName, planKey)
                .variables(
                        new Variable('FAIL_MESSAGE', ''),
                        new Variable('SLEEP_TIME', ''),
                        new Variable('TEST_MESSAGE', '')
                )
                .planRepositories(new GitRepository()
                        .name("Sample Gradle build2")
                        .url('https://github.com/horodchukanton/gradle-test-build.git')
                )
        bambooServer.publish(plan)


        def taskSourceCodeCheckout2 = new VcsCheckoutTask()
                .description('Checkout')
                .addCheckoutOfRepository('Sample Gradle build2')


        def taskCommand3 = new CommandTask()
                .description('Gradle Build')
                .executable('gradlew')
                .argument('build -PvariablesSource=environment')
                .environmentVariables('TEST_MESSAGE=${bamboo.TEST_MESSAGE} SLEEP_TIME=${bamboo.SLEEP_TIME} FAIL_MESSAGE=${bamboo.FAIL_MESSAGE}')

        def taskJUnitParser4 = TestParserTask.createJUnitParserTask()
                .description("Collect JUnit results")
                .resultDirectories("**/build/test-results/test/*.xml")


        def job = new Job("Default Job", "JOB1")
                .tasks(taskClean1, taskSourceCodeCheckout2, taskCommand3, taskJUnitParser4)
                .artifacts(artifactsArray)

        def stage = new Stage("Stage1")
                .description("Stage 1")
                .jobs(job)

        plan.stages(stage)

        bambooServer.publish(plan)
        return plan
    }

    def deletePlan(def project, def plan) {
        def result = doHttpRequest(DELETE, "/rest/api/latest/plan/$project-$plan")
        return result
    }

}
