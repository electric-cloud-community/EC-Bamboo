// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'ValidateCRDParams', description: '''Service procedure to check parameter values passed for DOISDataSource creation.''', {

    step 'ValidateCRDParams', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/ValidateCRDParams/steps/ValidateCRDParams.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: dec5846cf83f265d4e69ff6355d632e2 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
    property 'standardStepPicker', 'false'
}