// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'ValidateCRDParams', description: 'Validates collect reporting data params.', {

    step 'ValidateCRDParams', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/ValidateCRDParams/steps/ValidateCRDParams.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: 818c26cadb4f6fdb9d6d0685da4ac493 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
    property 'standardStepPicker', 'false'
}