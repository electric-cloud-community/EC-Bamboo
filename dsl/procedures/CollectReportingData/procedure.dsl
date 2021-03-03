// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK BELOW=== procedure_autogen starts ===
procedure 'CollectReportingData', description: '''This procedure collects reporting data for the DevOpsInsight Dashboards. It collects build results with optional tests run details.''', {

    step 'CollectReportingData', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CollectReportingData/steps/CollectReportingData.pl").text
        shell = 'ec-perl'
        shell = 'ec-perl'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== procedure_autogen ends, checksum: 890a31972ba186a957833692e5bf5d6b ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}