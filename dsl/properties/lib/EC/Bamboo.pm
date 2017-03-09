package EC::Bamboo;
use strict;
use warnings;
use base 'EC::Plugin::Core';
use Data::Dumper;
# use EC::Plugin::MicroRest;

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
    return $self->{_ua};
}


sub get_plan {
    my ($self, $project_key, $plan_key) = @_;

    my $rc = $self->get_rest_client();
    my $retval = $rc->get('/rest/api/latest/plan?os_authType=basic');
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
    return $self->get_rest_client()->get('/rest/api/latest/result/' . $plan);
}

1;
