// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'CreateRelease', description: 'Creates new version (release) from the successful build result.', {

    step 'CreateRelease', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRelease/steps/CreateRelease.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'version',
        description: 'Name of the created version.'
// === procedure_autogen ends, checksum: abbf3b19e114e70eddcb9485836ca816 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}