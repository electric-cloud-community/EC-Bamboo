// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'EnablePlan', description: 'Enables build plan.', {

    step 'EnablePlan', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/EnablePlan/steps/EnablePlan.pl").text
        shell = 'ec-perl'

    }
// === procedure_autogen ends, checksum: d0b468a7cc728d36ec6dbc832185a563 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}