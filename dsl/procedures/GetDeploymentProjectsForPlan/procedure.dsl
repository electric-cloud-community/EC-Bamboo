// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'GetDeploymentProjectsForPlan', description: 'Requests and returns list of all deployment projects linked to this build plan.', {

    step 'GetDeploymentProjectsForPlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetDeploymentProjectsForPlan/steps/GetDeploymentProjectsForPlan.pl").text
        shell = 'ec-perl'

    }
// === procedure_autogen ends, checksum: 14fe7b8b55ba6fb1d4852d5f4f02432a ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}