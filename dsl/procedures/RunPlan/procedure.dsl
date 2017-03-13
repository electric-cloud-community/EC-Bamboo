import java.io.File

def procName = 'RunPlan'
procedure procName, description: "Runs build plan by plan key and project key.", {
	step 'RunPlan',
    	  command: new File(pluginDir, "dsl/procedures/$procName/steps/RunPlan.pl").text,
    	  shell: 'ec-perl'
}
  
