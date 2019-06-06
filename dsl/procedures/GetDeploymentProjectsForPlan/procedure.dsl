// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'GetDeploymentProjectsForPlan', description: 'Requests and returns list of all deployment projects linked to this build plan.', {

    step 'GetDeploymentProjectsForPlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetDeploymentProjectsForPlan/steps/GetDeploymentProjectsForPlan.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'deploymentProjectKeys',
        description: 'List of comma-separated deployment project keys for the plan'
// === procedure_autogen ends, checksum: 2706417be3516d652282dccc810982e4 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}