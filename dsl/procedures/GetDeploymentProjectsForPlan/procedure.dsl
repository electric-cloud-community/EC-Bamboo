// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'GetDeploymentProjectsForPlan', description: '''This procedure returns information about deployment projects linked to this build plan.''', {

    step 'GetDeploymentProjectsForPlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetDeploymentProjectsForPlan/steps/GetDeploymentProjectsForPlan.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }

    formalOutputParameter 'deploymentProjectKeys',
        description: '''List of comma-separated deployment project keys for the plan'''
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: c405802663996fc70ee5e467cbf8ac11 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}