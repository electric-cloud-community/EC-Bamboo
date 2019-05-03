// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'DisablePlan', description: 'Disables build plan.', {

    step 'DisablePlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/DisablePlan/steps/DisablePlan.pl").text
        shell = 'ec-perl'

    }
// === procedure_autogen ends, checksum: 68e29c484b09ef18a95a27741e0c32d5 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}