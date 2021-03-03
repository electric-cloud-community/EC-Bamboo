// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'GetPlanRuns', description: '''This procedure returns information about runs on Bamboo build plan.''', {

    step 'GetPlanRuns', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetPlanRuns/steps/GetPlanRuns.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }

    formalOutputParameter 'resultKeys',
        description: '''List of comma-separated plan build result keys.'''

    formalOutputParameter 'latestResultKey',
        description: '''Key for the latest build run.'''
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: bcaa0a9f8539696aea213fb52db32f55 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}