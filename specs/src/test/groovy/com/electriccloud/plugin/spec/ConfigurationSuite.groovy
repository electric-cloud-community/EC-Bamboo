package com.electriccloud.plugin.spec


import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise

class ConfigurationSuite extends PluginTestHelper {
    @Shared
    String testProcedureName = "CreateConfiguration"

    @Shared
    String testProjectName = "EC-Nexus $testProcedureName"

    @Shared
    String config
    @Shared
    String endpoint
    @Shared
    String checkConnection
    @Shared
    String debugLevel
    @Shared
    String desc
    @Shared
    String caseId
    @Shared
    String user
    @Shared
    String pass
    @Shared
    def expectedOutcome


    @Shared
            configs = [
                    correct  : 'specConfig',
                    incorrect: '[/\\]',
                    empty    : '',
            ]

    @Shared
    def endpoints = [
            correct  : BAMBOO_URL,
            incorrect: "$BAMBOO_URL/incorrect",
            emty     : '',
    ]
    @Shared
    def credentials = [
            correct  : [
                    user    : BAMBOO_USERNAME,
                    password: BAMBOO_PASSWORD,
            ],
            incorrect: [
                    user    : 'incorrectUser',
                    password: 'incorrectPass',
            ],
            empty    : [
                    user    : '',
                    password: '',
            ]
    ]

    @Shared
    def descs = [
            correct: 'Configuration for Bamboo created by Automation Tests',
            empty  : '',
    ]

    def doSetupSpec() {
        redirectLogs()
    }

    def doCleanupSpec() {
        conditionallyDeleteProject(testProjectName)
    }

    @Sanity
    @Unroll
    def '#caseId, CreateConfiguration - Sanity'() {

        given:
        config = randomize(configs.correct)

        def creds = credentials[credentialCase]

        def params = [
                endpoint: endpoints.correct,
                checkConnection: checkConnection,
                debugLevel     : debugLevel,
                desc             : desc
        ]

        when: "When section: Create Configuration"
        def result = createPluginConfiguration(config, params, creds.user, creds.password)
        then: "Verification Create procedures"
        assert result
        println getJobLink(result.jobId)

        assert result.outcome == expectedOutcome

        cleanup:
        deleteConfiguration(PLUGIN_NAME, config)

        where:
        caseId       | checkConnection | credentialCase | debugLevel | desc          | expectedOutcome
        //Just Required fields
        'CHANGEME_1' | '0'             | 'correct'      | 'Info'     | descs.empty   | 'success'
        //All fields
        'CHANGEME_2' | '1'             | 'correct'      | 'Trace'    | descs.correct | 'success'
    }

/*
    @NewFeature(pluginVersion = "1.0.0")
    @Unroll
    def '#caseId, CreateConfiguration - Positive. New Feature for Nexus v1.0.0'() {

        given:
        config = randomize(configs.correct)

        def creds = credentials[credentialCase]

        def params = [
                instance : endpoint,
                checkConnection: checkConnection,
                debugLevel     : debugLevel,
                desc           : desc
        ]

        when: "When section: Create Configuration"
        def result = createPluginConfiguration(config, params, creds.user, creds.password)

        then: "Verification Create procedures"
        assert result
        println getJobLink(result.jobId)
        assert result.outcome == expectedOutcome

        cleanup:
        deleteConfiguration(PLUGIN_NAME, config)

        where:
        caseId                        | endpoint          | checkConnection | credentialCase | debugLevel | desc          | expectedOutcome
        'C386779_Step1/C386783_Step1' | endpoints.correct | '1'             | 'correct'      | 'Info'     | descs.empty   | 'success'
        'C386779_Step2/C386783_Step2' | endpoints.correct | '1'             | 'correct'      | 'Debug'    | descs.correct | 'success'
        'C386779_Step3/C386783_Step3' | endpoints.correct | '1'             | 'correct'      | 'Trace'    | descs.correct | 'success'
    }

    @Unroll
    @NewFeature(pluginVersion = "1.0.0")
    def '#caseId, CreateConfiguration - Negative, for Nexus v1.0.0'() {
        given:

        def params = [
                bamboo_server_url : endpoint,
//                checkConnection: checkConnection,
//                debugLevel     : debugLevel,
                desc           : desc
        ]

        when: "When section: Create Configuration"
        def result = createPluginConfiguration(config, params, user, pass)

        then: "Verification Create procedures"
        assert result
        println getJobLink(result.jobId)
        assert result.outcome == expectedOutcome
        assert result.logs =~ errorMessage
        cleanup:
        if (expectedOutcome != 'error') {
            deleteConfiguration(PLUGIN_NAME, config)
        }

        where:
        caseId                        | config                     | endpoint            | checkConnection | debugLevel | desc          | user                       | pass                           | expectedOutcome | errorMessage
        'C386781_Step1/C386784_Step1' | randomize(configs.correct) | endpoints.incorrect | '1'             | '1'        | descs.correct | credentials.correct.user   | credentials.correct.password   | 'error'         | "Response status:404 Not Found"
        'C386781_Step2/C386784_Step2' | randomize(configs.correct) | endpoints.correct   | '1'             | '1'        | descs.correct | credentials.incorrect.user | credentials.correct.password   | 'error'         | "Authentication check failed. Please check your credentials"
        'C386781_Step3/C386784_Step3' | randomize(configs.correct) | endpoints.correct   | '1'             | '1'        | descs.correct | credentials.correct.user   | credentials.incorrect.password | 'error'         | "Authentication check failed. Please check your credentials"
    }
*/

}
