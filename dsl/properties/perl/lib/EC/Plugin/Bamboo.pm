package EC::Plugin::Bamboo;
use strict;
use warnings;
use base qw/ECPDF/;
use Data::Dumper;
# Feel free to use new libraries here, e.g. use File::Temp;

use ECPDF::Log;
use ECPDF::Helpers qw/bailOut/;
use JSON qw/decode_json/;

# Service function that is being used to set some metadata for a plugin.
sub pluginInfo {
    $ECPDF::Log::LOG_TO_PROPERTY = '/myJob/debug_logs';

    return {
        pluginName      => '@PLUGIN_KEY@',
        pluginVersion   => '@PLUGIN_VERSION@',
        configFields    => [ 'config' ],
        configLocations => [ 'ec_plugin_cfgs' ]
    };
}

sub init {
    my ($self, $params) = @_;

    my ECPDF::Context $context = $self->getContext();
    my $config_values = $context->getConfigValues($params->{config});

    # Will add
    $self->{_config} = $config_values;

    $self->{restClient} = $context->newRESTClient();
    $self->{restAPIBase} = '/rest/api/latest/';
    $self->{restEncode} = \&JSON::encode_json;
    $self->{restDecode} = sub {
        my ($response_content) = @_;
        my $result = eval {JSON::decode_json($response_content)};
        if ($@) {
            $self->exit_with_error("Error while decoding JSON: $@. Got: $response_content");
        }
        return $result;
    };

    $self->{restHeaders} = { 'Accept' => 'application/json' };

}

sub config {return shift->{_config}};

sub exit_with_error {
    my ($self, $error) = @_;
    bailOut($error);
}

sub REST_newRequest {
    my ($self, $method, $path, $query_params, $content, $params) = @_;

    my $config = $self->config();

    my ECPDF::Client::REST $rest = $self->{restClient};

    my $path_base = $config->getRequiredParameter('endpoint')->getValue();
    if ($self->{restAPIBase}) {
        $path_base .= $self->{restAPIBase}
    }
    $path_base =~ s|/$||;

    if (!$path =~ /^\//) {
        $path = '/' . $path;
    }

    my HTTP::Request $request = $rest->newRequest($method, $path_base . $path);
    $request->authorization_basic(
        $config->getRequiredParameter('credential')->getUserName(),
        $config->getRequiredParameter('credential')->getSecretValue()
    );

    if ($query_params) {
        $request->uri->query_form(%$query_params);
    }

    if ($content && $self->{restEncode}) {
        my $encoded = &{$self->{restEncode}}($content);
        $request->content($encoded);
    }

    if ($self->{restHeaders}) {
        while (my ($name, $value) = each %{$self->{restHeaders}}) {
            $request->header($name, $value);
        }
    }

    if ($self->{restRequestHook}) {
        eval {
            $request = &{$self->{restRequestHook}}($request);
            1;
        } or do {
            $self->exit_with_error("Request hook failed: $@") unless ref $request;
        };
    }

    return $request;
}

sub REST_doRequest {
    my ($self, $path, $query_params, $content) = @_;

    my ECPDF::Client::REST $rest = $self->{restClient};

    my HTTP::Request $request = $self->REST_newRequest('GET', $path, $query_params, $content);
    logTrace("Request", $request);

    my HTTP::Response $response = $rest->doRequest($request);
    logTrace("Response", $response);

    if (!$response->is_success) {
        logDebug("Requested " . $request->uri);
        $self->exit_with_error("Error while performing request. " . $response->status_line);
    }

    my $result = $response->decoded_content();
    if ($self->{restDecode}) {
        $result = &{$self->{restDecode}}($result);
    }

    return $result;
}


=head2 getAllPlans

=cut
sub getAllPlans {
    my ECPDF $self = shift;
    my $params = shift;
    my ECPDF::StepResult $stepResult = shift;
    $self->init($params);

    # Setting default parameters
    $params->{resultPropertySheet} ||= '/myJob/plans';

    # Requesting and formatting information
    my @infoToSave = ();
    if (!$params->{projectKey}) {
        my $response = $self->REST_doRequest('/project', { expand => 'projects.project.plans.plan' });
        for my $project (@{$response->{projects}{project}}) {
            logInfo("Found project: '$project->{key}'");

            for my $plan (@{$project->{plans}{plan}}) {
                logInfo("Found plan: '$plan->{key}'");
                push(@infoToSave, planToShortInfo($plan));
            }
        }
    }
    else {
        my $response = $self->REST_doRequest('/project/' . $params->{projectKey}, { expand => 'plans.plan' });
        logInfo("Found project: '$response->{key}'");
        for my $plan (@{$response->{plans}{plan}}) {
            logInfo("Found plan: '$plan->{key}'");
            push(@infoToSave, planToShortInfo($plan));
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

    logInfo()
    $stepResult->setJobStepOutcome('success');
    $stepResult->setJobStepSummary('Plans found: ' . $planKeysStr);
    $stepResult->setJobSummary("Found " . scalar(@infoToSave) . ' plan(s).');

    $stepResult->apply();
}

# Auto-generated method for the procedure GetPlanDetails/GetPlanDetails
# Add your code into this method and it will be called when step runs
sub getPlanDetails {
    my ($pluginObject) = @_;
    my $context = $pluginObject->newContext();
    print "Current context is: ", $context->getRunContext(), "\n";
    my $params = $context->getStepParameters();
    print Dumper $params;

    my $configValues = $context->getConfigValues();
    print Dumper $configValues;

    my $stepResult = $context->newStepResult();
    print "Created stepresult\n";
    $stepResult->setJobStepOutcome('warning');
    print "Set stepResult\n";

    $stepResult->setJobSummary("See, this is a whole job summary");
    $stepResult->setJobStepSummary('And this is a job step summary');

    $stepResult->apply();
}
# Auto-generated method for the procedure GetPlanRuns/GetPlanRuns
# Add your code into this method and it will be called when step runs
sub getPlanRuns {
    my ($pluginObject) = @_;
    my $context = $pluginObject->newContext();
    print "Current context is: ", $context->getRunContext(), "\n";
    my $params = $context->getStepParameters();
    print Dumper $params;

    my $configValues = $context->getConfigValues();
    print Dumper $configValues;

    my $stepResult = $context->newStepResult();
    print "Created stepresult\n";
    $stepResult->setJobStepOutcome('warning');
    print "Set stepResult\n";

    $stepResult->setJobSummary("See, this is a whole job summary");
    $stepResult->setJobStepSummary('And this is a job step summary');

    $stepResult->apply();
}
# Auto-generated method for the procedure RunPlan/RunPlan
# Add your code into this method and it will be called when step runs
sub runPlan {
    my ($pluginObject) = @_;
    my $context = $pluginObject->newContext();
    print "Current context is: ", $context->getRunContext(), "\n";
    my $params = $context->getStepParameters();
    print Dumper $params;

    my $configValues = $context->getConfigValues();
    print Dumper $configValues;

    my $stepResult = $context->newStepResult();
    print "Created stepresult\n";
    $stepResult->setJobStepOutcome('warning');
    print "Set stepResult\n";

    $stepResult->setJobSummary("See, this is a whole job summary");
    $stepResult->setJobStepSummary('And this is a job step summary');

    $stepResult->apply();
}
# Auto-generated method for the procedure EnablePlan/EnablePlan
# Add your code into this method and it will be called when step runs
sub enablePlan {
    my ($pluginObject) = @_;
    my $context = $pluginObject->newContext();
    print "Current context is: ", $context->getRunContext(), "\n";
    my $params = $context->getStepParameters();
    print Dumper $params;

    my $configValues = $context->getConfigValues();
    print Dumper $configValues;

    my $stepResult = $context->newStepResult();
    print "Created stepresult\n";
    $stepResult->setJobStepOutcome('warning');
    print "Set stepResult\n";

    $stepResult->setJobSummary("See, this is a whole job summary");
    $stepResult->setJobStepSummary('And this is a job step summary');

    $stepResult->apply();
}
# Auto-generated method for the procedure DisablePlan/DisablePlan
# Add your code into this method and it will be called when step runs
sub disablePlan {
    my ($pluginObject) = @_;
    my $context = $pluginObject->newContext();
    print "Current context is: ", $context->getRunContext(), "\n";
    my $params = $context->getStepParameters();
    print Dumper $params;

    my $configValues = $context->getConfigValues();
    print Dumper $configValues;

    my $stepResult = $context->newStepResult();
    print "Created stepresult\n";
    $stepResult->setJobStepOutcome('warning');
    print "Set stepResult\n";

    $stepResult->setJobSummary("See, this is a whole job summary");
    $stepResult->setJobStepSummary('And this is a job step summary');

    $stepResult->apply();
}
# Auto-generated method for the procedure TriggerDeployment/TriggerDeployment
# Add your code into this method and it will be called when step runs
sub triggerDeployment {
    my ($pluginObject) = @_;
    my $context = $pluginObject->newContext();
    print "Current context is: ", $context->getRunContext(), "\n";
    my $params = $context->getStepParameters();
    print Dumper $params;

    my $configValues = $context->getConfigValues();
    print Dumper $configValues;

    my $stepResult = $context->newStepResult();
    print "Created stepresult\n";
    $stepResult->setJobStepOutcome('warning');
    print "Set stepResult\n";

    $stepResult->setJobSummary("See, this is a whole job summary");
    $stepResult->setJobStepSummary('And this is a job step summary');

    $stepResult->apply();
}
# Auto-generated method for the procedure GetDeploymentProjectsForPlan/GetDeploymentProjectsForPlan
# Add your code into this method and it will be called when step runs
sub getDeploymentProjectsForPlan {
    my ($pluginObject) = @_;
    my $context = $pluginObject->newContext();
    print "Current context is: ", $context->getRunContext(), "\n";
    my $params = $context->getStepParameters();
    print Dumper $params;

    my $configValues = $context->getConfigValues();
    print Dumper $configValues;

    my $stepResult = $context->newStepResult();
    print "Created stepresult\n";
    $stepResult->setJobStepOutcome('warning');
    print "Set stepResult\n";

    $stepResult->setJobSummary("See, this is a whole job summary");
    $stepResult->setJobStepSummary('And this is a job step summary');

    $stepResult->apply();
}
## === step ends ===

sub planToShortInfo {
    my ($plan) = @_;
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

    return \%shortInfo;
}

sub saveResultProperties {
    my ($self, $stepResult, $resultFormat, $resultProperty, $result, $transformSub) = @_;

    if ($resultFormat eq 'none') {
        logInfo("Will not save the results. 'Do Not Save The Result' was chosen for Result Format.");
        return;
    }

    if (!$resultProperty) {
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
            logDebug("Saving property '$property' with value '$value'");
            $stepResult->setOutcomeProperty($property, $properties->{$property});
        }
    }

    $stepResult->apply();

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

1;