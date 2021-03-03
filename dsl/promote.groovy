
// DO NOT EDIT THIS BLOCK BELOW=== promote_autogen starts ===
import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.util.BasePlugin

//noinspection GroovyUnusedAssignment
@BaseScript BasePlugin baseScript

// Variables available for use in DSL code
def pluginName = args.pluginName
def upgradeAction = args.upgradeAction
def otherPluginName = args.otherPluginName

def pluginKey = getProject("/plugins/$pluginName/project").pluginKey
def pluginDir = getProperty("/projects/$pluginName/pluginDir").value

//List of procedure steps to which the plugin configuration credentials need to be attached
def stepsWithAttachedCredentials = [
    [procedureName: "GetAllPlans", stepName: "GetAllPlans"],
    [procedureName: "GetPlanDetails", stepName: "GetPlanDetails"],
    [procedureName: "GetPlanRuns", stepName: "GetPlanRuns"],
    [procedureName: "RunPlan", stepName: "RunPlan"],
    [procedureName: "EnablePlan", stepName: "EnablePlan"],
    [procedureName: "DisablePlan", stepName: "DisablePlan"],
    [procedureName: "TriggerDeployment", stepName: "TriggerDeployment"],
    [procedureName: "GetDeploymentProjectsForPlan", stepName: "GetDeploymentProjectsForPlan"],
    [procedureName: "CreateRelease", stepName: "CreateRelease"],
    [procedureName: "CollectReportingData", stepName: "CollectReportingData"],
    [procedureName: "ValidateCRDParams", stepName: "ValidateCRDParams"],

]

project pluginName, {
    property 'ec_keepFilesExtensions', value: 'true'
    property 'ec_formXmlCompliant', value: 'true'
    loadPluginProperties(pluginDir, pluginName)
    loadProcedures(pluginDir, pluginKey, pluginName, stepsWithAttachedCredentials)

    // Plugin configuration metadata
    property 'ec_config', {
        configLocation = 'ec_plugin_cfgs'
        form = '$[' + "/projects/$pluginName/procedures/CreateConfiguration/ec_parameterForm]"
        property 'fields', {
            property 'desc', {
                property 'label', value: 'Description'
                property 'order', value: '1'
            }
        }
    }

    // Properties
    property 'ec_dsl_libraries_path', {

value = 'libs'

}

    property 'ec_devops_insight', {

expandable = false

property 'build', {

expandable = false

property 'source', {

value = 'Bamboo'

}

property 'operations', {

expandable = false

property 'createDOISDataSource', {

expandable = false

property 'procedureName', {

value = 'ValidateCRDParams'

}

}

property 'modifyDOISDataSource', {

expandable = false

property 'procedureName', {

value = 'ValidateCRDParams'

}

}

}

}

}

    }

def retainedProperties = []

upgrade(upgradeAction, pluginName, otherPluginName, stepsWithAttachedCredentials, 'ec_plugin_cfgs', retainedProperties)
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== promote_autogen ends, checksum: c38e7c65bbc12c646446282f6601d31d ===
// Do not edit the code above this line

project pluginName, {
    // You may add your own DSL instructions below this line, like
    // property 'myprop', {
    //     value: 'value'
    // }
}