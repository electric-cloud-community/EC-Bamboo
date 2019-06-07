package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise

class ConfigurationSuite extends BambooHelper {
    @Shared
    String testProcedureName = "CreateConfiguration"

    @Shared
    String config
    @Shared
    String endpoint
//    @Shared
//    String checkConnection
    @Shared
    String debugLevel = 1
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

    static configs = [
            correct  : 'specConfig',
            incorrect: '[/\\]',
            empty    : '',
    ]


    static def endpoints = [
            correct  : BAMBOO_URL,
            incorrect: "$BAMBOO_URL/incorrect",
            empty     : '',
    ]

    static def credentials = [
            valid  : [
                    user: BAMBOO_USERNAME,
                    pass: BAMBOO_PASSWORD,
            ],
            invalid: [
                    user: 'incorrectUser',
                    pass: 'incorrectPass',
            ],
            empty  : [
                    user: '',
                    pass: '',
            ]
    ]

    static def proxyCredentials = [
            valid  : [
                    user: (isProxyAvailable() ? getProxyUsername() : ''),
                    pass: (isProxyAvailable() ? getProxyPassword() : ''),
            ],
            invalid: [
                    user: 'incorrectUser',
                    pass: 'incorrectPass',
            ],
            empty  : [
                    user: '',
                    pass: '',
            ]
    ]

    static def proxyUrls = [
            valid  : (isProxyAvailable() ? getProxyURL() : 'no proxy'),
            invalid: 'https://google.com:3128',
            empty  : ''
    ]

    static def descs = [
            correct: 'Configuration for Bamboo created by Automation Tests',
            empty  : '',
    ]

    def doSetupSpec() {
        redirectLogs()
    }

    @Sanity
    @Unroll
    @IgnoreIf({ PluginTestHelper.isProxyAvailable() })
    def '#caseId, CreateConfiguration - Sanity'() {
        given:
        config = randomize(configs.correct)
        def creds = credentials[credentialCase]
        def params = [
                config    : config,
                endpoint  : endpoints.correct,
                debugLevel: debugLevel,
                desc      : desc
        ]
        when: "When section: Create Configuration"
        def result = createPluginConfiguration(params, creds)

        then: "Verification Create procedures"
        assert result
        println getJobLink(result.jobId)

        assert result.outcome == expectedOutcome

        // Using created config
        assert getPlanDetails('PROJECT', 'PLAN', [config: config, resultFormat: 'none'])

        cleanup:
        deleteConfiguration(PLUGIN_NAME, config)

        where:
        caseId       | credentialCase | desc          | expectedOutcome
        //Just Required fields
        'CHANGEME_1' | 'valid'        | descs.empty   | 'success'
        //All fields
        'CHANGEME_2' | 'valid'        | descs.correct | 'success'
    }

    @Sanity
    @Unroll
    @IgnoreIf({ !PluginTestHelper.isProxyAvailable() })
    def '#caseId, CreateConfiguration - Proxy - Sanity'() {
        given:
        config = randomize(configs.correct)
        endpoint = endpoints.correct

        def creds = credentials['valid']

        def proxyUrl = proxyUrls[proxyUrlCase]
        def proxyCreds = proxyCredentials[proxyCredsCase]

        def params = [
                config      : config,
                endpoint    : endpoint,
                debugLevel  : 3,
                desc        : desc,
                httpProxyUrl: proxyUrl
        ]
        when: "When section: Create Configuration"
        def result = createPluginConfiguration(params, creds, proxyCreds)

        then: "Verification Create procedures"
        assert result
        println getJobLink(result.jobId)
        assert result.outcome == expectedOutcome

        // Using this config
        try {
            assert getPlanDetails('PROJECT', 'PLAN', [config: config, resultFormat: 'none'])
        }
        catch (AssertionError e){
            if (expectedRetrievalOutcome == 'success'){
                throw new AssertionError(e)
            }
        }

        cleanup:
        deleteConfiguration(PLUGIN_NAME, config)

        where:
        caseId       | proxyUrlCase | proxyCredsCase | desc          | expectedOutcome | expectedRetrievalOutcome
        'CHANGEME_1' | 'valid'      | 'valid'        | descs.empty   | 'success'       | 'success'
        'CHANGEME_2' | 'valid'      | 'invalid'      | descs.empty   | 'success'       | 'proxyAccessDenied'
        'CHANGEME_3' | 'invalid'    | 'invalid'      | descs.correct | 'success'       | 'connectFailed'
    }


    Object createPluginConfiguration(Map params, Map credential = [:], Map proxyCredential = [:]) {
        def projectName = "/plugins/$PLUGIN_NAME/project"
        assert credential

        def credentials = []

        params.basic_credential = 'basic_credential'
        credentials.push([
                credentialName: 'basic_credential',
                userName      : credential.user ?: '',
                password      : credential.pass ?: ''
        ])

        params.proxy_credential = 'proxy_credential'
        credentials.push([
                credentialName: 'proxy_credential',
                userName      : proxyCredential.user ?: '',
                password      : proxyCredential.pass ?: ''
        ])

        return runProcedure(projectName, testProcedureName, params, credentials)
    }

}