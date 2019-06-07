// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'CollectReportingData', description: 'Collects reporting data for the DevOpsInsight Dashboards. Will collect build results with optional tests run summary.', {

    step 'CollectReportingData', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CollectReportingData/steps/CollectReportingData.pl").text
        shell = 'ec-perl'

        }
// === procedure_autogen ends, checksum: 37bb408cd131bb1c58be36cf9f19888d ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}