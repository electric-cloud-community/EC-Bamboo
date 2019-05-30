package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Unroll

import java.security.InvalidParameterException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher

class CollectReportingData extends BambooHelper {

    static String procedureName = 'CollectReportingData'
    static String projectName = "EC-Specs $procedureName"

    static String releaseName = "EC-Specs $procedureName Release"
    static String scheduleName = "EC-Specs $procedureName Schedule"

    static def procedureParams = [
            config               : '',
            projectKey           : '',
            planKey              : '',
            retrieveTestResults  : '',
            testCategory         : '',
            transformScript      : '',
            initialRetrievalCount: '',
            metadataPropertyPath : '',
            baseDrilldownUrl     : '',
            previewMode          : '',
            debugLevel           : '',
            releaseName          : '',
            releaseProjectName   : '',
    ]

    static def bambooProject = 'PROJECT'
    static def bambooPlan = 'LONG'
    static def metadataUniqueProperty = "EC-Bamboo-${bambooProject}-${bambooPlan}-build"

    static String SCHEDULE_METADATA_PATH = "/projects/$projectName/schedules/$scheduleName/ecreport_data_tracker"
    static String PROCEDURE_METADATA_PATH = "/projects/$projectName/ecreport_data_tracker"

    @Shared
    static def successfulBuildRun

    def doSetupSpec() {
        redirectLogs()
        createConfiguration(CONFIG_NAME)
        importProject(projectName, 'dsl/CollectReportingDataProject.dsl', [
                projectName  : projectName,
                releaseName  : releaseName,
                scheduleName : scheduleName,
                procedureName: procedureName,
                resourceName : getResourceName(),
                params       : procedureParams,
        ])

        successfulBuildRun = runPlan(bambooProject, bambooPlan, [waitForBuild: 1])
    }

    def doCleanupSpec() {
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
        conditionallyDeleteProject(projectName)
    }

    @Unroll
    @Sanity
    def '#caseId. CollectReportingData - procedure'() {
        given:
        def metadata = buildMetadataValueForCase(metadataCase)
        changeMetadata(metadata, PROCEDURE_METADATA_PATH)

        def procParams = [
                config               : CONFIG_NAME,
                projectKey           : bambooProject,
                planKey              : bambooPlan,
                retrieveTestResults  : '1',
                testCategory         : 'unit-test',
                transformScript      : '',
                initialRetrievalCount: '',
                metadataPropertyPath : PROCEDURE_METADATA_PATH,
                baseDrilldownUrl     : '',
                previewMode          : '0',
                debugLevel           : '1',
                releaseName          : releaseName,
                releaseProjectName   : projectName,
        ]

        when:
        def result = runProcedure(projectName, procedureName, procParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == 'success'

        def summary = getScheduleJobStepSummary(result.jobId)
        assert summary
        checkPayloadCountForMetadataCase(summary, metadataCase)

        cleanup:
        dsl "deleteProperty (propertyName: '${PROCEDURE_METADATA_PATH}')"

        where:
        caseId       | metadataCase
        'CHANGEME_1' | ''
        'CHANGEME_2' | 'last'
//        'CHANGEME_3' | 'future'

    }

    @Unroll
    def '#caseId. CollectReportingData - schedule'() {
        given:
        def metadata = buildMetadataValueForCase(metadataCase)
        changeMetadata(metadata, SCHEDULE_METADATA_PATH)

        def procParams = [
                config               : CONFIG_NAME,
                projectKey           : bambooProject,
                planKey              : bambooPlan,
                retrieveTestResults  : '1',
                testCategory         : 'unit-test',
                transformScript      : '',
                initialRetrievalCount: '',
                metadataPropertyPath : '',
                baseDrilldownUrl     : '',
                previewMode          : '0',
                debugLevel           : '1',
                releaseName          : releaseName,
                releaseProjectName   : projectName,
        ]

        when:
        def result = runSchedule(projectName, scheduleName, procedureName, procParams)

        then:
        println(getJobLink(result.jobId))
        assert result.outcome == 'success'

        def summary = getScheduleJobStepSummary(result.jobId)
        assert summary =~ /Payloads of type build sent: (\d+)/
        checkPayloadCountForMetadataCase(summary, metadataCase)

        cleanup:
        dsl "deleteProperty (propertyName: '${SCHEDULE_METADATA_PATH}')"

        where:
        caseId       | metadataCase
        'CHANGEME_4' | ''
        'CHANGEME_5' | 'last'
//        'CHANGEME_6' | 'future'
    }

    String buildMetadataValueForCase(String metadataCase) {
        if (!metadataCase) {
            return ''
        } else if (metadataCase == 'last') {
            def df1 = DateTimeFormatter.ISO_INSTANT
            def datetime = df1.parse((String) successfulBuildRun['buildStartedTime'])
            return Instant.from(datetime).minusSeconds(2).toString()
        } else if (metadataCase == 'future') {
            return Instant.now().plusSeconds(600).toString()
        }

        throw new InvalidParameterException("metadataCase should be one of 'last|future|'")
    }

    def checkPayloadCountForMetadataCase(summary, metadataCase) {
        if (metadataCase == 'future') {
            assert summary =~ /Up to date, nothing to sync/
            return true
        }

        println("Summary for metadataCase: $metadataCase: " + summary)
        Matcher match = (summary =~ /Payloads of type build sent: (\d+)/)
        assert match && match[0]
        def count = match[0][1]

        println("Checking payload count: " + count)

        if (!metadataCase) {
            assert count
        } else if (metadataCase == 'last') {
            assert count == '1'
        }

        return true
    }

    def getScheduleJobStepSummary(def jobId) {
        def summary = null
        def property = "/myJob/jobSteps/collect/steps/$procedureName/summary"
        println "Trying to get the summary for Procedure: $procedureName, property: $property; jobId: $jobId"
        try {
            summary = getJobProperty(property, jobId)
        } catch (Throwable e) {
            logger.debug("Can't retrieve Upper Step Summary from the property: $property; check job: $jobId")
        }
        return summary
    }

    def changeMetadata(def newStartTime, def metadataProperty = SCHEDULE_METADATA_PATH) {
        if (newStartTime) {
            def json = "{ \"startTime\" : \"$newStartTime\" }"
            setProperty(metadataProperty + "/${metadataUniqueProperty}", json)
        } else {
            logger.debug("No value, metadata will not be changed")
        }

        return true
    }


}