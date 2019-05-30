package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Unroll

class EnablePlanSuite extends BambooHelper {
    static procedureName = 'EnablePlan'
    static projectName = "EC-Specs $procedureName"

    static def procedureParams = [
            config    : '',
            projectKey: '',
            planKey   : '',
    ]

    static def bambooProjects = [
            valid     : 'PROJECT',
            unexisting: "__UNEXISTING__",
            empty     : ''
    ]

    static def bambooPlans = [
            valid       : 'PLAN',
            failing     : 'FAIL',
            timeout     : 'LONG',
            parametrized: 'PARAMS',
            unexisting  : "__UNEXISTING__",
            empty       : ''
    ]

    String config = CONFIG_NAME

    @Shared
    String projectKey, planKey

    def doSetupSpec() {
        createConfiguration(CONFIG_NAME)

        // Import procedure project
        importProject(projectName, 'dsl/procedure.dsl', [
                projectName  : projectName,
                procedureName: procedureName,
                resourceName : getResourceName(),
                params       : procedureParams,
        ])
    }

    def doCleanupSpec() {
        deleteConfiguration(PLUGIN_NAME, config)
        conditionallyDeleteProject(projectName)
    }

    @Sanity
    @Unroll
    def "#caseId. EnablePlan - Sanity - positive"() {
        given:
        config = CONFIG_NAME
        def procedureParams = [
                config    : config,
                projectKey: projectKey,
                planKey   : planKey,
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == 'success'

        // Check logs
        assert result.logs =~ /Plan '$projectKey-$planKey' was enabled/

        where:
        caseId       | config      | projectKey              | planKey
        'CHANGEME_1' | CONFIG_NAME | bambooProjects['valid'] | bambooPlans['valid']
    }

    @Unroll
    def "#caseId. EnablePlan - Sanity - negative"() {
        given:
        config = CONFIG_NAME
        def procedureParams = [
                config    : config,
                projectKey: projectKey,
                planKey   : planKey,
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == expectedOutcome

        // Check logs
        getProcedureJobStepSummary(procedureName, result.jobId)

        where:
        caseId       | config      | projectKey              | planKey                   | expectedOutcome | expectedSummary
        'CHANGEME_2' | CONFIG_NAME | bambooProjects['valid'] | bambooPlans['unexisting'] | 'error'         | 'not found'
    }


}