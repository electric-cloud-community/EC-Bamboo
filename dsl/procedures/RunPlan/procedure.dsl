import java.io.File

def procName = 'RunPlan'
procedure procName, {

	step 'RunPlan',
    	  command: new File(pluginDir, "dsl/procedures/$procName/steps/RunPlan.pl").text,
    	  shell: 'ec-perl'

	// add more steps here, e.g., 
	//step 'step2',
    //	  command: new File(pluginDir, "dsl/procedures/$procName/steps/step2.groovy").text,
    //	  shell: 'ec-groovy'
	  
}
  
