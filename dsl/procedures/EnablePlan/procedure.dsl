import java.io.File

def procName = 'EnablePlan'
procedure procName, description: "Enables build plan.", {
	step 'EnablePlan',
    	  command: new File(pluginDir, "dsl/procedures/$procName/steps/EnablePlan.pl").text,
    	  shell: 'ec-perl'
}
  
