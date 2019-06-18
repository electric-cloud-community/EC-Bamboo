def projName = args.projectName ?: 'Bamboo E2E pipeline'
def pipeName = args.pipelineName ?: 'Bamboo E2E pipeline'
def resName = args.resourceName ?: 'local'
def configName = args.configName ?: 'config'
def bambooProject = args.bambooBuildProject ?: 'PROJECT'
def bambooPlan = args.bambooBuildPlan ?: 'PLAN'
def bambooDeploymentProj = args.bambooDeploymentProject ?: 'Deployment Project'
def bambooDeploymentEnvironment = args.bambooDeployEnvironment ?: 'Stage'

project projName, {
    description = 'Default project created during installation.'
    resourceName = resName

    pipeline pipeName, {
        description = ''
        disableMultipleActiveRuns = '0'
        disableRestart = '0'
        enabled = '1'
        overrideWorkspace = '0'
        skipStageMode = 'ENABLED'

        stage 'PlanDetails', {
            description = ''
            colorCode = '#00adee'
            pipelineName = pipeName
            resourceName = resName
            waitForPlannedStartDate = '0'

            task 'GetAllPlans', {
                description = ''
                actualParameter = [
                        'config'             : configName,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'json',
                        'resultPropertySheet': '/myJob/plans',
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetAllPlans'
                taskType = 'PLUGIN'
            }

            task 'GetPlanDetails', {
                description = ''
                actualParameter = [
                        'config'             : configName,
                        'planKey'            : bambooPlan,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'json',
                        'resultPropertySheet': '/myJob/plan',
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetPlanDetails'
                taskType = 'PLUGIN'
            }

            task 'GetPlanRuns', {
                description = ''
                actualParameter = [
                        'buildState'         : 'Successful',
                        'config'             : configName,
                        'planKey'            : bambooPlan,
                        'maxResults'         : '1',
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'propertySheet',
                        'resultPropertySheet': '/myJob/resultKeys',
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetPlanRuns'
                taskType = 'PLUGIN'
            }
        }

        stage 'Enable/Disable Plan', {
            description = ''
            colorCode = '#d62728'
            pipelineName = pipeName
            resourceName = resName
            waitForPlannedStartDate = '0'
            
            task 'DisablePlan', {
                description = ''
                actualParameter = [
                        'config'    : configName,
                        'planKey'   : bambooPlan,
                        'projectKey': bambooProject,
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'DisablePlan'
                taskType = 'PLUGIN'
            }

            task 'EnablePlan', {
                description = ''
                actualParameter = [
                        'config'    : configName,
                        'planKey'   : bambooPlan,
                        'projectKey': bambooProject,
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'EnablePlan'
                taskType = 'PLUGIN'
            }
        }

        stage 'Build Plan', {
            description = ''
            colorCode = '#ff7f0e'
            pipelineName = pipeName
            resourceName = resName
            waitForPlannedStartDate = '0'

            task 'RunPlan', {
                description = ''
                actualParameter = [
                        'config'             : configName,
                        'planKey'            : bambooPlan,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'propertySheet',
                        'resultPropertySheet': '/myJob/runResult',
                        'waitForBuild'       : '1',
                        'waitTimeout'        : '300',
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'RunPlan'
                taskType = 'PLUGIN'
            }

            task 'GetPlanRuns', {
                description = ''
                actualParameter = [
                        'buildState'         : 'Successful',
                        'config'             : configName,
                        'maxResults'         : '1',
                        'planKey'            : bambooPlan,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'propertySheet',
                        'resultPropertySheet': '/myJob/resultKeys',
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetPlanRuns'
                taskType = 'PLUGIN'
            }

            task 'CheckLastSuccessfulChanged', {
                description = ''

                actualParameter = [
                        'commandToRun': '''my $prev = \'$[/myPipelineRuntime/stages/PlanDetails/tasks/GetPlanRuns/job/outputParameters/latestResultKey]\';
my $new = \'$[/myStageRuntime/tasks/GetPlanRuns/job/outputParameters/latestResultKey]\';
print "Checking $prev eq $new\\n";
if($prev eq $new){
  die "Something went wrong. Build number was not changed";
}
exit 0;''',
                        'shellToUse'  : 'ec-perl',
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Core'
                subprocedure = 'RunCommand'
                taskType = 'COMMAND'
            }
        }

        stage 'Deployment', {
            description = ''
            colorCode = '#2ca02c'
            pipelineName = pipeName
            resourceName = resName
            waitForPlannedStartDate = '0'

            task 'GetDeploymentProjects', {
                description = ''
                actualParameter = [
                        'config'             : configName,
                        'planKey'            : bambooPlan,
                        'projectKey'         : bambooProject,
                        'resultFormat'       : 'none',
                        'resultPropertySheet': '/myJob/deploymentProjects',
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'GetDeploymentProjectsForPlan'
                taskType = 'PLUGIN'
            }

            task 'CreateVersion', {
                description = ''
                actualParameter = [
                        'config'               : configName,
                        'deploymentProjectName': bambooDeploymentProj,
                        'planBuildKey'         : '$[/myPipelineRuntime/stages["Build Plan"]/tasks/GetPlanRuns/job/outputParameters/latestResultKey]',
                        'requestVersionName'   : '1',
                        'resultFormat'         : 'json',
                        'resultPropertySheet'  : '/myJob/version',
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'CreateRelease'
                taskType = 'PLUGIN'
            }

            task 'TriggerDeployment', {
                description = ''
                actualParameter = [
                        'config'                   : configName,
                        'deploymentEnvironmentName': bambooDeploymentEnvironment,
                        'deploymentProjectName'    : bambooDeploymentProj,
                        'deploymentVersionName'    : '$[/myStageRuntime/tasks/CreateVersion/job/outputParameters/version]',
                        'resultFormat'             : 'json',
                        'resultPropertySheet'      : '/myJob/deploymentResult',
                        'waitForDeployment'        : '1',
                        'waitTimeout'              : '300',
                ]
                alwaysRun = '0'
                enabled = '1'
                errorHandling = 'stopOnError'
                insertRollingDeployManualStep = '0'
                resourceName = resName
                subpluginKey = 'EC-Bamboo'
                subprocedure = 'TriggerDeployment'
                taskType = 'PLUGIN'
            }
        }

        // Custom properties
        property 'ec_counters', {
            // Custom properties
            pipelineCounter = '1'
        }

    }
}
