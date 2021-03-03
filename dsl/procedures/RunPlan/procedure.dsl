// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'RunPlan', description: '''This procedure runs Bamboo build plan.''', {

    step 'RunPlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/RunPlan/steps/RunPlan.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }

    formalOutputParameter 'buildResultKey',
        description: '''Composite key for the Run Result.'''

    formalOutputParameter 'buildUrl',
        description: '''Link to the result page.'''
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: 40b1c462d42e1725cd5eac81b7f727a3 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}