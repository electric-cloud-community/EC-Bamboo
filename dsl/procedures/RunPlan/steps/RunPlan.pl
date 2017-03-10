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
    my $plan = $bamboo->get_plan($params->{project_key}, $params->{plan_key});
    my $run = $bamboo->run_plan($params->{project_key}, $params->{plan_key});
    my $reached_terminal_state = 0;
    for (my $i = 0; $i < 10; $i++) {
        my $result = $bamboo->get_result($run);
        if ($result->{state} =~ m/^(?:queued|unknown)$/is) {
            print "Current build state is: $result->{state}. Waiting...\n";
        }
        # if ($result->{state} =~ m/^(Successful|Failed)$/is) {
        if ($result->{lifeCycleState} eq 'Finished') {
            print "Reached build terminal state $result->{state}:\n";
            print '=' x 60 . "\n";
            print "Started at: $result->{prettyBuildStartedTime}\n";
            print "Finished at: $result->{prettyBuildCompletedTime}\n";
            print "BuildStatus: $result->{state}\n";
            print "Build Result Number: $result->{planResultKey}->{key}\n";
            print "Duration: $result->{buildDurationDescription}\n";
            print "API Link: $result->{link}->{href}\n";
            print '=' x 60;

            my $msg = qq|Go to <a href="$result->{link}->{href}" target="_BLANK">$bsu/$result->{planResultKey}->{key}</a>.|;
            if ($result->{state} eq 'Failed') {
                $bamboo->error('<html><span>   Failed. </span>' . $msg . '</html>');
                exit 1;
            }
            $bamboo->success("<html><span>   Success. </span>" . $msg . '</html>');
            exit 0;
        }
        sleep 5;
    }

    if (!$reached_terminal_state) {
        exit 1;
    }
}

