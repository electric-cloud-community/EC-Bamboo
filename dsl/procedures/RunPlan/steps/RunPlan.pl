$[/myProject/scripts/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';
use Data::Dumper;

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

    my $config = $bamboo->get_config($params->{config});
    my $plan = $bamboo->get_plan($params->{project_key}, $params->{plan_key});
    my $run = $bamboo->run_plan($params->{project_key}, $params->{plan_key});
    my $result = $bamboo->get_result($run);
    print Dumper $result;
}

