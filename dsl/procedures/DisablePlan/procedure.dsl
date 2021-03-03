// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'DisablePlan', description: '''This procedure disables build plan.''', {

    step 'DisablePlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/DisablePlan/steps/DisablePlan.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: 7c5d41590c050d56cc13009102416f91 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}