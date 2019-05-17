// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'RunPlan', description: 'Runs build plan by plan key and project key.', {

    step 'RunPlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/RunPlan/steps/RunPlan.pl").text
        shell = 'ec-perl'

    }

    formalOutputParameter 'planKeys',
        description: 'List of comma-separated plan keys.'
// === procedure_autogen ends, checksum: 686f4ac060e9a77c24933423c3681ddf ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}