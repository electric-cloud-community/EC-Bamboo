// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'GetPlanDetails', description: '''This procedure prints Bamboo build plan details.''', {

    step 'GetPlanDetails', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetPlanDetails/steps/GetPlanDetails.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: a4e76c42952df1e8bd416eae8430a302 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}