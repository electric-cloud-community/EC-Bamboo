// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'ValidateCRDParams', description: 'Service procedure to check parameter values passed for DOISDataSource creation.', {

    step 'ValidateCRDParams', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/ValidateCRDParams/steps/ValidateCRDParams.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: 1386f5f1a89484d3343906de65c8419b ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
    property 'standardStepPicker', 'false'
}