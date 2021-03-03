// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'EnablePlan', description: '''This procedure enables build plan.''', {

    step 'EnablePlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/EnablePlan/steps/EnablePlan.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: 270235ea84f0cea13b66df01d24b1961 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}