// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'DisablePlan', description: 'This procedure disables build plan.', {

    step 'DisablePlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/DisablePlan/steps/DisablePlan.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: 53e8750260334b8fd39f8a1878cd356e ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}