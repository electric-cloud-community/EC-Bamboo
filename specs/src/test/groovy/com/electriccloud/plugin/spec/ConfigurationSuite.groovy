package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.*
import spock.lang.*

@Stepwise

class ConfigurationSuite extends PluginTestHelper {
    @Shared
    def testProjectName = "EC-Nexus-Specs-RetrieveArtifactFromNexus"
    @Shared
    def testProcedureName = "CreateConfiguration"
    @Shared
    String configName

    @Shared
        configs = [
            correct  : 'specConfig',
            incorrect: '[/\\]',
            empty    : '',
        ]

    @Shared
    def instances = [
        correct  : BAMBOO_URL,
        incorrect: "$BAMBOO_URL/incorrect",
        emty     : '',
    ]
    @Shared
    def credentials = [
        correct  : [
            user    : NEXUS_USERNAME,
            password: NEXUS_PASSWORD,
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
        correct: 'Configuration for Nexus created by Automation Tests',
        empty  : '',
    ]

    //@Shared
    //def attemptConnections = checkbox
    //@Shared
    //def debugLevels
    def doSetupSpec(){
        redirectLogs()
    }

    def doCleanupSpec() {
//        deleteConfiguration(PLUGIN_NAME, configName)
        conditionallyDeleteProject(testProjectName)
    }

    @Shared
    String config
    @Shared
    String instance
    @Shared
    String attemptConnection
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

    @Sanity
    @Unroll
    def '#caseId, CreateConfiguration - Sanity'() {

        given:

/* Test getResourceByPublickLink
        def url = "https://github.com/electric-cloud/TestApplications/raw/master/EC-RemoteAccess/keys.zip"
        def dir = "/tmp/test"
        getResourceByPublickLink(testProjectName, url, dir, 'local', 'true', 'true', dir)
*/

        configName = randomize(configs.correct)

        def creds = credentials[credentialCase]

        def params = [
            instance         : instances.correct,
            attemptConnection: attemptConnection,
            debugLevel       : debugLevel,
            desc             : desc
        ]

        when: "When section: Create Configuration"
//        def html = testToHtml(PLUGIN_NAME, testProcedureName, "CreateConfiguration - Sanity", params)
        def result = createPluginConfiguration(configName, params, creds.user, creds.password)
        then: "Verification Create procedures"
        assert result
        println getJobLink(result.jobId)
        assert result.outcome == expectedOutcome
        if (attemptConnection == '1'){
            assert result.logs =~ "Server: Nexus/$NEXUS_VERSION"
        }

        cleanup:
        deleteConfiguration(PLUGIN_NAME, configName)

        where:
        caseId                        | attemptConnection | credentialCase | debugLevel | desc          | expectedOutcome
        //Just Required fields
        'C386776_Step1/C386782_Step1' | '0'               | 'correct'      | 'Info'     | descs.empty   | 'success'
        //All fields
        'C386776_Step2/C386782_Step2' | '1'               | 'correct'      | 'Trace'    | descs.correct | 'success'
    }

    @NewFeature(pluginVersion = "1.0.0")
    @Unroll
    def '#caseId, CreateConfiguration - Positive. New Feature for Nexus v1.0.0'() {

        given:
        configName = randomize(configs.correct)

        def creds = credentials[credentialCase]

        def params = [
                instance         : instance,
                attemptConnection: attemptConnection,
                debugLevel       : debugLevel,
                desc             : desc
        ]

        when: "When section: Create Configuration"
        def result = createPluginConfiguration(configName, params, creds.user, creds.password)

        then: "Verification Create procedures"
        assert result
        println getJobLink(result.jobId)
        assert result.outcome == expectedOutcome
        if (attemptConnection == '1'){
            assert result.logs =~ "Server: Nexus/$NEXUS_VERSION"
        }
        cleanup:
        deleteConfiguration(PLUGIN_NAME, configName)

        where:
        //TODO: Verify all possible Positive Tests
        caseId                        | instance          | attemptConnection | credentialCase | debugLevel | desc          | expectedOutcome
        'C386779_Step1/C386783_Step1' | instances.correct | '1'               | 'correct'      | 'Info'     | descs.empty   | 'success'
        'C386779_Step2/C386783_Step2' | instances.correct | '1'               | 'correct'      | 'Debug'    | descs.correct | 'success'
        'C386779_Step3/C386783_Step3' | instances.correct | '1'               | 'correct'      | 'Trace'    | descs.correct | 'success'
    }

    @Unroll
    @NewFeature(pluginVersion = "1.0.0")
    def '#caseId, CreateConfiguration - Negative, for Nexus v1.0.0'() {
        given:

        def params = [
            instance         : instance,
            attemptConnection: attemptConnection,
            debugLevel       : debugLevel,
            desc             : desc
        ]

        when: "When section: Create Configuration"
        def result = createPluginConfiguration(config, params, user, pass)

        then: "Verification Create procedures"
        assert result
        println getJobLink(result.jobId)
        assert result.outcome == expectedOutcome
        assert result.logs =~errorMessage
        cleanup:
        if (expectedOutcome != 'error') {
            deleteConfiguration(PLUGIN_NAME, config)
        }

        where:
        caseId                        | config                     | instance            | attemptConnection | debugLevel | desc          | user                       | pass                           | expectedOutcome | errorMessage
        'C386781_Step1/C386784_Step1' | randomize(configs.correct) | instances.incorrect | '1'               | '1'        | descs.correct | credentials.correct.user   | credentials.correct.password   | 'error'         | "Response status:404 Not Found"
        'C386781_Step2/C386784_Step2' | randomize(configs.correct) | instances.correct   | '1'               | '1'        | descs.correct | credentials.incorrect.user | credentials.correct.password   | 'error'         | "Authentication check failed. Please check your credentials"
        'C386781_Step3/C386784_Step3' | randomize(configs.correct) | instances.correct   | '1'               | '1'        | descs.correct | credentials.correct.user   | credentials.incorrect.password | 'error'         | "Authentication check failed. Please check your credentials"

//Commented due to specific logic of Perl parsing
//Could be uncommented when input validation will be added
//        'NewFeatureNegative_v1.0.0-3' | randomize(configs.correct) | instances.correct   | ''                | '1'        | descs.correct | credentials.correct.user   | credentials.correct.password   | 'error'
//        'NewFeatureNegative_v1.0.0-4' | randomize(configs.correct) | instances.correct   | '1'               | '-1'       | descs.correct | credentials.correct.user   | credentials.correct.password   | 'error'
//        'NewFeatureNegative_v1.0.0-5' | randomize(configs.correct) | instances.correct   | 'a'               | '1'        | descs.correct | credentials.correct.user   | credentials.correct.password   | 'error'
//        'NewFeatureNegative_v1.0.0-6' | randomize(configs.correct) | instances.correct   | '1'               | 'a'        | descs.correct | credentials.correct.user   | credentials.correct.password   | 'error'
    }

}
