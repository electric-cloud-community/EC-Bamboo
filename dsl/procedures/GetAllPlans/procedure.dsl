// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'GetAllPlans', description: 'Returns all plans that are available for current user.', {

    step 'GetAllPlans', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetAllPlans/steps/GetAllPlans.pl").text
        shell = 'ec-perl'

        }

    formalOutputParameter 'planKeys',
        description: 'List of comma-separated plan keys'
// === procedure_autogen ends, checksum: 97f3f0f71b433088f01b46aca21e41d1 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}