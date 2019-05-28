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
    my $params = $bamboo->getContext()->getRuntimeParameters();

    # Adding from the end of the list
    for my $row ( reverse @$records ) {

        my $data = $dataset->newData({
            reportObjectType => 'build',
        });

        # TODO: check fields

        $row->{sourceUrl} = $row->{url};
        $row->{pluginConfiguration} = $params->{config};
        $row->{startTime} = $row->{buildStartedTime};
        $row->{endTime} = $row->{buildCompletedTime};
        $row->{documentId} = $row->{url};
        $row->{buildStatus} = $row->{buildState};

        # hardcode
        $row->{launchedBy} = 'admin';

        for my $k (keys %$row) {
            next if ref $row->{$k};
            $data->addValue($k => $row->{$k});
        }

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

sub getRecordsAfter {
    my FlowPlugin::Bamboo::Reporting $self = shift;
    my FlowPlugin::Bamboo $bamboo = shift;
    my FlowPDF::Component::EF::Reporting::Metadata $metadata = shift;

    my $params = $bamboo->getContext()->getRuntimeParameters();

    my $metadataValues = $metadata->getValue();
    logDebug("Got metadata value in getRecordsAfter:", Dumper $metadataValues);

    my $records = $bamboo->getBuildRuns($params->{projectKey}, $params->{planKey}, {
        maxResults => 0,
        afterTime  => $metadataValues->{buildStartedTime}
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

    return $pluginObject->compareISODates($value1->{buildStartedTime}, $value2->{buildStartedTime});
}

1;