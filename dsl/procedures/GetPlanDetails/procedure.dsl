// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'GetPlanDetails', description: 'Gets build plan details.', {

    step 'GetPlanDetails', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetPlanDetails/steps/GetPlanDetails.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: 2345aa34d7543983c9ffcdeb8fbe5e17 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}