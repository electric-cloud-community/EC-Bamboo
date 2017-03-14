import java.io.File

def procName = 'GetAllPlans'
procedure procName, description: "Returns all plans that are available for current user.", {
    step 'GetAllPlans',
    command: new File(pluginDir, "dsl/procedures/$procName/steps/GetAllPlans.pl").text,
    shell: 'ec-perl'
}
