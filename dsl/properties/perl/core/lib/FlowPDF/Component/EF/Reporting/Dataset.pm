=head1 NAME

FlowPDF::Component::EF::Reporting::Dataset

=head1 AUTHOR

CloudBees

=head1 DESCRIPTION

A dataset object.

=head1 METHODS

=head2 getReportObjectTypes()

=head3 Description

Returns a report object types for current dataset.

=head3 Parameters

=over 4

=item None

=back

=head3 Returns

=over 4

=item (ARRAY ref) Report object types for current dataset

=back

=head3 Exceptions

=over 4

=item None

=back

=head3 Usage

%%%LANG=perl%%%

    my $reportObjectTypes = $dataset->getReportObjectTypes();

%%%LANG%%%

=head2 getData()

=head3 Description

Returns an ARRAY ref with data objects for current dataset.

=head3 Parameters

=over 4

=item None

=back

=head3 Returns

=over 4

=item (ARRAY ref of L<FlowPDF::Component::EF::Reporting::Data>) An array reference of Data object for the current Dataset object.

=back

=head3 Exceptions

=over 4

=item None

=back

=head3 Usage

%%%LANG=perl%%%

    my $data = $dataset->getData();

%%%LANG%%%

=cut

package FlowPDF::Component::EF::Reporting::Dataset;
use base qw/FlowPDF::BaseClass2/;
use FlowPDF::Types;

__PACKAGE__->defineClass({
    reportObjectTypes => FlowPDF::Types::ArrayrefOf(FlowPDF::Types::Scalar()),
    data              => FlowPDF::Types::ArrayrefOf(FlowPDF::Types::Reference('FlowPDF::Component::EF::Reporting::Data')),
});

use strict;
use warnings;

use Data::Dumper;

use FlowPDF::Helpers qw/bailOut inArray/;
use FlowPDF::Component::EF::Reporting::Data;
use FlowPDF::Log;


=head2 newData($params)

=head3 Description

Creates a new data object and adds it to the current dataset and returns a reference for it.

=head3 Parameters

A hash reference with following fields

=over 4

=item (Required)(String) reportObjectType: a report object type for the current data

=item (Optional)(HASH ref) values: a values from which data object will be created.

=back

=head3 Returns

=over 4

=item (L<FlowPDF::Component::EF::Reporting::Data>) A reference to newly created data.

=back

=head3 Exceptions

=over 4

=item Fatal error if required fields are missing.

=back

=head3 Usage

%%%LANG=perl%%%

    my $data = $dataset->newData({
        reportObjectType => 'build',
        values => {
            buildNumber => '2',
            status => 'success',
        }
    });

%%%LANG%%%

=cut


sub newData {
    my ($self, $params) = @_;

    if (!$params->{values}) {
        $params->{values} = {};
    }
    if (!$params->{dependentData}) {
        $params->{dependentData} = [];
    }
    my $data = FlowPDF::Component::EF::Reporting::Data->new($params);
    logTrace("Data object address:  $data");
    my $dataRef = $self->getData();
    logTrace("Dataset object BEFORE inserting into dataset: ", Dumper $dataRef);
    push @$dataRef, $data;
    logTrace("Dataset object AFTER inserting into dataset: ", Dumper $dataRef);
    return $data;
}

1;
