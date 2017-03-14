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
        'config'
    );

    # bsu = bamboo server url.
    my $bsu = $bamboo->get_config($params->{config})->{bamboo_server_url};
    my $config = $bamboo->get_config($params->{config});
    my $expand = 'stages.stage';

    my $plans = $bamboo->get_all_plans();
    for my $p (@{$plans->{plans}->{plan}}) {
        print '=' x 60 . "\n";
        print "Build name: $p->{buildName}\n";
        print "Project name: $p->{projectName}\n";
        print "Project key: $p->{projectKey}\n";
        print "Key: $p->{key}\n";
        print "Short key: $p->{shortKey}\n";
        print "Short name: $p->{shortName}\n";
        print "Is enabled?: $p->{enabled}\n";
        print "Link: $p->{link}->{href}\n";
        print '=' x 60 . "\n";
    }
}

