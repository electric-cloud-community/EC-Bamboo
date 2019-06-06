// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'GetPlanRuns', description: 'Returns information about build plan runs byt project key and plan key.', {

    step 'GetPlanRuns', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetPlanRuns/steps/GetPlanRuns.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'resultKeys',
        description: 'List of comma-separated plan build result keys.'

    formalOutputParameter 'latestResultKey',
        description: 'Key for the latest build run.'
// === procedure_autogen ends, checksum: 5fe58af8ee7fc9cbfa11ec33aa7f8e72 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}