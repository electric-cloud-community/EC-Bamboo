package com.electriccloud.plugin.spec

import com.atlassian.bamboo.specs.api.BambooSpec
import com.atlassian.bamboo.specs.api.builders.plan.Job
import com.atlassian.bamboo.specs.api.builders.plan.Plan
import com.atlassian.bamboo.specs.api.builders.plan.Stage
import com.atlassian.bamboo.specs.api.builders.project.Project
import com.atlassian.bamboo.specs.builders.task.ScriptTask
import com.atlassian.bamboo.specs.util.BambooServer
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

    static def commanderAddress = System.getProperty("COMMANDER_SERVER")

    def protocol
    def host
    def port
    def urlPath
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

        this.protocol = protocol
        this.host     = host
        this.port     = port
        this.urlPath  = urlPath
        this.userName = userName
        this.password = password
        this.http = new HTTPBuilder(endpoint)
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
        def bambooServer = new BambooServer("http://$commanderAddress:8085")
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

    def deletePlan(def project, def plan) {
        def result = doHttpRequest(DELETE, "/rest/api/latest/plan/$project-$plan")
        return result
    }

}
