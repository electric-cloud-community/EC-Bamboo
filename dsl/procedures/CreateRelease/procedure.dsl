// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'CreateRelease', description: 'This procedure creates new release from the build plan result.', {

    step 'CreateRelease', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRelease/steps/CreateRelease.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'release',
        description: 'Name of the created release.'
// === procedure_autogen ends, checksum: 7eac76e2b57ad83a6270451fe7084756 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}