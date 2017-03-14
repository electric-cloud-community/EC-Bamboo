import java.io.File

def procName = 'DisablePlan'
procedure procName, description: "Disables build plan", {
	step 'DisablePlan',
    	  command: new File(pluginDir, "dsl/procedures/$procName/steps/DisablePlan.pl").text,
    	  shell: 'ec-perl'
}
  
