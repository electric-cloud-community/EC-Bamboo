package FlowPlugin::Bamboo;
use strict;
use warnings;
use base qw/FlowPDF/;
use Data::Dumper;
# Feel free to use new libraries here, e.g. use File::Temp;

use FlowPDF::Log;
use FlowPDF::Helpers qw/bailOut/;
use FlowPlugin::REST;
use FlowPDF::Constants qw/AUTH_SCHEME_VALUE_FOR_BASIC_AUTH/;

use JSON qw/decode_json/;
use URI::Escape qw/uri_escape_utf8/;

# Service function that is being used to set some metadata for a plugin.
sub pluginInfo {
    return {
        pluginName          => '@PLUGIN_KEY@',
        pluginVersion       => '@PLUGIN_VERSION@',
        configFields        => [ 'config' ],
        configLocations     => [ 'ec_plugin_cfgs' ],
        defaultConfigValues => {
            authScheme => AUTH_SCHEME_VALUE_FOR_BASIC_AUTH
        }
    };
}

sub init {
    my ($self, $params) = @_;

    my FlowPDF::Context $context = $self->getContext();

    # Check if should redirect logs
    my $propLocation = '/plugins/@PLUGIN_KEY@/project/ec_debug_logToProperty';
    eval {
        my $debugToPropertyXpath = $context->getEc->getProperty($propLocation);
        my $debugToProperty = $debugToPropertyXpath->findvalue('//value')->string_value();;
        if (defined $debugToProperty && $debugToProperty ne '') {
            FlowPDF::Log::setLogToProperty($debugToProperty);
            FlowPDF::Log::FW::setLogToProperty($debugToProperty);
        }
    };

    # Show framework logs when debug level is set to "Trace"
    if ($params->{debugLevel} >= FlowPDF::Log::TRACE) {
        FlowPDF::Log::FW::setLogLevel(FlowPDF::Log::TRACE);
    }

    $self->{restClient} = FlowPlugin::REST->new($context, {
        APIBase     => '/rest/api/latest/',
        contentType => 'json',
        errorHook   => {
            default => sub {
                return $self->defaultErrorHandler(@_)
            }
        }
    });
}

sub config {
    my $self = shift;
    unless ($self->{_config}) {
        $self->{_config} = $self->getContext()->getConfigValues();
    }
    return $self->{_config};
};

#@returns FlowPlugin::REST
sub client {return shift->{restClient}};

=head2 getAllPlans

=cut
sub getAllPlans {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    # Setting default parameters
    $params->{resultFormat} ||= 'json';
    $params->{resultPropertySheet} ||= '/myJob/plans';

    logInfo("Requesting and formatting information");
    my @infoToSave = ();
    if (!$params->{projectKey}) {
        my $response = $self->client->get('/project', { expand => 'projects.project.plans.plan' });
        return unless defined $response;

        for my $project (@{$response->{projects}{project}}) {
            logInfo("Found project: '$project->{key}'");

            for my $plan (@{$project->{plans}{plan}}) {
                logInfo("Found plan: '$plan->{key}'");
                push(@infoToSave, _planToShortInfo($plan));
            }
        }
    }
    else {
        my $response = $self->client->get('/project/' . $params->{projectKey}, { expand => 'plans.plan' });
        return unless defined $response;

        logInfo("Found project: '$response->{key}'");
        for my $plan (@{$response->{plans}{plan}}) {
            logInfo("Found plan: '$plan->{key}'");
            push(@infoToSave, _planToShortInfo($plan));
        }
    }

    # When have no information
    if (!@infoToSave) {
        $stepResult->setJobStepOutcome('warning');
        $stepResult->setJobSummary("No plans found");
        $stepResult->setJobStepSummary("No plans found");
        return
    }

    # Save to a properties
    $self->saveResultProperties(
        $stepResult,
        $params->{resultFormat},
        $params->{resultPropertySheet},
        \@infoToSave
    );

    # Saving outcome properties and parameters
    my $planKeysStr = join(', ', map {$_->{key}} @infoToSave);
    $stepResult->setOutputParameter('planKeys', $planKeysStr);

    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary("Found " . scalar(@infoToSave) . ' plan(s).');
    $stepResult->setJobSummary("Found " . scalar(@infoToSave) . ' plan(s).');

    $stepResult->apply();
}


=head2 getPlanDetails

=cut
sub getPlanDetails {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    # Setting default parameters
    $params->{resultFormat} ||= 'json';
    $params->{resultPropertySheet} ||= '/myJob/plan';

    my $planKey = "$params->{projectKey}-$params->{planKey}";

    logInfo("Requesting details for $planKey");
    my $response = $self->client->get("/plan/$planKey", { expand => 'stages.stage' }, undef, {
        errorHook => {
            404 => sub {
                $stepResult->setJobStepOutcome('error');
                $stepResult->setJobSummary("Plan '$planKey' was not found");
                $stepResult->setJobStepSummary("Plan '$planKey' was not found");
                return;
            }
        }
    });
    return unless defined $response;

    logInfo("Found plan: '$response->{key}'");

    my $infoToSave = _planToShortInfo($response, [ 'stages' ]);

    # Save to a properties
    $self->saveResultProperties(
        $stepResult,
        $params->{resultFormat},
        $params->{resultPropertySheet},
        $infoToSave
    );

    # Saving outcome properties and parameters
    $stepResult->setOutputParameter('planKeys', $infoToSave->{key});

    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary("Received info about $infoToSave->{key}.");
    $stepResult->setJobSummary("Received info about $infoToSave->{key}.");

    $stepResult->apply();
}

sub getDeploymentProjectsForPlan {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    $params->{resultFormat} ||= 'json';
    $params->{resultPropertySheet} ||= '/myJob/deploymentProjects';

    my $planKey = "$params->{projectKey}-$params->{planKey}";

    logInfo("Requesting information deployment projects for plan $planKey");
    my $deployProjectsRefs = $self->client->get("/deploy/project/forPlan", { planKey => $planKey });
    return unless defined $deployProjectsRefs;

    # Result is array ref of { id => '', name => '' }
    my @infoToSave = ();
    for my $deployRef (@$deployProjectsRefs) {
        logInfo("Found deployment project: " . $deployRef->{name});

        # Get details for each plan
        my $deploymentProjectId = $deployRef->{id};
        my $deploymentProjectInfo = $self->client->get("/deploy/project/$deploymentProjectId");
        push(@infoToSave, _deploymentProjectToShortInfo($deploymentProjectInfo));
    }

    if (!@infoToSave) {
        $stepResult->setJobStepOutcome("warning");
        $stepResult->setJobStepSummary("No deployment projects found for plan: $planKey");
        $stepResult->setJobSummary("No deployment projects found for plan: $planKey");
        return;
    }

    $self->saveResultProperties($stepResult, $params->{resultFormat}, $params->{resultPropertySheet}, \@infoToSave);

    my @deploymentProjectKeys = map {$_->{key}} @infoToSave;
    $stepResult->setOutputParameter('deploymentProjectKeys', join(', ', @deploymentProjectKeys));

    my $summary = "Deployment projects info saved to property(ies).";
    logInfo($summary);
    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary($summary);
    $stepResult->setJobSummary("Found " . (scalar @infoToSave) . " deployment project(s).");
    $stepResult->apply();
}

=head2 getPlanRuns

=cut
sub getPlanRuns {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    # Setting default parameters
    $params->{resultFormat} ||= 'json';
    $params->{resultPropertySheet} ||= '/myJob/planRuns';

    my $planKey = "$params->{projectKey}-$params->{planKey}";
    my %requestParameters = (
        expand        => 'results.result.artifacts,results.result.labels',
        'max-results' => (defined $params->{maxResults} ? $params->{maxResults} : 25)
    );

    if (defined $params->{buildState} && $params->{buildState} ne 'All') {
        $requestParameters{buildstate} = $params->{buildState};
    }

    logInfo("Requesting and formatting information");
    my @infoToSave = ();
    my $response = $self->client->get("/result/$planKey", \%requestParameters, undef, {
        errorHook => {
            404 => sub {
                $stepResult->setJobStepOutcome('warning');
                $stepResult->setJobSummary("Plan '$planKey' was not found.");
                $stepResult->setJobStepSummary("Plan '$planKey' was not found.");
                return;
            }
        }
    });
    return unless defined $response;

    for my $buildResult (@{$response->{results}{result}}) {
        logInfo("Found build result: '$buildResult->{key}'");
        push(@infoToSave, _planBuildResultToShortInfo($buildResult, [ 'artifacts', 'labels' ]));
    }

    # When have no information
    if (!@infoToSave) {
        $stepResult->setJobStepOutcome('warning');
        $stepResult->setJobSummary("No results found for plan");
        $stepResult->setJobStepSummary("No results found for plan");
        return
    }

    # Save to a properties
    $self->saveResultProperties(
        $stepResult,
        $params->{resultFormat},
        $params->{resultPropertySheet},
        \@infoToSave
    );

    # Saving outcome properties and parameters
    my $buildKeysStr = join(', ', map {$_->{key}} @infoToSave);
    $stepResult->setOutputParameter('resultKeys', $buildKeysStr);

    # Look for the latest key
    my @sortedKeys = sort map {$_->{key}} @infoToSave;
    $stepResult->setOutputParameter('latestResultKey', $sortedKeys[$#sortedKeys]);

    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary('Plan run(s) information was saved to properties.');
    $stepResult->setJobSummary('Plan run(s) information was saved to properties.');

    $stepResult->apply();
}

sub createRelease {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    $params->{resultFormat} ||= 'json';
    $params->{resultPropertySheet} ||= '/myJob/version';

    if (!$params->{versionName} && !$params->{requestVersionName}) {
        bailOut("Either 'Version Name' or 'Request Version Name?' should be specified.")
    }

    # Stripping plan key from the build result key [PROJECT-PLAN]-22
    my ($project, $plan) = split('-', $params->{planBuildKey});
    my $planKey = join('-', $project, $plan);
    logInfo("Plan key : $planKey");

    logInfo("Requesting deployment projects for plan '$planKey'");
    my $deployProjectsRefs = $self->client->get("/deploy/project/forPlan", { planKey => $planKey });
    return unless defined $deployProjectsRefs;

    # Filtering one that have same name
    logDebug("Filtering returned projects to one with name '$params->{deploymentProjectName}'");
    my ($deploymentProject) = grep {$_->{name} eq $params->{deploymentProjectName}} @$deployProjectsRefs;

    if (!defined $deploymentProject) {
        logWarning("Here are the deployment projects we've got: " . join(', ', map {$_->{name}} @$deployProjectsRefs));
        return $self->setStepResultFields($stepResult, 'error', "Can't find deployment project with name '$params->{deploymentProjectName}'",
            "Can't find deployment project with name '$params->{deploymentProjectName}' for plan '$planKey'"
        );
    }

    my $deploymentProjectId = $deploymentProject->{id};
    logDebug("Deployment project id: $deploymentProjectId");

    if (!$params->{versionName} && $params->{requestVersionName}) {
        logInfo("Requesting next version name for $deploymentProject->{name}");
        my $nextVersionRequest = $self->client->get("/deploy/projectVersioning/$deploymentProjectId/nextVersion", {
            # Can't see any difference with/without this but as we have it, let's specify
            resultKey => $params->{planBuildKey}
        });
        return unless defined $nextVersionRequest;
        $params->{versionName} = $nextVersionRequest->{nextVersionName};
    }

    logInfo("Next version name is '$params->{versionName}'");

    logInfo("Creating version");
    my $createVersionResponse = $self->client->post("/deploy/project/$deploymentProjectId/version", undef,
        # Content
        {
            planResultKey => $params->{planBuildKey},
            name          => $params->{versionName}
        },
        {
            errorHook => {
                400 => sub {return $self->detailedErrorHandler(@_)}
            }
        }
    );
    return unless defined $createVersionResponse;

    logInfo("Created version: $params->{versionName}");
    my $shortInfo = _versionToShortInfo($createVersionResponse);

    $self->saveResultProperties($stepResult, $params->{resultFormat}, $params->{resultPropertySheet}, $shortInfo);
    $stepResult->setOutputParameter('version', $params->{versionName});

    my $summary = "Created version: $params->{versionName}";
    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary($summary);
    $stepResult->setJobSummary($summary);

    return;
}

sub runPlan {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    # Setting default values
    $params->{waitTimeout} ||= 300;
    $params->{resultFormat} ||= 'json';
    $params->{resultPropertySheet} ||= '/myJob/runResult';

    my %queueRequestParams = (
        executeAllStages => 'true'
    );

    # Additional parameters
    if ($params->{additionalBuildVariables}) {
        my $buildParams = parseKeyValuePairs($params->{additionalBuildVariables});
        for (keys %$buildParams) {
            $queueRequestParams{'bamboo.variable.' . $_} = uri_escape_utf8($buildParams->{$_});
        }

    }

    # Custom revision
    $queueRequestParams{customRevision} = $params->{customRevision} if $params->{customRevision};

    my $planKey = "$params->{projectKey}-$params->{planKey}";
    my $queueRequestPath = "/queue/$planKey";

    # Perform request to run the build
    my $queueResponse = $self->client->post($queueRequestPath, \%queueRequestParams, undef, {
        errorHook => {
            # Concurrent limit will cause 400
            400 => sub {
                my (undef, $response, $decoded) = @_;
                return $self->defaultErrorHandler($response, $decoded, $stepResult);
            }
        }
    });
    return unless (defined $queueResponse);

    my $buildNumber = $queueResponse->{buildNumber};
    logInfo("Build Number: $buildNumber.\nBuild Result Key: $queueResponse->{buildResultKey}.");

    my $bambooResultURL = $params->{endpoint}
        . "/chain/result/viewChainResult.action?"
        . "planKey=$planKey&buildNumber=$buildNumber";

    $stepResult->setReportUrl("View Build Report", $bambooResultURL);
    $stepResult->setOutputParameter('buildUrl', $bambooResultURL);
    $stepResult->setOutputParameter('buildResultKey', $queueResponse->{buildResultKey});

    if ($params->{waitForBuild}) {
        my $statusRequestPath = '/result/status/' . $queueResponse->{buildResultKey};

        # Request will return 404 if build is not running
        my $errorHook = { 404 => sub {return { finished => 'true' }} };

        my $waited = 0;
        my $sleepTime = 5;
        my $finished = 0;
        while (!$finished && $waited <= $params->{waitTimeout}) {
            # Request status
            my $status = $self->client->get($statusRequestPath, {}, undef, {
                errorHook => $errorHook
            });

            # Check status (this could be moved to 404 handler, but this way it is clearer)
            if ($status->{finished}) {
                $finished = 1;
                # Updating progress property
                $stepResult->setJobStepSummary("Build is finished. Requesting build result.");
                $stepResult->applyAndFlush();
                logInfo("Build finished");
                last;
            }

            logInfo("Current Stage: '" . ($status->{currentStage} || 'Not available yet') . "'. "
                . "Approximate Completed: $status->{progress}{percentageCompletedPretty}. "
                . "$status->{progress}{prettyTimeRemainingLong}"
            );

            # Updating progress property
            $stepResult->setJobStepSummary("Approximate Completed: $status->{progress}{percentageCompletedPretty}");
            $stepResult->applyAndFlush();

            logInfo("Waiting $sleepTime second(s) before requesting the status again.");
            $waited += $sleepTime;
            sleep $sleepTime;
        }

        if ($finished == 0) {
            return $self->setStepResultFields($stepResult, 'error',
                'Exceeded the wait timeout while waiting for the build to finish.'
            );
        }
    }

    # Get build info
    #/result/{projectKey}-{buildKey}-{buildNumber : ([0-9]+)|(latest)}?expand&favourite&start-index&max-results
    my $buildInfo = $self->client->get("/result/$queueResponse->{buildResultKey}", {
        expand => 'results.result.artifacts,results.result.labels'
    });
    return unless defined $buildInfo;

    my $infoToSave = _planBuildResultToShortInfo($buildInfo, [ 'artifacts', 'labels' ]);

    # Save properties
    $self->saveResultProperties($stepResult, $params->{resultFormat}, $params->{resultPropertySheet}, $infoToSave);

    if (!$params->{waitForBuild}) {
        return $self->setStepResultFields($stepResult, 'success', 'Build was successfully added to a queue.');
    }
    # Failed build
    elsif ($infoToSave->{finished} && !$infoToSave->{successful}) {
        return $self->setStepResultFields($stepResult, 'warning', 'Build was not finished successfully');
    }
    # Build that was not started
    elsif (!$infoToSave->{finished} && $infoToSave->{buildState} eq 'Unknown') {
        return $self->setStepResultFields($stepResult, 'warning', "Build was not started.");
    }

    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobSummary('Completed with Success');
    $stepResult->setJobStepSummary("Completed with Success. Build Result Key: '$infoToSave->{key}'");
    $stepResult->apply();
}

sub triggerDeployment {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    $params->{waitTimeout} ||= 300;
    $params->{resultFormat} ||= 'json';
    $params->{resultPropertySheet} ||= '/myJob/deploymentResult';

    # We need to get ID of:
    # - Deployment project
    # - Environment
    # - Version
    # The only way to get the deployment project is to get all and then filter it by ourselves
    # Environment information is by default included to the project
    # To get version Id should perform additional request
    # * For all available versions (<6.2.5)

    # Get deployment project from the whole list

    logInfo("Requesting deployment projects available for user");
    my $allProjects = $self->client->get('/deploy/project/all');
    return unless defined $allProjects;

    logInfo("Filtering to the one with name '$params->{deploymentProjectName}'");
    my ($project) = grep {$_->{name} eq $params->{deploymentProjectName}} @$allProjects;
    if (!defined $project) {
        logInfo("Here are names of all projects returned by Bamboo: ", (map {$_->{name}} @$allProjects));
        return $self->setStepResultFields($stepResult, 'error', "Can't find deployment project '$params->{deploymentProjectName}'.");
    }
    logInfo("Project key: " . $project->{name});
    logTrace("Project", $project);

    # Get environment
    my ($environment) = grep {$_->{name} eq $params->{deploymentEnvironmentName}} @{$project->{environments}};
    if (!defined $environment) {
        return $self->setStepResultFields($stepResult, 'error', "Can't find environment '$params->{deploymentEnvironmentName}'.");
    }
    logTrace("Environment", $environment);

    # Get version id from the deployment project
    my $allVersions = $self->client->get("/deploy/project/$project->{id}/versions");
    my ($version) = grep {$_->{name} eq $params->{deploymentVersionName}} @{$allVersions->{versions}};
    if (!defined $version) {
        return $self->setStepResultFields($stepResult, 'error', "Can't find version '$params->{deploymentVersionName}'.");
    }
    logTrace("Version", $version);

    # Trigger deployment
    my $triggerDeploymentResponse = $self->client->post('/queue/deployment', {
        environmentId => $environment->{id},
        versionId     => $version->{id}
    });
    return unless defined $triggerDeploymentResponse;

    my $deploymentResultId = $triggerDeploymentResponse->{deploymentResultId};
    logInfo("Deployment Result Key: $deploymentResultId");

    if ($params->{waitForDeployment}) {
        $stepResult->setJobStepSummary("Waiting for the deployment to finish.");
        $stepResult->applyAndFlush();

        my $waited = 0;
        my $sleepTime = 5;
        my $finished = 0;
        while (!$finished && $waited <= $params->{waitTimeout}) {
            # Request status
            my $status = $self->client->get("/deploy/result/$deploymentResultId");
            if ($status->{lifeCycleState} eq 'FINISHED') {
                $finished = 1;
                # Updating progress property
                $stepResult->setJobStepSummary("Deployment is finished. Requesting result.");
                $stepResult->applyAndFlush();
                logInfo("Deployment finished.");
                last;
            }

            logInfo("Waiting $sleepTime second(s) before requesting the status again.");
            $waited += $sleepTime;
            sleep $sleepTime;
        } # while

        if ($finished == 0) {
            return $self->setStepResultFields($stepResult, 'error', 'Exceeded the wait timeout while waiting for the deployment to finish.');
        }
    }

    logInfo("Requesting deployment result");
    my $deploymentResult = $self->client->get("/deploy/result/$deploymentResultId");
    my $shortInfo = _deploymentResultToShortInfo($deploymentResult);

    logInfo("Saving result properties");
    $self->saveResultProperties($stepResult, $params->{resultFormat}, $params->{resultPropertySheet}, $shortInfo);

    my $bambooResultURL = $params->{endpoint}
        . '/deploy/viewDeploymentResult.action?deploymentResultId=' . $deploymentResultId;

    $stepResult->setReportUrl("View Deployment Report", $bambooResultURL);
    $stepResult->setOutputParameter('deploymentResultKey', $deploymentResultId);
    $stepResult->setOutputParameter('deploymentResultUrl', $bambooResultURL);

    if (!$params->{waitForDeployment}) {
        return $self->setStepResultFields($stepResult, 'success', "Deployment was successfully added to a queue.");
    }

    # Failed deployment
    if ($params->{waitForDeployment} && $shortInfo->{deploymentState} ne 'SUCCESS') {
        return $self->setStepResultFields($stepResult, 'warning', "Deployment was not finished successfully.");
    }

    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobSummary("Finished with Success.");
    $stepResult->setJobStepSummary("Finished with Success. Deployment result key: '$shortInfo->{key}'");
    $stepResult->apply();
}

sub enablePlan {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    my $planKey = "$params->{projectKey}-$params->{planKey}";

    logInfo("Setting enable flag for plan $planKey");
    my $result = $self->client->post("/plan/$planKey/enable");
    return if (!defined $result || $result ne '1');

    my $summary = "Build plan '$planKey' was enabled.";

    logInfo($summary);
    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary($summary);
    $stepResult->setJobSummary($summary);
    $stepResult->apply();
}

sub disablePlan {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    my $planKey = "$params->{projectKey}-$params->{planKey}";

    logInfo("Removing enable flag for plan $planKey");
    my $result = $self->client->delete("/plan/$planKey/enable");
    return if (!defined $result || $result ne '1');

    my $summary = "Build plan '$planKey' was disabled.";

    logInfo($summary);
    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary($summary);
    $stepResult->setJobSummary($summary);
    $stepResult->apply();
}

sub checkConnection {
    my ($self, $configValues) = @_;

    my $client = FlowPlugin::REST->new($configValues, {
        APIBase     => '/rest/api/latest/',
        contentType => 'json'
    });

    my $userInfo = $client->get('/currentUser');

    return $userInfo->{name} eq $configValues->getParameter('credential')->getUserName();
}

sub collectReportingData {
    my FlowPlugin::Bamboo $self = shift;
    my $params = shift;
    $self->init($params);

    if ($params->{debugLevel}) {
        FlowPDF::Log::setLogLevel(FlowPDF::Log::DEBUG);
    }

    my $requestKey = $params->{projectKey} . ($params->{planKey} ? '-' . $params->{planKey} : '');

    my $reporting = FlowPDF::ComponentManager->loadComponent('FlowPlugin::Bamboo::Reporting', {
        reportObjectTypes   => [ 'build' ],
        initialRecordsCount => $params->{initialRecordsCount},
        metadataUniqueKey   => $requestKey,
        payloadKeys         => [ 'startTime' ]
    }, $self);

    $reporting->CollectReportingData();
}

sub validateCRDParams {
    my FlowPlugin::Bamboo $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    my @required = qw/config projectKey/;
    for my $param (@required) {
        bailOut("Parameter $params is mandatory") unless $params->{$param};
    }

    $stepResult->setJobSummary('success');
    $stepResult->setJobStepOutcome('Parameters check passed');

    exit 0;
}

# Get Build Runs for the plan of a project
# /result/{projectKey}-{buildKey}?expand&start-index&max-results
sub getBuildRuns {
    my ($self, $projectKey, $planKey, $parameters) = @_;

    # Adding plan key if given
    my $requestKey = $projectKey . ($planKey ? '-' . $planKey : '');
    my $requestPath = '/result/' . $requestKey;

    my $limit = 0;
    if (defined $parameters->{maxResults}) {
        $limit = $parameters->{maxResults};
    }

    my $buildResults = $self->client->get($requestPath, { expand => 'results.result', 'max-results' => $limit });
    return unless defined $buildResults;

    my @result = map {_planBuildResultToShortInfo($_)} @{$buildResults->{results}{result}};

    return \@result;
}

sub getBuildRunsAfter {
    my ($self, $projectKey, $planKey, $parameters) = @_;
    my $afterTime = $parameters->{afterTime};

    my @results = ();

    # Adding plan key if given
    my $requestKey = $projectKey . ($planKey ? '-' . $planKey : '');
    my $requestPath = '/result/' . $requestKey;

    # Will load this count of results at once
    my $requestPackSize = 25;

    my %requestParams = (
        expand        => 'results.result.labels',
        'max-results' => $requestPackSize,
        'start-index' => 0
    );

    my $reachedGivenTime = 0;
    my $haveMoreResults = 1;

    while (!$reachedGivenTime && $haveMoreResults) {
        my $buildResults = $self->client->get($requestPath, \%requestParams);
        return unless defined $buildResults;

        # If returned less results that we requested, than there are no more updates to request
        $haveMoreResults = $buildResults->{results}{size} >= $requestPackSize;

        for my $buildResult (@{$buildResults->{results}{result}}) {
            my $parsed = _planBuildResultToShortInfo($buildResult, [ 'labels' ]);

            if ($self->compareISODateTimes($afterTime, $parsed->{buildStartedTime}) >= 0) {
                $reachedGivenTime = 1;
                last;
            }

            push @results, $parsed;
        }

        # Request next pack
        $requestParams{'start-index'} += $requestPackSize;
    }

    return \@results;
}

# 2019-05-28T11:14:06.894Z
sub compareISODateTimes {
    my ($self, $date1, $date2) = @_;

    $date1 =~ s/[^0-9]//g;
    $date2 =~ s/[^0-9]//g;

    logDebug("Comparing: $date1 > $date2 = ", $date1 <=> $date2);

    return $date1 <=> $date2;
}

sub defaultErrorHandler {
    my FlowPDF $self = shift;
    my ($response, $decoded) = @_;

    logDebug(Dumper \@_);

    if (!$decoded || !$decoded->{message}) {
        $decoded->{message} = 'No specific error message was returned. Check logs for details';
        logError($response->decoded_content || 'No content returned');
    }

    my FlowPDF::StepResult $stepResult = $self->getContext()->newStepResult();
    $stepResult->setJobStepOutcome('error');
    $stepResult->setJobStepSummary($decoded->{message});
    $stepResult->setJobSummary("Error happened while performing the operation: '$decoded->{message}'");
    $stepResult->apply();

    return;
}

sub detailedErrorHandler {
    my ($self, $response, $decoded) = @_;

    my $summaryError = 'Request errors: ';

    if ($decoded->{errors} && ref $decoded->{errors} eq 'ARRAY' && @{$decoded->{errors}}) {
        $summaryError .= join("\n", @{$decoded->{errors}});
    }

    if ($decoded->{fieldErrors} && ref $decoded->{fieldErrors} eq 'HASH') {
        for my $badField (keys %{$decoded->{fieldErrors}}) {
            $summaryError .= "\n $badField : " . join("\n", @{$decoded->{fieldErrors}{$badField}});
        }
    }

    my FlowPDF::StepResult $stepResult = $self->getContext()->newStepResult();
    $stepResult->setJobStepOutcome('warning');
    $stepResult->setJobStepSummary($summaryError);
    $stepResult->setJobSummary("Received error while performing the request.");
    $stepResult->apply();
    return;
}

sub setStepResultFields {
    my ($self, $stepResult, $outcome, $jobStepSummary, $jobSummary) = @_;
    $jobSummary ||= $jobStepSummary;

    logTrace("Finish initiated by:" . join(', ', caller()));

    $stepResult->setJobStepOutcome($outcome);
    $stepResult->setJobStepSummary($jobStepSummary);
    $stepResult->setJobSummary($jobSummary);

    $stepResult->apply();
}

sub _planToShortInfo {
    my ($plan, $expanded) = @_;
    my @oneToOne = qw/
        key
        name
        type

        shortName
        buildName

        averageBuildTimeInSeconds

        projectName
        projectKey

        enabled
        isBuilding
    /;

    my %shortInfo = (
        url             => $plan->{link}{href},
        stagesSize      => $plan->{stages}{size},

        # 'description' is a predefined field in Flow
        planDescription => $plan->{description}
    );

    $shortInfo{$_} = $plan->{$_} for (@oneToOne);

    if (defined $expanded && ref $expanded eq 'ARRAY') {
        for my $section (@$expanded) {

            if ($section eq 'stages' && $plan->{stages}{size} > 0) {
                # Create copy and remove nested keys
                my @stages = @{$plan->{stages}{stage}};
                for my $stage (@stages) {
                    for my $k (keys %$stage) {
                        delete $stage->{$k} if ref $stage->{$k};
                    }
                    # Removing service property
                    delete $stage->{expand};
                }
                # Save cleaned result
                $shortInfo{stages} = \@stages;
                $shortInfo{stageNames} = join(', ', map {"'$_->{name}'"} @stages);
            }

        }
    }

    return \%shortInfo;
}

sub _planBuildResultToShortInfo {
    my ($buildInfo, $expanded) = @_;

    my @oneToOne = qw/
        key
        buildNumber
        buildState
        finished
        successful
        lifeCycleState

        planName
        projectName

        vcsRevisionKey

        buildTestSummary
        successfulTestCount
        failedTestCount
        skippedTestCount
        quarantinedTestCount

        buildStartedTime
        buildCompletedTime
        buildDuration
        buildDurationInSeconds
        buildReason
    /;

    my %result = (
        url     => $buildInfo->{link}{href},
        planKey => $buildInfo->{plan}{key},
    );

    if ($buildInfo->{buildTestSummary} && $buildInfo->{buildTestSummary} ne 'No tests found') {
        push(@oneToOne,
            qw/buildTestSummary
                successfulTestCount
                failedTestCount
                quarantinedTestCount
                skippedTestCount/
        );

        $result{totalTestsCount} = ($buildInfo->{successfulTestCount} || 0)
            + ($buildInfo->{failedTestCount} || 0)
            + ($buildInfo->{quarantinedTestCount} || 0)
            + ($buildInfo->{skippedTestCount} || 0)
    }

    if (defined $expanded && ref $expanded eq 'ARRAY') {
        for my $section (@$expanded) {
            if ($section eq 'labels' && $buildInfo->{labels}{size} > 0) {
                $result{labels} = join(', ', map {$_->{name}} @{$buildInfo->{labels}});
            }
            elsif ($section eq 'artifacts' && $buildInfo->{artifacts}{size}) {
                $result{artifacts} = $buildInfo->{artifacts}{artifact};
            }
        }
    }

    $result{$_} = $buildInfo->{$_} for (@oneToOne);

    return \%result;
}

sub _deploymentProjectToShortInfo {
    my ($deploymentProject) = @_;

    my @oneToOne = qw/
        id
        name
    /;

    my %result = (
        key     => $deploymentProject->{key}{key},
        planKey => $deploymentProject->{planKey}{key},
    );

    $result{$_} = $deploymentProject->{$_} for @oneToOne;

    my @environments = ();
    my @environmentNames = ();
    for my $env (@{$deploymentProject->{environments}}) {
        push(@environmentNames, $env->{name});
        push(@environments, {
            id                  => $env->{id},
            key                 => $env->{key}{key},
            name                => $env->{name},
            deploymentProjectId => $env->{deploymentProjectId},
            position            => $env->{position},
            configurationState  => $env->{configurationState}
        });
    }
    $result{environments} = \@environments;
    $result{environmentNames} = join(', ', @environmentNames);

    return \%result;
}

sub _deploymentResultToShortInfo {
    my ($deploymentResult) = @_;

    my @oneToOne = qw/
        deploymentState
        deploymentVersionName
        lifeCycleState
        id
    /;

    my %result = (
        key       => $deploymentResult->{key}{key},
        agentId   => $deploymentResult->{agent}{id},
        agentName => $deploymentResult->{agent}{name}
    );

    $result{$_} = $deploymentResult->{$_} for @oneToOne;

    return \%result;
}

sub _versionToShortInfo {
    my ($version) = @_;

    my @oneToOne = qw/
        id
        name
        creatorDisplayName
        planBranchName
        creationDate
    /;

    my %result = ();
    $result{$_} = $version->{$_} for @oneToOne;

    return \%result;
}

sub saveResultProperties {
    my ($self, $stepResult, $resultFormat, $resultProperty, $result) = @_;

    if (!defined $resultFormat) {
        bailOut("No result format was supplied to saveResultProperties()");
    }
    elsif ($resultFormat !~ m/^(?:none|propertySheet|json)$/s) {
        bailOut("Wrong Result Property Format provided. Has to be one of 'none', 'propertySheet', 'json'");
    }

    if ($resultFormat eq 'none') {
        logInfo("Will not save the results. 'Do Not Save The Result' was chosen for Result Format.");
        return 0;
    }

    if (!defined $resultProperty || $resultProperty eq '') {
        bailOut("No result property was supplied to saveResultProperties()");
    }

    if ($resultFormat eq 'json') {
        my $encodedResult = JSON::encode_json($result);
        $stepResult->setOutcomeProperty($resultProperty, $encodedResult);
        logInfo("Result was saved to the property '$resultProperty'");
    }
    elsif ($resultFormat eq 'propertySheet') {
        my $properties = transformToProperties($resultProperty, $result, 'key');

        for my $property (keys %$properties) {
            my $value = $properties->{$property};
            next unless (defined $value);
            logDebug("Saving property '$property' with value '$value'");
            $stepResult->setOutcomeProperty($property, $value);
        }
        logInfo("Result was saved under the propertySheet '$resultProperty'");
    }

    $stepResult->apply();

    return 1;
}

sub transformToProperties {
    my ($currentPath, $object, $IdKeyName) = @_;

    $IdKeyName ||= 'key';

    print " $currentPath : $object \n";

    my %result = ();
    my $adopt = sub {
        my ($flattened) = @_;
        for my $f (keys %$flattened) {
            $result{$f} = $flattened->{$f};
        }
    };

    if (!ref $object) {
        $result{$currentPath} = $object;
    }
    # JSON boolean
    if (JSON::is_bool($object)) {
        $result{$currentPath} = (!!$object) ? 'true' : 'false';
    }
    elsif ('ARRAY' eq ref $object && @$object) {
        # Simple scalar array
        if (!ref $object->[0]) {
            $result{$currentPath} = join(", ", @$object);
        }
        # Array of maps with id
        elsif (ref $object->[0] eq 'HASH' && $object->[0]{$IdKeyName}) {
            for my $item (@$object) {
                my $id = $item->{$IdKeyName};
                die "One of the items doesn't have id: " . Dumper $item unless $id;
                $adopt->(transformToProperties("$currentPath/$id", $item));
            }
            $result{"$currentPath/$IdKeyName" . 's'} = join(',', map {$_->{$IdKeyName}} @$object);
        }
        # Array of maps without ids
        elsif (ref $object->[0] eq 'HASH') {
            for (my $i = 0; $i < scalar(@$object); $i++) {
                my $item = $object->[$i];
                for my $key (keys %$item) {
                    $adopt->(transformToProperties($currentPath . "/$i/$key", $item->{$key}));
                }
            }
            $result{$currentPath . "/count"} = scalar(@$object);
        }
        # Array of arrays
        elsif (ref $object->[0] eq 'ARRAY') {
            for (my $i = 0; $i < scalar(@$object); $i++) {
                $adopt->(transformToProperties($currentPath . "/$i", $object->[$i]));
            }
            $result{$currentPath . "/count"} = scalar(@$object);
        }

    }
    elsif ('HASH' eq ref $object) {
        for (keys %$object) {
            $adopt->(transformToProperties($currentPath . "/$_", $object->{$_}));
        }
    }

    return \%result;
}

sub parseKeyValuePairs {
    my ($rawAttributes) = @_;

    my %pairs = ();

    # Parse given attributes
    eval {
        # Splitting by both '\n' and ';#;#;#' (Flow UI will send \\\n) - CEV-21967
        my @attributes = split(/\n|(?:;#){3}/, $rawAttributes);
        foreach my $attributePair (@attributes) {
            my ($name, $value) = split('=', $attributePair, 2);
            $pairs{$name} = $value;
        }
        1;
    };
    if (defined $@ && $@ ne '') {
        logError("Failed to parse custom attributes : $@");
        return 0;
    };

    return \%pairs;
}

1;