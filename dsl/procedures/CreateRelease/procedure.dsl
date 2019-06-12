// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'CreateRelease', description: 'This procedure creates new release (version) from the build plan result.', {

    step 'CreateRelease', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRelease/steps/CreateRelease.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'version',
        description: 'Name of the created version.'
// === procedure_autogen ends, checksum: 6de49319fe5e15199648c73a9d9ef796 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}