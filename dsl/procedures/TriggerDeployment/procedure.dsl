// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'TriggerDeployment', description: 'Runs deployment plan.', {

    step 'TriggerDeployment', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/TriggerDeployment/steps/TriggerDeployment.pl").text
        shell = 'ec-perl'

    }

    formalOutputParameter 'deploymentResultKey',
        description: 'Key of the deployment result.'

    formalOutputParameter 'deploymentResultUrl',
        description: 'URL to the deployment result report.'
// === procedure_autogen ends, checksum: be97d85d7dbe38bf9f92c5bdacf34eea ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}