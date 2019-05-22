package FlowPlugin::Bamboo;
use strict;
use warnings;
use base qw/FlowPDF/;
use Data::Dumper;
# Feel free to use new libraries here, e.g. use File::Temp;

use FlowPDF::Log;
use FlowPDF::Helpers qw/bailOut/;
use FlowPlugin::REST;

use JSON qw/decode_json/;

# Service function that is being used to set some metadata for a plugin.
sub pluginInfo {
    FlowPDF::Log::setLogToProperty('/myJob/debug_logs');

    return {
        pluginName      => '@PLUGIN_KEY@',
        pluginVersion   => '@PLUGIN_VERSION@',
        configFields    => [ 'config' ],
        configLocations => [ 'ec_plugin_cfgs' ]
    };
}

sub init {
    my ($self, $params) = @_;

    my FlowPDF::Context $context = $self->getContext();
    my $configValues = $context->getConfigValues($params->{config});

    # Will add
    $self->{_config} = $configValues;

    $self->{restClient} = FlowPlugin::REST->new($configValues, {
        APIBase     => '/rest/api/latest/',
        contentType => 'json',
        errorHook   => {
            default => sub {
                return $self->defaultErrorHandler(@_)
            }
        }
    });
}

sub config {return shift->{_config}};

#@returns FlowPlugin::REST
sub client {return shift->{restClient}};

sub exit_with_error {
    my ($self, $error) = @_;
    bailOut($error);
}

=head2 getAllPlans

=cut
sub getAllPlans {
    my FlowPDF $self = shift;
    my $params = shift;
    my FlowPDF::StepResult $stepResult = shift;
    $self->init($params);

    # Setting default parameters
    $params->{resultPropertySheet} ||= '/myJob/plans';

    # Requesting and formatting information
    my @infoToSave = ();
    if (!$params->{projectKey}) {
        my $response = $self->client->get('/project', { expand => 'projects.project.plans.plan' });
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

    logInfo("Plan(s) information was saved to properties.");

    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary('Plans found: ' . $planKeysStr);
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
    $params->{resultPropertySheet} ||= '/myJob/plan';

    my $planKey = "$params->{projectKey}-$params->{planKey}";

    my $response = $self->client->get("/plan/$planKey", { expand => 'stages.stage' }, undef, {
        errorHook => {
            404 => sub {
                $stepResult->setJobStepOutcome('warning');
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

    logInfo("Plan(s) information was saved to properties.");

    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary('Plans found: ' . $infoToSave->{key});
    $stepResult->setJobSummary("Info about $planKey was saved to property(ies)");

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
    $params->{resultPropertySheet} ||= '/myJob/plans';

    my $planKey = "$params->{projectKey}-$params->{planKey}";
    my %requestParameters = (
        expand => 'results.result',
    );

    if (defined $params->{buildState} && $params->{buildState} ne 'All'){
        $requestParameters{buildstate} = $params->{buildState};
    }

    # Requesting and formatting information
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
        push(@infoToSave, _planBuildResultToShortInfo($buildResult));
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
    logInfo("Plan run(s) information was saved to properties.");

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

sub runPlan {
    my ($self, $params, $stepResult) = @_;
    $self->init($params);

    # Setting default values
    $params->{waitTimeout} ||= 300;
    $params->{resultFormat} ||= 'json';
    $params->{resultPropertySheet} ||= '/myJob/runResult';

    my %queueRequestParams = (
        executeAllStages => 'true'
    );

    # Additional parameter
    if ($params->{additionalBuildVariables}) {
        my $buildParams = parseKeyValuePairs($params->{additionalBuildVariables});
        $queueRequestParams{$_} = $buildParams->{$_} for (keys %$buildParams);
    }

    # Custom revision
    $queueRequestParams{customRevision} = $params->{customRevision} if $params->{customRevision};

    my $queueRequestPath = "/queue/$params->{projectKey}-$params->{planKey}";

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
    if (!defined $queueResponse) {
        logDebug("Empty queueResponse. Assuming it was handled by errorHook");
    }

    my $buildNumber = $queueResponse->{buildNumber};
    logInfo("Build Number: $buildNumber.\nBuild Result Key: $queueResponse->{buildResultKey}.");

    # TODO: set Flow URL property

    $stepResult->setOutputParameter('buildUrl', $queueResponse->{link}{href});
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
            $stepResult->setJobStepOutcome('warning');
            $stepResult->setJobStepSummary('Exceeded the wait timeout while waiting for the build to finish.');
            $stepResult->setJobSummary('Exceeded the wait timeout while waiting for the build to finish.');
            return;
        }
    }

    # Get build info
    #/result/{projectKey}-{buildKey}-{buildNumber : ([0-9]+)|(latest)}?expand&favourite&start-index&max-results
    my $buildInfo = $self->client->get("/result/$queueResponse->{buildResultKey}");
    my $infoToSave = _planBuildResultToShortInfo($buildInfo);

    # Save properties
    $self->saveResultProperties($stepResult, $params->{resultFormat}, $params->{resultPropertySheet}, $infoToSave);

    if (!$params->{waitForBuild}) {
        $stepResult->setJobStepOutcome('success');
        $stepResult->setJobSummary("Build was successfully added to a queue.");
        $stepResult->setJobStepSummary('Build was successfully added to a queue.');
        return;
    }
    # Failed build
    elsif ($infoToSave->{finished} && !$infoToSave->{successful}) {
        $stepResult->setJobStepOutcome('warning');
        $stepResult->setJobSummary("Build was not finished successfully");
        $stepResult->setJobStepSummary('Build was not finished successfully');
        return;
    }
    # Build that was not started
    elsif (!$infoToSave->{finished} && $infoToSave->{buildState} eq 'Unknown') {
        $stepResult->setJobStepOutcome('warning');
        $stepResult->setJobSummary("Build was not started.");
        $stepResult->setJobStepSummary('Build was not started (probably because of compilation errors)');
        return;
    }

    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobSummary("Build result information saved to the properties");
    $stepResult->setJobStepSummary('Build result information saved to the properties');
    $stepResult->apply();
}

sub defaultErrorHandler {
    my FlowPDF $self = shift;
    my ($response, $decoded) = @_;

    logDebug(Dumper \@_);

    if (!$decoded || !$decoded->{message}) {
        $decoded->{message} = 'No specific error message was returned. Check logs for details';
        logError($response->decoded_content || 'No content returned');
    }

    my $stepResult = $self->getContext()->newStepResult();
    $stepResult->setJobStepOutcome('error');
    $stepResult->setJobStepSummary($decoded->{message});
    $stepResult->setJobSummary("Error happened while performing the operation: '$decoded->{message}'");
    $stepResult->apply();

    return;
}

sub _planToShortInfo {
    my ($plan, $expanded) = @_;
    my @oneToOne = qw/
        key
        name
        type

        shortName
        buildName
        description

        averageBuildTimeInSeconds

        projectName
        projectKey

        enabled
        isBuilding
    /;

    my %shortInfo = ();
    for (@oneToOne) {
        $shortInfo{$_} = $plan->{$_};
    }

    $shortInfo{url} = $plan->{link}{href};
    $shortInfo{stagesSize} = $plan->{stages}{size};

    if (defined $expanded && ref $expanded eq 'ARRAY') {
        for my $section (@$expanded) {

            if ($section eq 'stages' && $plan->{stages}{size} > 0) {
                # Create copy and remove nested keys
                my @stages = @{$plan->{stages}{stage}};
                for my $stage (@stages) {
                    for my $k (keys %$stage) {
                        delete $stage->{$k} if ref $stage->{$k};
                    }
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
    my ($buildInfo) = @_;

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
    /;

    my %result = (
        url     => $buildInfo->{link}{href},
        planKey => $buildInfo->{plan}{key},
    );

    if ($buildInfo->{artifacts}{size}) {
        $result{artifacts} = $buildInfo->{artifacts}{artifact};
    }

    for (@oneToOne) {
        $result{$_} = $buildInfo->{$_};
    }

    return \%result;
}

sub saveResultProperties {
    my ($self, $stepResult, $resultFormat, $resultProperty, $result) = @_;

    if ($resultFormat eq 'none') {
        logInfo("Will not save the results. 'Do Not Save The Result' was chosen for Result Format.");
        return;
    }

    if (!defined $resultProperty || $resultProperty eq '') {
        bailOut("No result property was supplied to saveResultProperties()");
    }

    if ($resultFormat eq 'json') {
        my $encodedResult = JSON::encode_json($result);
        $stepResult->setOutcomeProperty($resultProperty, $encodedResult);
        return;
    }

    if ($resultFormat eq 'propertySheet') {
        my $properties = transformToProperties($resultProperty, $result, 'key');

        for my $property (keys %$properties) {
            my $value = $properties->{$property};
            next if (!$value && $value ne '0');

            logDebug("Saving property '$property' with value '$value'");
            $stepResult->setOutcomeProperty($property, $value);
        }
    }

    return;
}

sub transformToProperties {
    my ($currentPath, $object, $IdKeyName) = @_;

    $IdKeyName ||= 'key';

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
            for my $item (@$object) {
                for my $key (keys %$item) {
                    $adopt->(transformToProperties($currentPath . "/$key", $item->{$key}));
                }
            }
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