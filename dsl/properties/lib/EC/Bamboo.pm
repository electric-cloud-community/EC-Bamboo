package EC::Bamboo;
use strict;
use warnings;
use base 'EC::Plugin::Core';
use Data::Dumper;
use JSON;


sub after_init_hook {
    my ($self, %params) = @_;

    $self->{plugin_name} = 'EC-Bamboo';
    $self->{_credentials} = {};
    my $dryrun = 0;
}

sub get_config {
    my ($self, $config_name) = @_;

    my $cred = $self->SUPER::get_credentials(
        $config_name => {
            userName => 'user',
            password => 'password',
            bamboo_server_url => 'bamboo_server_url'
        },
        'ec_plugin_cfgs');

    $self->{_credentials} = $cred;
    return $cred;
}

sub get_rest_client {
    my ($self) = @_;

    return $self->{_ua} if $self->{_ua};
    my $cnf = $self->get_config();
    $self->{_ua} = EC::Plugin::MicroRest->new(
        auth => 'basic',
        url => $cnf->{bamboo_server_url},
        user => $cnf->{user},
        password => $cnf->{password},
        content_type => 'application/json'
    );
    $self->{_ua}->ignore_errors(1);
    return $self->{_ua};
}


sub get_plan {
    my ($self, $project_key, $plan_key, $expand) = @_;

    my $rc = $self->get_rest_client();
    if ($expand) {
        $expand = "?expand=$expand";
    }
    $expand ||= '';
    # my $retval = $rc->get('/rest/api/latest/plan/' . $project_key . '-' . $plan_key  . '?expand=stages.stage');
    my $retval = $rc->get('/rest/api/latest/plan/' . $project_key . '-' . $plan_key  . $expand);
    if ($retval->{"status-code"} && $retval->{"status-code"} > 399) {
        $retval->{message} ||= '';
        $self->bail_out($retval->{message});
        exit 1;
    }
    return $retval;
}

sub run_plan {
    my ($self, $project_key, $plan_key) = @_;

    my $url = sprintf '/rest/api/latest/queue/%s-%s?stage&executeAllStages',
        $project_key, $plan_key;

    my $retval = $self->get_rest_client->post($url);
    return $retval;
}

sub get_result {
    my ($self, $runplan) = @_;

    if (!$runplan->{link}->{href}) {
        print "Error occured\n";
        exit 1;
    }
    my $plan = $1 if $runplan->{link}->{href} =~ m|/result/(.*?)$|s;
    my $retval = $self->get_rest_client()->get('/rest/api/latest/result/' . $plan);
    $self->save_properties($retval);
    return $retval;
}

sub print_stats {
    my ($self, $plan) = @_;

    my $retval = {
        'Failed' => 0,
        'Successful' => 0,
        'Unknown' => 0,
    };

    print "Plan link: $plan->{link}->{href}\n";
    for my $p (@{$plan->{results}->{result}}) {
        print '=' x 60 . "\n";
        print "Number: $p->{number}\n";
        print "Run link: $p->{link}->{href}\n";
        print "State: $p->{state}\n";
        print "Build State: $p->{buildState}\n";
        print '=' x 60 . "\n";
        $retval->{$p->{buildState}} += 1;
    }
    return $retval;
}


sub get_plan_statistics {
    my ($self, $project_key, $plan_key) = @_;

    my $retval = $self->get_rest_client()->get("/rest/api/latest/result/$project_key-$plan_key");
    $self->save_properties($retval);
    return $retval;
}


sub get_all_plans {
    my ($self) = @_;

    my $retval = $self->get_rest_client()->get("/rest/api/latest/plan?expand=plans.plan");
    $self->save_properties($retval);
    return $retval;
}


sub enable_plan {
    my ($self, $project_key, $plan_key) = @_;

    my $retval = $self->get_rest_client()->post("/rest/api/latest/plan/$project_key-$plan_key/enable");
    return $retval;
}


sub disable_plan {
    my ($self, $project_key, $plan_key) = @_;

    my $retval = $self->get_rest_client()->delete("/rest/api/latest/plan/$project_key-$plan_key/enable");
    return $retval;
}


sub save_properties {
    my ($self, $params) = @_;

    $self->set_property(response => encode_json($params));
    return 1;
}

sub flow_result {
    my ($self, $success_response) = @_;

    my $is_flow_runtime = 0;
    eval {
        my $prop = $self->ec()->getProperty('/myPipelineStageRuntime/ec_summary');
        $is_flow_runtime = 1;
    };

    return unless $is_flow_runtime;

    $self->ec()->setProperty('/myPipelineStageRuntime/ec_summary/Bamboo Result:', $success_response);
    return 1;
}


1;
