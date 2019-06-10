def deploymentProj = 'Deployment Project'
def configName = args.configName ?: 'config'
def bambooProject = 'PROJECT'
def bambooPlan = 'PLAN'

project 'Default', {
    description = 'Default project created during installation.'
    resourceName = null
    workspaceName = null

    pipeline 'Bamboo', {
        description = ''
        disableMultipleActiveRuns = '0'
        disableRestart = '0'
        enabled = '1'
        overrideWorkspace = '0'
        pipelineRunNameTemplate = null
        releaseName = null
        skipStageMode = 'ENABLED'
        templatePipelineName = null
        templatePipelineProjectName = null
        type = null
        workspaceName = null

        stage 'PlanDetails', {
            description = ''
            colorCode = '#00adee'
            completionType = 'auto'
            condition = null
            duration = null
            parallelToPrevious = null
            pipelineName = 'Bamboo'
            plannedEndDate = null
            plannedStartDate = null
            precondition = null
            resourceName = null
            waitForPlannedStartDate = '0'

            gate 'PRE', {
                condition = null
                precondition = null
            }

            gate 'POST', {
                condition = null
                precondition = null
            }

            task 'GetAllPlans', {
                description = ''
                actionLabelText = null
                actualParameter = [
                        'config'             : configName,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'json',
                        'resultPropertySheet': '/myJob/plans',
                ]
                advancedMode = '0'
                afterLastRetry = null
                allowOutOfOrderRun = '0'
                allowSkip = null
                alwaysRun = '0'
                condition = null
                deployerExpression = null
                deployerRunType = null
                disableFailure = null
                duration = null
                emailConfigName = null
                enabled = '1'
                environmentName = null
                environmentProjectName = null
                environmentTemplateName = null
                environmentTemplateProjectName = null
                errorHandling = 'stopOnError'
                gateCondition = null
                gateType = null
                groupName = null
                groupRunType = null
                insertRollingDeployManualStep = '0'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                parallelToPrevious = null
                plannedEndDate = null
                plannedStartDate = null
                precondition = null
                requiredApprovalsCount = null
                resourceName = ''
                retryCount = null
                retryInterval = null
                retryType = null
                rollingDeployEnabled = null
                rollingDeployManualStepCondition = null
                skippable = '0'
                snapshotName = null
                stageSummaryParameters = null
                startingStage = null
                subErrorHandling = null
                subapplication = null
                subpipeline = null
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetAllPlans'
                subprocess = null
                subproject = null
                subrelease = null
                subreleasePipeline = null
                subreleasePipelineProject = null
                subreleaseSuffix = null
                subservice = null
                subworkflowDefinition = null
                subworkflowStartingState = null
                taskProcessType = null
                taskType = 'PLUGIN'
                triggerType = null
                useApproverAcl = '0'
                waitForPlannedStartDate = '0'
            }

            task 'GetPlanDetails', {
                description = ''
                actionLabelText = null
                actualParameter = [
                        'config'             : configName,
                        'planKey'            : bambooPlan,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'json',
                        'resultPropertySheet': '/myJob/plan',
                ]
                advancedMode = '0'
                afterLastRetry = null
                allowOutOfOrderRun = '0'
                allowSkip = null
                alwaysRun = '0'
                condition = null
                deployerExpression = null
                deployerRunType = null
                disableFailure = null
                duration = null
                emailConfigName = null
                enabled = '1'
                environmentName = null
                environmentProjectName = null
                environmentTemplateName = null
                environmentTemplateProjectName = null
                errorHandling = 'stopOnError'
                gateCondition = null
                gateType = null
                groupName = null
                groupRunType = null
                insertRollingDeployManualStep = '0'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                parallelToPrevious = null
                plannedEndDate = null
                plannedStartDate = null
                precondition = null
                requiredApprovalsCount = null
                resourceName = ''
                retryCount = null
                retryInterval = null
                retryType = null
                rollingDeployEnabled = null
                rollingDeployManualStepCondition = null
                skippable = '0'
                snapshotName = null
                stageSummaryParameters = null
                startingStage = null
                subErrorHandling = null
                subapplication = null
                subpipeline = null
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetPlanDetails'
                subprocess = null
                subproject = null
                subrelease = null
                subreleasePipeline = null
                subreleasePipelineProject = null
                subreleaseSuffix = null
                subservice = null
                subworkflowDefinition = null
                subworkflowStartingState = null
                taskProcessType = null
                taskType = 'PLUGIN'
                triggerType = null
                useApproverAcl = '0'
                waitForPlannedStartDate = '0'
            }

            task 'GetPlanRuns', {
                description = ''
                actionLabelText = null
                actualParameter = [
                        'buildState'         : 'Successful',
                        'config'             : configName,
                        'planKey'            : bambooPlan,
                        'maxResults'         : '1',
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'propertySheet',
                        'resultPropertySheet': '/myJob/resultKeys',
                ]
                advancedMode = '0'
                afterLastRetry = null
                allowOutOfOrderRun = '0'
                allowSkip = null
                alwaysRun = '0'
                condition = null
                deployerExpression = null
                deployerRunType = null
                disableFailure = null
                duration = null
                emailConfigName = null
                enabled = '1'
                environmentName = null
                environmentProjectName = null
                environmentTemplateName = null
                environmentTemplateProjectName = null
                errorHandling = 'stopOnError'
                gateCondition = null
                gateType = null
                groupName = null
                groupRunType = null
                insertRollingDeployManualStep = '0'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                parallelToPrevious = null
                plannedEndDate = null
                plannedStartDate = null
                precondition = null
                requiredApprovalsCount = null
                resourceName = ''
                retryCount = null
                retryInterval = null
                retryType = null
                rollingDeployEnabled = null
                rollingDeployManualStepCondition = null
                skippable = '0'
                snapshotName = null
                stageSummaryParameters = null
                startingStage = null
                subErrorHandling = null
                subapplication = null
                subpipeline = null
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetPlanRuns'
                subprocess = null
                subproject = null
                subrelease = null
                subreleasePipeline = null
                subreleasePipelineProject = null
                subreleaseSuffix = null
                subservice = null
                subworkflowDefinition = null
                subworkflowStartingState = null
                taskProcessType = null
                taskType = 'PLUGIN'
                triggerType = null
                useApproverAcl = '0'
                waitForPlannedStartDate = '0'
            }
        }

        stage 'Build Plan', {
            description = ''
            colorCode = '#ff7f0e'
            completionType = 'auto'
            condition = null
            duration = null
            parallelToPrevious = null
            pipelineName = 'Bamboo'
            plannedEndDate = null
            plannedStartDate = null
            precondition = null
            resourceName = null
            waitForPlannedStartDate = '0'

            gate 'PRE', {
                condition = null
                precondition = null
            }

            gate 'POST', {
                condition = null
                precondition = null
            }

            task 'RunPlan', {
                description = ''
                actionLabelText = null
                actualParameter = [
                        'config'             : configName,
                        'planKey'            : bambooPlan,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'propertySheet',
                        'resultPropertySheet': '/myJob/runResult',
                        'waitForBuild'       : '1',
                        'waitTimeout'        : '300',
                ]
                advancedMode = '0'
                afterLastRetry = null
                allowOutOfOrderRun = '0'
                allowSkip = null
                alwaysRun = '0'
                condition = null
                deployerExpression = null
                deployerRunType = null
                disableFailure = null
                duration = null
                emailConfigName = null
                enabled = '1'
                environmentName = null
                environmentProjectName = null
                environmentTemplateName = null
                environmentTemplateProjectName = null
                errorHandling = 'stopOnError'
                gateCondition = null
                gateType = null
                groupName = null
                groupRunType = null
                insertRollingDeployManualStep = '0'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                parallelToPrevious = null
                plannedEndDate = null
                plannedStartDate = null
                precondition = null
                requiredApprovalsCount = null
                resourceName = ''
                retryCount = null
                retryInterval = null
                retryType = null
                rollingDeployEnabled = null
                rollingDeployManualStepCondition = null
                skippable = '0'
                snapshotName = null
                stageSummaryParameters = null
                startingStage = null
                subErrorHandling = null
                subapplication = null
                subpipeline = null
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'RunPlan'
                subprocess = null
                subproject = null
                subrelease = null
                subreleasePipeline = null
                subreleasePipelineProject = null
                subreleaseSuffix = null
                subservice = null
                subworkflowDefinition = null
                subworkflowStartingState = null
                taskProcessType = null
                taskType = 'PLUGIN'
                triggerType = null
                useApproverAcl = '0'
                waitForPlannedStartDate = '0'
            }

            task 'GetPlanRuns', {
                description = ''
                actionLabelText = null
                actualParameter = [
                        'buildState'         : 'Successful',
                        'config'             : configName,
                        'maxResults'         : '1',
                        'planKey'            : bambooPlan,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'propertySheet',
                        'resultPropertySheet': '/myJob/resultKeys',
                ]
                advancedMode = '0'
                afterLastRetry = null
                allowOutOfOrderRun = '0'
                allowSkip = null
                alwaysRun = '0'
                condition = null
                deployerExpression = null
                deployerRunType = null
                disableFailure = null
                duration = null
                emailConfigName = null
                enabled = '1'
                environmentName = null
                environmentProjectName = null
                environmentTemplateName = null
                environmentTemplateProjectName = null
                errorHandling = 'stopOnError'
                gateCondition = null
                gateType = null
                groupName = null
                groupRunType = null
                insertRollingDeployManualStep = '0'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                parallelToPrevious = null
                plannedEndDate = null
                plannedStartDate = null
                precondition = null
                requiredApprovalsCount = null
                resourceName = ''
                retryCount = null
                retryInterval = null
                retryType = null
                rollingDeployEnabled = null
                rollingDeployManualStepCondition = null
                skippable = '0'
                snapshotName = null
                stageSummaryParameters = null
                startingStage = null
                subErrorHandling = null
                subapplication = null
                subpipeline = null
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetPlanRuns'
                subprocess = null
                subproject = null
                subrelease = null
                subreleasePipeline = null
                subreleasePipelineProject = null
                subreleaseSuffix = null
                subservice = null
                subworkflowDefinition = null
                subworkflowStartingState = null
                taskProcessType = null
                taskType = 'PLUGIN'
                triggerType = null
                useApproverAcl = '0'
                waitForPlannedStartDate = '0'
            }

            task 'CheckLastSuccessfulChanged', {
                description = ''
                actionLabelText = null
                actualParameter = [
                        'commandToRun': '''my $prev = \'$[/myPipelineRuntime/stages/PlanDetails/tasks/GetPlanRuns/job/outputParameters/latestResultKey]\';

my $new = \'$[/myStageRuntime/tasks/GetPlanRuns/job/outputParameters/latestResultKey]\';

print "Checking $prev eq $new\\n";

if($prev eq $new){
  die "Something went wrong";
}

exit 0;''',
                        'shellToUse'  : 'ec-perl',
                ]
                advancedMode = '0'
                afterLastRetry = null
                allowOutOfOrderRun = '0'
                allowSkip = null
                alwaysRun = '0'
                condition = null
                deployerExpression = null
                deployerRunType = null
                disableFailure = null
                duration = null
                emailConfigName = null
                enabled = '1'
                environmentName = null
                environmentProjectName = null
                environmentTemplateName = null
                environmentTemplateProjectName = null
                errorHandling = 'stopOnError'
                gateCondition = null
                gateType = null
                groupName = null
                groupRunType = null
                insertRollingDeployManualStep = '0'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                parallelToPrevious = null
                plannedEndDate = null
                plannedStartDate = null
                precondition = null
                requiredApprovalsCount = null
                resourceName = ''
                retryCount = null
                retryInterval = null
                retryType = null
                rollingDeployEnabled = null
                rollingDeployManualStepCondition = null
                skippable = '0'
                snapshotName = null
                stageSummaryParameters = null
                startingStage = null
                subErrorHandling = null
                subapplication = null
                subpipeline = null
                subpluginKey = 'EC-Core'
                subprocedure = 'RunCommand'
                subprocess = null
                subproject = null
                subrelease = null
                subreleasePipeline = null
                subreleasePipelineProject = null
                subreleaseSuffix = null
                subservice = null
                subworkflowDefinition = null
                subworkflowStartingState = null
                taskProcessType = null
                taskType = 'COMMAND'
                triggerType = null
                useApproverAcl = '0'
                waitForPlannedStartDate = '0'
            }
        }

        stage 'Deployment', {
            description = ''
            colorCode = '#2ca02c'
            completionType = 'auto'
            condition = null
            duration = null
            parallelToPrevious = null
            pipelineName = 'Bamboo'
            plannedEndDate = null
            plannedStartDate = null
            precondition = null
            resourceName = null
            waitForPlannedStartDate = '0'

            gate 'PRE', {
                condition = null
                precondition = null
            }

            gate 'POST', {
                condition = null
                precondition = null
            }

            task 'GetDeploymentProjects', {
                description = ''
                actionLabelText = null
                actualParameter = [
                        'config'             : configName,
                        'planKey'            : bambooPlan,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'none',
                        'resultPropertySheet': '/myJob/deploymentProjectKeys',
                ]
                advancedMode = '0'
                afterLastRetry = null
                allowOutOfOrderRun = '0'
                allowSkip = null
                alwaysRun = '0'
                condition = null
                deployerExpression = null
                deployerRunType = null
                disableFailure = null
                duration = null
                emailConfigName = null
                enabled = '1'
                environmentName = null
                environmentProjectName = null
                environmentTemplateName = null
                environmentTemplateProjectName = null
                errorHandling = 'stopOnError'
                gateCondition = null
                gateType = null
                groupName = null
                groupRunType = null
                insertRollingDeployManualStep = '0'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                parallelToPrevious = null
                plannedEndDate = null
                plannedStartDate = null
                precondition = null
                requiredApprovalsCount = null
                resourceName = ''
                retryCount = null
                retryInterval = null
                retryType = null
                rollingDeployEnabled = null
                rollingDeployManualStepCondition = null
                skippable = '0'
                snapshotName = null
                stageSummaryParameters = null
                startingStage = null
                subErrorHandling = null
                subapplication = null
                subpipeline = null
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetDeploymentProjectsForPlan'
                subprocess = null
                subproject = null
                subrelease = null
                subreleasePipeline = null
                subreleasePipelineProject = null
                subreleaseSuffix = null
                subservice = null
                subworkflowDefinition = null
                subworkflowStartingState = null
                taskProcessType = null
                taskType = 'PLUGIN'
                triggerType = null
                useApproverAcl = '0'
                waitForPlannedStartDate = '0'
            }

            task 'CreateVersion', {
                description = ''
                actionLabelText = null
                actualParameter = [
                        'config'               : configName,
                        'deploymentProjectName': deploymentProj,
                        'planBuildKey'         : '$[/myPipelineRuntime/stages["Build Plan"]/tasks/GetPlanRuns/job/outputParameters/latestResultKey]',
                        'requestVersionName'   : '1',
                        'resultFormat'         : 'json',
                        'resultPropertySheet'  : '/myJob/version',
                ]
                advancedMode = '0'
                afterLastRetry = null
                allowOutOfOrderRun = '0'
                allowSkip = null
                alwaysRun = '0'
                condition = null
                deployerExpression = null
                deployerRunType = null
                disableFailure = null
                duration = null
                emailConfigName = null
                enabled = '1'
                environmentName = null
                environmentProjectName = null
                environmentTemplateName = null
                environmentTemplateProjectName = null
                errorHandling = 'stopOnError'
                gateCondition = null
                gateType = null
                groupName = null
                groupRunType = null
                insertRollingDeployManualStep = '0'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                parallelToPrevious = null
                plannedEndDate = null
                plannedStartDate = null
                precondition = null
                requiredApprovalsCount = null
                resourceName = ''
                retryCount = null
                retryInterval = null
                retryType = null
                rollingDeployEnabled = null
                rollingDeployManualStepCondition = null
                skippable = '0'
                snapshotName = null
                stageSummaryParameters = null
                startingStage = null
                subErrorHandling = null
                subapplication = null
                subpipeline = null
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'CreateRelease'
                subprocess = null
                subproject = null
                subrelease = null
                subreleasePipeline = null
                subreleasePipelineProject = null
                subreleaseSuffix = null
                subservice = null
                subworkflowDefinition = null
                subworkflowStartingState = null
                taskProcessType = null
                taskType = 'PLUGIN'
                triggerType = null
                useApproverAcl = '0'
                waitForPlannedStartDate = '0'
            }

            task 'TriggerDeployment', {
                description = ''
                actionLabelText = null
                actualParameter = [
                        'config'                   : configName,
                        'deploymentEnvironmentName': 'Stage',
                        'deploymentProjectName'    : deploymentProj,
                        'deploymentVersionName'    : '$[/myStageRuntime/tasks/CreateVersion/job/outputParameters/version]',
                        'resultFormat'             : 'json',
                        'resultPropertySheet'      : '/myJob/deploymentResult',
                        'waitForDeployment'        : '1',
                        'waitTimeout'              : '300',
                ]
                advancedMode = '0'
                afterLastRetry = null
                allowOutOfOrderRun = '0'
                allowSkip = null
                alwaysRun = '0'
                condition = null
                deployerExpression = null
                deployerRunType = null
                disableFailure = null
                duration = null
                emailConfigName = null
                enabled = '1'
                environmentName = null
                environmentProjectName = null
                environmentTemplateName = null
                environmentTemplateProjectName = null
                errorHandling = 'stopOnError'
                gateCondition = null
                gateType = null
                groupName = null
                groupRunType = null
                insertRollingDeployManualStep = '0'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                parallelToPrevious = null
                plannedEndDate = null
                plannedStartDate = null
                precondition = null
                requiredApprovalsCount = null
                resourceName = ''
                retryCount = null
                retryInterval = null
                retryType = null
                rollingDeployEnabled = null
                rollingDeployManualStepCondition = null
                skippable = '0'
                snapshotName = null
                stageSummaryParameters = null
                startingStage = null
                subErrorHandling = null
                subapplication = null
                subpipeline = null
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'TriggerDeployment'
                subprocess = null
                subproject = null
                subrelease = null
                subreleasePipeline = null
                subreleasePipelineProject = null
                subreleaseSuffix = null
                subservice = null
                subworkflowDefinition = null
                subworkflowStartingState = null
                taskProcessType = null
                taskType = 'PLUGIN'
                triggerType = null
                useApproverAcl = '0'
                waitForPlannedStartDate = '0'
            }
        }

        // Custom properties

        property 'ec_counters', {

            // Custom properties
            pipelineCounter = '6'
        }
    }
}
