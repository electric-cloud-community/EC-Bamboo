// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'GetPlanDetails', description: 'Gets build plan details.', {

    step 'GetPlanDetails', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetPlanDetails/steps/GetPlanDetails.pl").text
        shell = 'ec-perl'

    }
// === procedure_autogen ends, checksum: a5ab9d114d2e396b8f5c48bb383d3b5a ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}