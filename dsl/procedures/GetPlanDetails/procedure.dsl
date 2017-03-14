import java.io.File

def procName = 'GetPlanDetails'
procedure procName, description: "Prints build plan details.", {
    step 'GetPlanDetails',
    command: new File(pluginDir, "dsl/procedures/$procName/steps/GetPlanDetails.pl").text,
    shell: 'ec-perl'
}
