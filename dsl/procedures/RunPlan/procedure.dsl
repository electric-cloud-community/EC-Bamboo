// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'RunPlan', description: 'This procedure runs Bamboo build plan.', {

    step 'RunPlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/RunPlan/steps/RunPlan.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'buildResultKey',
        description: 'Composite key for the Run Result.'

    formalOutputParameter 'buildUrl',
        description: 'Link to the result page.'
// === procedure_autogen ends, checksum: 1b881808a3a68d3660935543567c0dbb ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}