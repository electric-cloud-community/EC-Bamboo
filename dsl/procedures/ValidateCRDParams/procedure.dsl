// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'ValidateCRDParams', description: 'Validates collect reporting data params.', {

    step 'ValidateCRDParams', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/ValidateCRDParams/steps/ValidateCRDParams.pl").text
        shell = 'ec-perl'

    }
// === procedure_autogen ends, checksum: c99eba25759b0ac6354546793d6d4d05 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
    property 'standardStepPicker', 'false'
}