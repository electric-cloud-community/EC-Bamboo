package FlowPlugin::Bamboo::Reporting;
use strict;
use warnings FATAL => 'all';

use base qw/FlowPDF::Component::EF::Reporting/;
use FlowPDF::Log;
use Data::Dumper;


sub buildDataset {
    my FlowPlugin::Bamboo::Reporting $self = shift;
    my FlowPlugin::Bamboo $bamboo = shift;
    my ($records) = @_;

    my FlowPDF::Component::EF::Reporting::Dataset $dataset = $self->newDataset([ 'build' ]);
    my FlowPDF::Context $context = $bamboo->getContext();
    my $params = $context->getRuntimeParameters();

    # Adding from the end of the list
    for my $row ( reverse @$records ) {

        my %payload = (
            source              => 'Bamboo',
            pluginName          => '@PLUGIN_NAME@',
            projectName         => $context->retrieveCurrentProjectName(),
            releaseName         => $params->{releaseName},
            releaseUri          => _buildRunBuildURL($params, $row),
            releaseProjectName  => $params->{releaseProjectName},
            pluginConfiguration => $params->{config},
            baseDrilldownUrl    => $params->{baseDrilldownUrl} || $params->{endpoint},
            buildNumber         => $row->{buildNumber},
            timestamp           => $row->{buildStartedTime},
            endTime             => $row->{buildCompletedTime},
            startTime           => $row->{buildStartedTime},
            buildStatus         => $row->{buildState},
            launchedBy          => 'N/A',
            jobName             => $row->{key},
            duration            => $row->{buildDurationInSeconds},
            tags                => $row->{labels} || '',
            sourceUrl           => $row->{url},
        );

        $dataset->newData({
            reportObjectType => 'build',
            values           => \%payload
        });

        # if ($params->{retrieveTestResults}) {
        #     logInfo("Test results retrieval is enabled");
        #     my $testReport = $bamboo->getTestReport(
        #         $params->{jobName},
        #         $row->{number},
        #         $params->{testReportUrl}
        #     );
        #     if (%$testReport) {
        #         logInfo("Got testreport for build number $row->{number}, creating new dependent data");
        #         my $dependentData = $data->createNewDependentData('quality');
        #
        #         logDebug("Test Report: ", $testReport);
        #         $dependentData->addValue(projectName => $row->{projectName});
        #         $dependentData->addValue(releaseName => $row->{releaseName});
        #         $dependentData->addValue(releaseProjectName => $row->{releaseProjectName});
        #         $dependentData->addValue(skippedTests => $testReport->{skipCount} || 0);
        #         $dependentData->addValue(successfulTests => $testReport->{passCount} || 0);
        #         $dependentData->addValue(failedTests => $testReport->{failCount} || 0);
        #         $dependentData->addValue(timestamp => $row->{endTime});
        #         $dependentData->addValue(category => $params->{testCategory});
        #         $dependentData->addValue(
        #             totalTests => $testReport->{skipCount} + $testReport->{skipCount} + $testReport->{skipCount}
        #         );
        #     }
        # }
        # my $dataRef = $dataset->getData();
        # unshift @$dataRef, $data;
    }
    return $dataset;
}

sub _buildRunBuildURL {
    my ($params, $buildInfo) = @_;
    my $drilldownURL = $params->{baseDrilldownUrl};
    $drilldownURL ||= $params->{endpoint};

    $drilldownURL =~ s|/+$||;
    return $drilldownURL . '/browse/' . $buildInfo->{key};
}

sub getRecordsAfter {
    my FlowPlugin::Bamboo::Reporting $self = shift;
    my FlowPlugin::Bamboo $bamboo = shift;
    my FlowPDF::Component::EF::Reporting::Metadata $metadata = shift;

    my $params = $bamboo->getContext()->getRuntimeParameters();

    my $metadataValues = $metadata->getValue();
    logDebug("Got metadata value in getRecordsAfter:", Dumper $metadataValues);

    my $records = $bamboo->getBuildRuns($params->{projectKey}, $params->{planKey}, {
        maxResults => 0,
        afterTime  => $metadataValues->{startTime}
    });

    logDebug("Records after GetRecordsAfter", Dumper $records);

    return $records;
}

sub getLastRecord {
    my FlowPlugin::Bamboo::Reporting $self = shift;
    my FlowPlugin::Bamboo $pluginObject = shift;

    my $params = $pluginObject->getContext()->getRuntimeParameters();
    logDebug("Last record runtime params:", Dumper $params);

    my $runs = $pluginObject->getBuildRuns($params->{projectKey}, $params->{planKey}, {
        maxResults => 1
    });

    return $runs->[0];
}

sub initialGetRecords {
    my FlowPlugin::Bamboo::Reporting $self = shift;
    my FlowPlugin::Bamboo $pluginObject = shift;
    my ($limit) = @_;

    my $params = $pluginObject->getContext()->getRuntimeParameters();
    my $records = $pluginObject->getBuildRuns($params->{projectKey}, $params->{planKey}, {
        maxResults => ($limit || 10)
    });

    return $records;
}

sub compareMetadata {
    my ($self, $metadata1, $metadata2) = @_;
    my $value1 = $metadata1->getValue();
    my $value2 = $metadata2->getValue();

    my FlowPlugin::Bamboo $pluginObject = $self->getPluginObject();

    return $pluginObject->compareISODates($value1->{startTime}, $value2->{startTime});
}

1;