package dsl

def projName = args.projectName
def relName = args.releaseName
def procName = args.procedureName
def schedName = args.scheduleName
def resName = args.resourceName
def params = args.params


project projName, {

    procedure procName, {
        project = projName
        description = ''
        jobNameTemplate = ''
        resourceName = resName
        timeLimit = ''
        timeLimitUnits = 'minutes'
        workspaceName = ''

        params.each { k, defaultValue ->
            formalParameter k, defaultValue: defaultValue, {
                type = 'textarea'
            }
        }

        step 'collect', {
            project = projName
            procedureName = procName
            description = ''
            alwaysRun = '0'
            broadcast = '0'
            condition = ''
            errorHandling = 'failProcedure'
            exclusiveMode = 'none'
            precondition = ''
            releaseMode = 'none'
            resourceName = resName
            subprocedure = 'CollectReportingData'
            subproject = '/plugins/EC-Bamboo/project'
            timeLimit = ''
            timeLimitUnits = 'minutes'
            workspaceName = ''


            params.each { k, v ->
                actualParameter k, '$[' + k + ']'
            }
        }
    }


    release relName, {
        project = projName
        description = ''
    }

    schedule schedName, {
        project = projName
        description = ''
        beginDate = ''
        endDate = ''
        interval = '0'
        intervalUnits = 'minutes'
        misfirePolicy = 'ignore'
        monthDays = ''
        priority = 'normal'
        procedureName = procName
        scheduleDisabled = '0'
        startTime = ''
        stopTime = ''
        timeZone = 'Etc/UTC'
        weekDays = ''
    }

}
