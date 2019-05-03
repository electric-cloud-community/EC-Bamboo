package com.electriccloud.plugin.spec


import com.electriccloud.spec.PluginSpockTestSupport
import org.apache.commons.lang3.StringUtils

class PluginTestHelper extends PluginSpockTestSupport {

    static String automationTestsContextRun = System.getenv('AUTOMATION_TESTS_CONTEXT_RUN') ?: 'Sanity'
    static String PLUGIN_NAME = System.getenv('PLUGIN_NAME') ?: 'EC-Bamboo'
    static String PLUGIN_VERSION = System.getenv('PLUGIN_VERSION') ?: '1.5.0'

    static String BAMBOO_URL = getURL()
    static String BAMBOO_USERNAME = getBambooUsername()
    static String BAMBOO_PASSWORD = getBambooPassword()

    static String CONFIG_NAME = 'specConfig'

    def createConfiguration(String configName = CONFIG_NAME, Map props = [:]) {
        String username = BAMBOO_USERNAME
        String password = BAMBOO_PASSWORD
        String endpoint = BAMBOO_URL

        String checkConnection = 1

        if (System.getenv('RECREATE_CONFIG')) {
            props.recreate = true
        }

        createPluginConfiguration(
            PLUGIN_NAME,
            configName,
            [
                desc             : 'Spec tests configuration',
                endpoint         : endpoint,
                credential       : 'credential',
                debugLevel       : 3,
                checkConnection: checkConnection,
            ],
            username,
            password,
            props
        )
    }

    static String getAssertedEnvVariable(String varName) {
        String varValue = System.getenv(varName)
        println("VAR: '$varName'" + " VALUE: " + varValue ?: '')
        assert varValue
        return varValue
    }

    static String getURL() { getAssertedEnvVariable("BAMBOO_URL") }

    static String getBambooUsername() { getAssertedEnvVariable("BAMBOO_USERNAME") }

    static String getBambooPassword() { getAssertedEnvVariable("BAMBOO_PASSWORD") }

    def runProcedure(String projectName, String procedureName, Map parameters) {
        // Skip undefined values
        def parametersString = parameters
            .findAll { k, v -> v != null }
            .collect { k, v ->
            v = ((String) v).replace('\'', '\\\'')
            "$k: '''$v'''"
        }.join(', ')

        def code = """
            runProcedure(
                projectName: '$projectName',
                procedureName: '$procedureName',
                actualParameter: [
                    $parametersString                 
                ]
            )
        """

        return runProcedureDsl(code)
    }

    def redirectLogs(String parentProperty = '/myJob') {
        def propertyLogName = parentProperty + '/debug_logs'
        dsl """
            setProperty(
                propertyName: "/plugins/${PLUGIN_NAME}/project/ec_debug_logToProperty",
                value: "$propertyLogName"
            )
        """
        return propertyLogName
    }

    String redirectLogsToPipeline() {
        String propertyName = '/myPipelineRuntime/debugLogs'
        dsl """
            setProperty(
                propertyName: "/plugins/${PLUGIN_NAME}/project/ec_debug_logToProperty",
                value: "$propertyName"
            )
        """
        propertyName
    }

    String getJobLogs(def jobId) {
        assert jobId
        def logs
        try {
            logs = getJobProperty("/myJob/debug_logs", jobId)
        } catch (Throwable e) {
            logs = "Possible exception in logs; check job"
        }
        logs
    }

    def getPipelineLogs(flowRuntimeId) {
        assert flowRuntimeId
        getPipelineProperty('/myPipelineRuntime/debugLogs', flowRuntimeId)
    }

    def flexibleIgnore(name = null) {
        def ignoreVar = System.getenv('SPEC_CASE')
        println ignoreVar
        if (!ignoreVar) {
            return false
        }
        if (!name) {
            return true
        }
        return name =~ /$ignoreVar/ ? false : true
    }

    def runProcedureDsl(dslString) {
        redirectLogs()
        assert dslString

        def result = dsl(dslString)
        assert result.jobId

        waitUntil {
            jobCompleted result.jobId
        }
        def logs = getJobLogs(result.jobId)
        def outcome = jobStatus(result.jobId).outcome
        logger.debug("DSL: $dslString")
        logger.debug("Logs: $logs")
        logger.debug("Outcome: $outcome")
        [logs: logs, outcome: outcome, jobId: result.jobId]
    }

    def getStepSummary(def jobId, def stepName) {
        assert jobId
        def summary
        def property = "/myJob/jobSteps/$stepName/summary"
        println "Trying to get the summary for Procedure: checkConnection, property: $property, jobId: $jobId"
        try {
            summary = getJobProperty(property, jobId)
        } catch (Throwable e) {
            logger.debug("Can't retrieve Upper Step Summary from the property: '$property'; check job: " + jobId)
        }
        return summary
    }

    def createResource(def resName) {
        dsl """
            createResource(
                resourceName: '$resName',
                hostName: '127.0.0.1',
                port: '7800'
            )
        """
    }


    def getJobStepDetails(def jobStepId) {
        assert jobStepId
        def stepId
        try {
            stepId = dsl """
            getJobStepDetails(
                jobStepId: '$jobStepId'
                )
          """
        } catch (Throwable e) {
            logger.debug("Can't retrieve job Step Details for job Step: " + jobStepId)
        }
        return stepId
    }

    def getJobStepProperty(def jobStepId, def propertyName) {
        assert jobStepId
        def propertyValue
        try {
            propertyValue = dsl """
            getProperty(
            jobStepId: '$jobStepId',
            propertyName: '$propertyName'
            )
        """
        } catch (Throwable e) {
            logger.debug("Can't retrieve property name $propertyName for jobStepIS: $jobStepId")
        }
        return propertyValue
    }

    def traverse(jobStepId, List stepNames, def propertyName) {
        def retval = []
        println "SubStepId:  $jobStepId"
        getJobStepDetails(jobStepId)?.jobStep?.calledProcedure?.jobStep?.each {
            println "SubStep Name: $it.stepName"
            if (it.stepName in stepNames) {
                def summary = getJobStepProperty(it.jobStepId, propertyName)?.property?.value
                retval << summary
            }
            retval.addAll(traverse(it.jobStepId, stepNames, propertyName))
        }
        return retval
    }

    def createPluginConfiguration(def configName, Map params, def userName, def password) {
        assert PLUGIN_NAME
        assert configName
        def result = runProcedure("""
            runProcedure(
                projectName: "/plugins/$PLUGIN_NAME/project",
                procedureName: 'CreateConfiguration',
                    credential: [
                    [
                        credentialName: 'credential',
                        userName: '$userName',
                        password: '$password'
                    ],
                ],
                actualParameter: [
                    config           : '${configName}', 
                    desc             : '${params.desc}',
                    instance         : '${params.instance}',
                    debugLevel       : '${params.debugLevel}',
                    attemptConnection: '${params.attemptConnection}',
                    credential       : 'credential',
                ]
            )
            """)

        assert result?.jobId
        waitUntil {
            jobCompleted(result)
        }
        return result
    }

    def getWorkSpace(def jobId) {
        return (getJobDetails(jobId).job.workspace.unix[0]) ? getJobDetails(jobId).job.workspace.unix[0] : getJobDetails(jobId).job.workspace.winDrive[0]
    }

    def uploadFileToDocker(def dockerContainerName, def sourceFilePath, def dectinationFilePath, def options = "", def os = 'UNIX-like') {
        def command = "dokcer cp"
        try {
            command = "${command} ${options} ${sourceFilePath} ${dockerContainerName}:${dectinationFilePath}"
            command.execute()
        } catch (Exception e) {
            throw new Exception("Can't copy file: ${sourceFilePath} to ${dockerContainerName}:${dectinationFilePath}" + e)
        }

    }


    def createDirOnResource(def dirPath, def resource = 'local') {
        Map runParams = [
            Path: dirPath,
        ]
        println "Create directory: ${dirPath} on Resource: ${resource}..."
        //plugin Parameters
        //Path - Path of the directory to create
        def result = runProcedure('/plugins/EC-FileOps/project', 'CheckExistence', runParams, [], resource)
        if (result.outcome == 'error') {
            result = runProcedure('/plugins/EC-FileOps/project', 'CreateDirectory', runParams, [], resource)
            println "Directory ${dirPath} has been created sucessfully on Resource: ${resource}"
        } else {
            println "Directory ${dirPath} does exist on Resource: ${resource}"
        }
        assert result.outcome == 'success'
    }


    // MarkupBuilder is a lot cleaner way of generating valid xml/html markup
// than writing tags as string and forgetting to close one ;)
    /*def testToHtml(def pluginName, def procedureName, def testName, Map params) {
        StringBuilder output = new StringBuilder()
        if (params.size() > 0) {
            output.append("<!DOCTYPE html>")
            output.append("""
            <style>
            table {
              font-family: arial, sans-serif;
              border-collapse: collapse;
              width: 100%;
            }

            td, th {
              border: 1px solid #dddddd;
              text-align: left;
              padding: 8px;
            }

            tr:nth-child(even) {
              background-color: #dddddd;
            }
            </style>
            <html>
            <body>
            <h2>Plugin: ${pluginName}</h2>
            <h3>Procedure Name: ${procedureName}</h2>
            <h3>Test Name: ${testName}</h2>
            <h4>Parameters</h3>
            <table>
            """)
            output.append("<tr>\n")
            output.append("  <th>Parameter Name</th>\n")
            output.append("  <th>Value</th>\n")
            output.append("</tr>\n")
            params.each { k, v ->
                output.append("<tr>\n")
                output.append("  <th>${k}</th>\n")
                output.append("  <th>${v}</th>\n")
                output.append("</tr>\n")
            }
            output.append("</table>\n")
            output.append("</body>\n")
            output.append("</html>\n")
            return output.toString()
        }
    }
*/

    int versionCompare(String v1, String v2) {
        int v1Len = StringUtils.countMatches(v1, ".");
        int v2Len = StringUtils.countMatches(v2, ".");

        if (v1Len != v2Len) {
            int count = Math.abs(v1Len - v2Len);
            if (v1Len > v2Len)
                for (int i = 1; i <= count; i++)
                    v2 += ".0"
            else
                for (int i = 1; i <= count; i++)
                    v1 += ".0"
        }

        if (v1.equals(v2))
            return 0

        String[] v1Str = StringUtils.split(v1, ".");
        String[] v2Str = StringUtils.split(v2, ".");
        for (int i = 0; i < v1Str.length; i++) {
            String str1 = "", str2 = ""
            for (char c : v1Str[i].toCharArray()) {
                if (Character.isLetter(c)) {
                    int u = c - 'a' + 1;
                    if (u < 10)
                        str1 += String.valueOf("0" + u)
                    else
                        str1 += String.valueOf(u)
                } else
                    str1 += String.valueOf(c)
            }
            for (char c : v2Str[i].toCharArray()) {
                if (Character.isLetter(c)) {
                    int u = c - 'a' + 1
                    if (u < 10)
                        str2 += String.valueOf("0" + u)
                    else
                        str2 += String.valueOf(u)
                } else
                    str2 += String.valueOf(c)
            }
            v1Str[i] = "1" + str1
            v2Str[i] = "1" + str2

            int num1 = Integer.parseInt(v1Str[i])
            int num2 = Integer.parseInt(v2Str[i])

            if (num1 != num2) {
                if (num1 > num2)
                    return 1
                else
                    return 2
            }
        }
        return -1
    }

    def isVersion(String v1) {
        assert v1
        String[] numbers = v1.split("\\.")
        if (numbers.size() <= 0) {
            throw new Exception("Is not supported version format.")
        }
        numbers.each {
            try {
                it.toInteger()
            } catch (Exception e) {
                throw new Exception("Can not be converted to numbers. " + e)
            }
        }
        return true
    }

    def versionVerification(def expectedVersion, def actualVersion) {
        assert isVersion(expectedVersion)
        assert isVersion(actualVersion)
        def rez = versionCompare((String) expectedVersion, (String) actualVersion)
        assert ((rez == 0) || (rez == 2))
    }

    Map getArtifactParametersForPath(def jobProperties, String path = defaultResProp.path) {
        println("PATH : " + path)

        if (path =~ /myJob/) {
            path = path.replace('/myJob/', '')
        }

        def pathSegments = path.split('/')

        // Simply going one step deeper for every segment
        def result = jobProperties
        pathSegments.each { it -> result = result[it] }

        return result
    }

    String getFilePath(def jobId, def fileName, String destinationDir = "") {
        def workspace = getWorkSpace(jobId)
        String slash = ""
        slash = workspace[0] == '/' ? '/' : '\\'
        if (destinationDir) {
            if (destinationDir[-1] in ['\\', '/']) {
                destinationDir = destinationDir[0..-2]
            }
            return destinationDir + slash + (String) fileName
        } else {
            return workspace + slash + (String) fileName
        }
    }


    String getResultPropertySheet(def propParam) {
        def resultPropertyPath
        if (propParam) {
            resultPropertyPath = "/myJob/${propParam}"

        } else {
            // This is a default defined in a plugin
            resultPropertyPath = '/myJob/retrievedArtifactVersions/local'
        }
        println('Getting the ResultPropertySheet')
        return (String) resultPropertyPath
    }


    def findStrings(String str, def array) {
        array.each { it ->
            if (!str.contains(it)) {
                return false
            }
        }
        return true
    }
}