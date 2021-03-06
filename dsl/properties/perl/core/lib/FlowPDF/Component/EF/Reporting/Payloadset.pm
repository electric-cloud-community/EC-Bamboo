# WARNING
# Do not edit this file manually. Your changes will be overwritten with next FlowPDF update.
# WARNING

=head1 NAME

FlowPDF::Component::EF::Reporting::Payloadset

=head1 AUTHOR

CloudBees

=head1 DESCRIPTION

A payloadset object.

=head1 METHODS



=head2 getReportObjectTypes()

=head3 Description

Returns an array reference of string report object types for current set of payloads

=head3 Parameters

=over 4

=item None

=back

=head3 Returns

=over 4

=item (ARRAY ref) Report object types

=back

=head3 Exceptions

=over 4

=item None

=back

=head3 Usage

%%%LANG=perl%%%

    my $reportObjectTypes = $payloadSet->getReportObjectTypes();

%%%LANG%%%


=head2 getPayloads()

=head3 Description

Returns an array references to the payloads.

=head3 Parameters

=over 4

=item None

=back

=head3 Returns

=over 4

=item (ARRAY ref) of L<FlowPDF::Component::EF::Reporting::Payload>

=back

=head3 Exceptions

=over 4

=item None

=back

=head3 Usage

%%%LANG=perl%%%

    my $payloads = $payloadset->getPayloads();

%%%LANG%%%

=head2 newPayload($params)

=head3 Description

Creates a new payload and adds it to current payload set and returns a reference for it.

=head3 Parameters

A hash reference with following fields

=over 4

=item (Required)(String) reportObjectType: a report object type for the current payload

=item (Optional)(HASH ref) values: a values that will be send to the Devops Insight Center. An actual payload.

=back

=head3 Returns

=over 4

=item (L<FlowPDF::Component::EF::Reporting::Payload>) A reference to newly created payload.

=back

=head3 Exceptions

=over 4

=item Fatal error if required fields are missing.

=back

=head3 Usage

%%%LANG=perl%%%

    my $payload = $payloadSet->newPayload({
        reportObjectType => 'build',
        values => {
            buildNumber => '2',
            status => 'success',
        }
    });

%%%LANG%%%

=cut

package FlowPDF::Component::EF::Reporting::Payloadset;
use base qw/FlowPDF::BaseClass2/;
use FlowPDF::Types;

__PACKAGE__->defineClass({
    ec                => FlowPDF::Types::Reference('ElectricCommander'),
    reportObjectTypes => FlowPDF::Types::ArrayrefOf(FlowPDF::Types::Scalar()),
    payloads          => FlowPDF::Types::ArrayrefOf(FlowPDF::Types::Reference('FlowPDF::Component::EF::Reporting::Payload'))
});

use strict;
use warnings;
use JSON;

use FlowPDF::Helpers qw/bailOut inArray/;
use FlowPDF::Component::EF::Reporting::Payload;
use FlowPDF::Log;


=head2 newPayload($params)

=head3 Description

Returns an array reference of string report object types for current set of payloads

=head3 Parameters

A hash reference with following fields

=over 4

=item (Required)(String) reportObjectType: a report object type for the current payload

=item (Optional)(HASH ref) values: a values that will be send to the Devops Insight Center. An actual payload.

=back

=head3 Returns

=over 4

=item (L<FlowPDF::Component::EF::Reporting::Payload>) A reference to newly created payload.

=back

=head3 Exceptions

=over 4

=item Fatal error if required fields are missing.

=back

=head3 Usage

%%%LANG=perl%%%

    my $payload = $payloadSet->newPayload({
        reportObjectType => 'build',
        values => {
            buildNumber => '2',
            status => 'success',
        }
    });

%%%LANG%%%

=cut

sub newPayload {
    my ($self, $params) = @_;

    if (!$params->{dependentPayloads}) {
        $params->{dependentPayloads} = [];
    }
    my $payload = FlowPDF::Component::EF::Reporting::Payload->new($params);

    my $payloads = $self->getPayloads();
    push @$payloads, $payload;
    return $payload;
}


# private function
sub report {
    my ($self) = @_;

    my $retval = {};
    my $payloads = $self->getPayloads();
    reportRecursive($payloads, $retval);
    return $retval;
}


sub reportOld {
    my ($self) = @_;

    my $payloads = $self->getPayloads();

    my $retval = {};
    for my $row (@$payloads) {
        $row->sendReportToEF();
        $retval->{$row->getReportObjectType()}++;
    }
    return $retval;
}

sub reportRecursive {
    my ($payloads, $retval) = @_;
    # use Data::Dumper;
    # print Dumper \@_;
    for my $row (@$payloads) {
        $row->sendReportToEF();
        $retval->{$row->getReportObjectType()}++;
        my $dep = $row->getDependentPayloads();
        if (@$dep) {
            reportRecursive($dep, $retval);
        }
    }
}

=head2 getLastPayload()

=head3 Description

Returns the last payload from current Payloadset.

=head3 Parameters

=over 4

=item None

=back

=head3 Returns

=over 4

=item (L<FlowPDF::Component::EF::Reporting::Payload>) A reference to the last payload.

=back

=head3 Exceptions

=over 4

=item None

=back

=head3 Usage

%%%LANG=perl%%%

    my $lastPayload = $payloadSet->getLastPayload();

%%%LANG%%%

=cut

sub getLastPayload {
    my ($self) = @_;

    my $payloads = $self->getPayloads();
    return $payloads->[-1];
}


1;
