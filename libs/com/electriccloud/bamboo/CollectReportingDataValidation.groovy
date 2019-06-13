package com.electriccloud.bamboo

import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes

class CollectReportingDataValidation {

    static final String PLUGIN_NAME = 'EC-Bamboo'
    static final String CONFIG_LOCATION = 'ec_plugin_cfgs'
    static final ArrayList REQUIRED_PARAMETERS = ['config', 'projectKey']
    static final ArrayList SUPPORTED_PAYLOAD_TYPES = ['build']

    static String getSourceDetails(Map pluginParameters) {
        def sourceIdentifier = pluginParameters.projectKey

        if (pluginParameters.planKey) {
            sourceIdentifier += '-' + pluginParameters.planKey
        }

        return sourceIdentifier
    }

    void validatePluginParams(Map pluginParameters) {
        if (pluginParameters == null) {
            throw EcException
                    .code(ErrorCodes.InvalidParameterValue)
                    .message("pluginParameters are required for dsl script")
                    .build();
        }

        checkRequiredParameters(pluginParameters)

        // Specific checks can be placed below this line
        String frequency = pluginParameters.frequency

        if (!((frequency ?: "").isInteger()) || Integer.parseInt(frequency) <= 0) {
            throw EcException.code(ErrorCodes.InvalidArgument)
                    .message("'Polling Frequency' must be a positive integer.")
                    .build();
        }

    }

    static def getCreateActualParameters(Map pluginParameters) {
        return [
                config               : pluginParameters.config,
                projectKey           : pluginParameters.projectKey,
                planKey              : pluginParameters.planKey,
                initialRecordsCount: pluginParameters.initialRecordsCount,
                transformScript      : pluginParameters.transformScript,
                metadataPropertyPath : pluginParameters.metadataPropertyPath,
                baseDrilldownUrl     : pluginParameters.baseDrilldownUrl,
                previewMode          : '0',
                debugLevel           : '0',
        ]
    }


    static def getModifyActualParameters(Map pluginParameters) {
        return [
                config          : pluginParameters.config,
                projectKey      : pluginParameters.projectKey,
                planKey         : pluginParameters.planKey,
                transformScript : pluginParameters.transformScript,
                baseDrilldownUrl: pluginParameters.baseDrilldownUrl,
        ]
    }

    static void checkPayloadType(String payloadType) {
        if (payloadType == null) {
            throw EcException
                    .code(ErrorCodes.InvalidParameterValue)
                    .message("payloadType is required for dsl script")
                    .build();
        }

        if (SUPPORTED_PAYLOAD_TYPES.indexOf(payloadType) < 0) {
            throw EcException
                    .code(ErrorCodes.InvalidParameterValue)
                    .message("Payload type '$payloadType' is not supported")
                    .build();
        }
    }

    private static void checkRequiredParameters(Map pluginParameters) {
        REQUIRED_PARAMETERS.each { it ->
            if (pluginParameters[it] == null) {
                throw EcException
                        .code(ErrorCodes.MissingArgument)
                        .message("plugin parameter $it is required for dsl script")
                        .build();
            }
        }

    }

    static String getConfigProperty(Map pluginParameters) {
        String configName = pluginParameters.config
        return "/plugins/${PLUGIN_NAME}/project/${CONFIG_LOCATION}/${configName}/endpoint"
    }
}
