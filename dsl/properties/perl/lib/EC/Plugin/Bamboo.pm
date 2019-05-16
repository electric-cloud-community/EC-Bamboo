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
    $self->{restEncode} = sub {
        my ($request_content) = @_;
        return JSON::encode_json($request_content);
    };
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
    my HTTP::Response $response = $rest->doRequest($request);

    if (!$response->is_success) {
        logDebug("Was requesting: " . $request->uri);
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
    my ($params, $stepResult) = @_;
    $self->init($params);

    # Perform request

    my $json = $self->REST_doRequest('/project', { expand => 'projects.project.plans.plan' });
    logTrace("Result", $json);

    # Save to a properties



    # Print short info

    $stepResult->setJobStepOutcome('warning');
    $stepResult->setJobSummary("See, this is a whole job summary");
    $stepResult->setJobStepSummary('And this is a job step summary');

    $stepResult->apply();
}

=head2 getPlanDetails

=cut
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

=head2 getPlanRuns

=cut
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

=head2 runPlan

=cut
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

=head2 enablePlan

=cut
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

=head2 disablePlan

=cut
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

=head2 triggerDeployment

=cut
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

=head2 getDeploymentProjectsForPlan

=cut
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
# Please do not remove the marker above, it is used to place new procedures into this file.

1;