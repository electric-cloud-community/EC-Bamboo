// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'GetPlanRuns', description: 'Returns information about build plan runs byt project key and plan key.', {

    step 'GetPlanRuns', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetPlanRuns/steps/GetPlanRuns.pl").text
        shell = 'ec-perl'

    }

    formalOutputParameter 'planKeys',
        description: 'List of comma-separated plan keys.'
// === procedure_autogen ends, checksum: 741d02be4c0718ac6a9af85045005707 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}