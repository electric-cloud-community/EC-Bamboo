$[/myProject/scripts/preamble]

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
    $bamboo->enable_plan($params->{project_key}, $params->{plan_key});
    print "Build plan $params->{project_key}-$params->{plan_key} has been enabled\n";
}

