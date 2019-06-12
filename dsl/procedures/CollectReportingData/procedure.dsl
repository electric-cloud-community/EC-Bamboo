// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'CollectReportingData', description: 'This procedure collects reporting data for the DevOpsInsight Dashboards. It collects build results with optional tests run details.', {

    step 'CollectReportingData', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CollectReportingData/steps/CollectReportingData.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: a66176a09aa81a3269767492b511a6d2 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}