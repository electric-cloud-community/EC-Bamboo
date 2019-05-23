=head1 NAME

FlowPDF::Parameter

=head1 AUTHOR

Electric Cloud

=head1 DESCRIPTION

This class represents ElectricFlow parameters. It could be:

=over 4

=item B<Configuration Parameters>

=item B<Procedure Parameters>

=back

=head1 SYNOPSIS

Objects of this class have support of toString(). If this object will be used in a string context like:

%%%LANG=perl%%%

    my $parameter = $stepParameters->getParameter('query');
    print "Parameter: $parameter\n";

%%%LANG%%%

getValue() method will be applied automatically and you will get a value instead of reference address.

This object is being returned by L<FlowPDF::Config> or L<FlowPDF::StepParameters> getParameter() method.

L<FlowPDF::StepParameters> object is being returned by getStepParameters() method of L<FlowPDF::Context>.

L<FlowPDF::Config> object is being returned by getConfigValues method of L<FlowPDF::Context>

=head1 METHODS

=head2 getName()

=head3 Description

Gets a name from FlowPDF::Parameter object.

=head3 Parameters

=over 4

=item None

=back

=head3 Returns

=over 4

=item (String) Name of the parameter.

=back

=head3 Usage

%%%LANG=perl%%%

    my $parameterName = $parameter->getName();

%%%LANG%%%

=head2 getValue()

=head3 Description

Returns a value of the current parameter.

=head3 Parameters

=over 4

=item None

=cut

=back

=head3 Returns

=over 4

=item (String) Value of the parameter

=back

=head3 Usage

%%%LANG=perl%%%

    my $parameterValue = $parameter->getValue();

%%%LANG%%%

Also, note, that B<this method is being applied by default, if FlowPDF::Parameter object is being used in string context>:

%%%LANG=perl%%%

    # getValue is being applied automatically in string conext. Following 2 lines of code are doing the same:
    print "Query: $query\n";
    printf "Query: %s\n", $query->getValue();

%%LANG

=head2 setName($name)

=head3 Description

Sets a name for the current parameter.

=head3 Parameters

=over 4 

=item (Required) (String) Parameter Name

=back

=head3 Returns

=over 4

=item (FlowPDF::Parameter) self

=back

=head3 Usage

%%%LANG=perl%%%

    $parameter->setName('myNewName');

%%%LANG%%%

=head2 setValue($value)

=head3 Description

=head3 Parameters

=over 4

=item (Required)(String) Parameter Value

=back

=head3 Returns

=over 4

=item (FlowPDF::Parameter) self

=back

=head3 Usage

%%%LANG=perl%%%

    $parameter->setValue('MyNewValue');

%%%LANG%%%

=cut

package FlowPDF::Parameter;
use base qw/FlowPDF::BaseClass2/;
use FlowPDF::Types;

__PACKAGE__->defineClass({
    name  => FlowPDF::Types::Scalar(),
    value => FlowPDF::Types::Scalar(),
});
use overload
    '""' => 'toString';

use strict;
use warnings;

sub set {
    my ($self, $name, $value) = @_;

    $self->setName($name);
    $self->setValue($value);

    return 1;
}


sub toString {
    my ($self) = @_;

    return $self->getValue();
}

1;