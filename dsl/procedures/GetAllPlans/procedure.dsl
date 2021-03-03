// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'GetAllPlans', description: '''Returns all plans that are available for current user.''', {

    step 'GetAllPlans', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetAllPlans/steps/GetAllPlans.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }

    formalOutputParameter 'planKeys',
        description: '''List of comma-separated plan keys'''
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: 5c9473b6e8da339e8389ff301034108e ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}