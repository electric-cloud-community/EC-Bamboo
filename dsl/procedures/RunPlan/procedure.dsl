// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'RunPlan', description: 'Runs build plan by plan key and project key.', {

    step 'RunPlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/RunPlan/steps/RunPlan.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'buildResultKey',
        description: 'Composite key for the Run Result.'

    formalOutputParameter 'buildUrl',
        description: 'Link to the result page.'
// === procedure_autogen ends, checksum: 030adee29cc76adfac6cd5c0b6adfe6e ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}