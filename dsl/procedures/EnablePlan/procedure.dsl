// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'EnablePlan', description: 'Enables build plan.', {

    step 'EnablePlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/EnablePlan/steps/EnablePlan.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: e32b4fe57f679943d877bf309b8eb3e3 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}