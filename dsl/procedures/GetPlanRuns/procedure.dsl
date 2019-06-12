// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'GetPlanRuns', description: 'This procedure returns information about runs on Bamboo build plan.', {

    step 'GetPlanRuns', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetPlanRuns/steps/GetPlanRuns.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'resultKeys',
        description: 'List of comma-separated plan build result keys.'

    formalOutputParameter 'latestResultKey',
        description: 'Key for the latest build run.'
// === procedure_autogen ends, checksum: 37dbdfa4666f61003364175a7788ef20 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}