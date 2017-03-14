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
    my $plan = $bamboo->get_plan($params->{project_key}, $params->{plan_key});
    my $result = $bamboo->get_plan_statistics($params->{project_key}, $params->{plan_key});
    my $stat = $bamboo->print_stats($result);
    # Successful, Failed or Unknown
    my $details = "$params->{project_key}-$params->{plan_key} runs: ";
    $details .= "Successful: $stat->{Successful}, Failed: $stat->{Failed}, Unknown: $stat->{Unknown}.";
    $bamboo->success($details);
    $bamboo->flow_result($details);
}

