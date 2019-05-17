package com.electriccloud.plugin.spec


import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*

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
                endpoint       : endpoints.correct,
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
        caseId       | checkConnection | credentialCase | debugLevel | desc          | expectedOutcome
        //Just Required fields
        'CHANGEME_1' | '0'             | 'correct'      | 'Info'     | descs.empty   | 'success'
        //All fields
        'CHANGEME_2' | '1'             | 'correct'      | 'Trace'    | descs.correct | 'success'
    }

}