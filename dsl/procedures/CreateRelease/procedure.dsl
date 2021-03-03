// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'CreateRelease', description: '''This procedure creates new release (version) from the build plan result.''', {

    step 'CreateRelease', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRelease/steps/CreateRelease.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }

    formalOutputParameter 'release',
        description: '''Name of the created release.'''
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: 5c51cee721ca7b5e00d28012e43e5544 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}