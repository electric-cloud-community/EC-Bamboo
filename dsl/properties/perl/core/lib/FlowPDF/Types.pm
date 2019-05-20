package FlowPDF::Types;
# use base qw/Exporter/;

use strict;
use warnings;
use Data::Dumper;

use FlowPDF::Helpers qw/bailOut/;

use FlowPDF::Types::Any;
use FlowPDF::Types::Reference;
use FlowPDF::Types::Scalar;
use FlowPDF::Types::Enum;
use FlowPDF::Types::ArrayrefOf;

sub Reference {
    my (@refs) = @_;

    return FlowPDF::Types::Reference->new(@refs);
}

sub Enum {
    my (@vals) = @_;

    return FlowPDF::Types::Enum->new(@vals);
}

sub Scalar {
    my ($value) = @_;
    return FlowPDF::Types::Scalar->new($value);
}

sub Any {
    return FlowPDF::Types::Any->new();
}

sub ArrayrefOf {
    my (@refs) = @_;

    return FlowPDF::Types::ArrayrefOf->new(@refs);
}

1;
