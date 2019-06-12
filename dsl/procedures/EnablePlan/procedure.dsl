// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'EnablePlan', description: 'This procedure enables build plan.', {

    step 'EnablePlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/EnablePlan/steps/EnablePlan.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: be72adfdc468565dac1413f0c2fdb274 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}