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
// === procedure_autogen ends, checksum: c2974c8ff8008c956e79ffd5dda18a02 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}