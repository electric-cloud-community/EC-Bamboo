package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.E2E

class E2ESuite extends BambooHelper {

    static def pipelineName = 'Bamboo E2E Specs'
    static def pipelineFileName = 'E2EPipeline.dsl'
    static def projectName = 'Bamboo E2E Specs'
    static def config = CONFIG_NAME

    static def bambooBuildProject = 'PROJECT'
    static def bambooBuildPlan = 'PLAN'
    static def bambooDeploymentProject = 'Deployment Project'
    static def bambooDeployEnvironment = 'Stage'

    def doSetupSpec() {
		createDefaultProject()
        createConfiguration(config)

        importProject(projectName, "dsl/${pipelineFileName}", [
                projectName            : projectName,
                pipelineName           : pipelineName,
                resourceName           : getResourceName(),
                configName             : config,
                bambooBuildProject     : bambooBuildProject,
                bambooBuildPlan        : bambooBuildPlan,
                bambooDeploymentProject: bambooDeploymentProject,
                bambooDeployEnvironment: bambooDeployEnvironment,
        ])

        // Need to have one plan run before the pipeline run
        runPlan(bambooBuildProject, bambooBuildPlan, [waitForBuild: 1])
    }

    def doCleanupSpec() {
        deleteConfiguration(PLUGIN_NAME, config)
        conditionallyDeleteProject(projectName)
    }

    @E2E
    def "runPipeline"() {
        when:
        def resp = runPipeline(projectName, pipelineName, [], 300)

        def pipelineStatus = getPipelineRuntimeDetails(resp['flowRuntime']['flowRuntimeId'])
        then:
        println(getPipelineLink(projectName, pipelineName, resp['flowRuntime']['flowRuntimeId']))

        assert pipelineStatus['flowRuntime']['status'][0] == 'success'
    }
}
