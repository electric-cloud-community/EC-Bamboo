// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'GetPlanDetails', description: 'This procedure prints Bamboo build plan details.', {

    step 'GetPlanDetails', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetPlanDetails/steps/GetPlanDetails.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: 36701c167ca4ddc599097b7d112a9fca ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}