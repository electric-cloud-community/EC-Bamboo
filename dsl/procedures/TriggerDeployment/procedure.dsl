// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'TriggerDeployment', description: 'Runs deployment plan.', {

    step 'TriggerDeployment', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/TriggerDeployment/steps/TriggerDeployment.pl").text
        shell = 'ec-perl'

    }

    formalOutputParameter 'deploymentKeys',
        description: 'List of comma-separated deployment result keys.'

    formalOutputParameter 'resultURL',
        description: 'URL to the deployment result report.'
// === procedure_autogen ends, checksum: 53593fedd214edc8e83eaf951df733f1 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}