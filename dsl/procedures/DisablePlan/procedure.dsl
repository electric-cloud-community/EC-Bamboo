// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'DisablePlan', description: 'Disables build plan', {

    step 'DisablePlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/DisablePlan/steps/DisablePlan.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: bf6d459ab003a63d95aa7a82b7f8810b ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}