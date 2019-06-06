// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'CreateVersion', description: 'Creates new version (release) from the successful build result', {

    step 'CreateVersion', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateVersion/steps/CreateVersion.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'version',
        description: 'Name of the created version.'
// === procedure_autogen ends, checksum: c65e62ee62d76c15abd955e107083e96 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}