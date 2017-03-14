$[/myProject/scripts/preamble]
use Data::Dumper;

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

main();

sub main {
    my $bamboo = EC::Bamboo->new(
        project_name => $PROJECT_NAME,
        plugin_name => $PLUGIN_NAME,
        plugin_key => $PLUGIN_KEY
    );

    my $params = $bamboo->get_params_as_hashref(
        'project_key',
        'plan_key',
        'config'
    );

    # bsu = bamboo server url.
    my $bsu = $bamboo->get_config($params->{config})->{bamboo_server_url};
    my $config = $bamboo->get_config($params->{config});
    my $expand = 'stages.stage';
    my $plan = $bamboo->get_plan($params->{project_key}, $params->{plan_key}, $expand);
    print "Project name: $plan->{projectName}\n";
    print "Project key: $plan->{projectKey}\n";
    print "Plan name: $plan->{name}\n";
    print "Plan type: $plan->{type}\n";
    print "Build name: $plan->{buildName}\n";
    print "Plan key: $plan->{key}\n";
    print "Project link: $plan->{project}->{link}->{href}\n";
    print "Plan link: $plan->{link}->{href}\n";
    print "Enabled?: $plan->{enabled}\n";
    print "Stages:\n";
    for my $p (@{$plan->{stages}->{stage}}) {
        print " - Name: $p->{name}\n";
    }

}

