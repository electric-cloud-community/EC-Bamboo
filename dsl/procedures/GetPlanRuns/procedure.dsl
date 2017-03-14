import java.io.File

def procName = 'GetPlanRuns'
procedure procName, description: "Returns information about build plan runs byt project key and plan key.", {
    step 'GetPlanRuns',
    command: new File(pluginDir, "dsl/procedures/$procName/steps/GetPlanRuns.pl").text,
    shell: 'ec-perl'
    
}
  
