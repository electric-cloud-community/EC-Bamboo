// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'TriggerDeployment', description: '''This procedure runs Bamboo deployment plan.''', {

    step 'TriggerDeployment', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/TriggerDeployment/steps/TriggerDeployment.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }

    formalOutputParameter 'deploymentResultKey',
        description: '''Key of the deployment result.'''

    formalOutputParameter 'deploymentResultUrl',
        description: '''URL to the deployment result report.'''
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: 37c5723a1cbe681ee324796831379adf ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}