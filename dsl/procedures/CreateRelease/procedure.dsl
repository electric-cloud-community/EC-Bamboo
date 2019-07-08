// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'CreateRelease', description: 'This procedure creates new release (version) from the build plan result.', {

    step 'CreateRelease', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRelease/steps/CreateRelease.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'release',
        description: 'Name of the created release.'
// === procedure_autogen ends, checksum: 56413f935de12e8b3ccdcb8a7bbfb90c ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}