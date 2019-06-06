package FlowPDF::Client::REST::Auth;
use base qw/FlowPDF::BaseClass2/;
use strict;
use warnings;

use FlowPDF::Types;

__PACKAGE__->defineClass({
    authType => FlowPDF::Types::Scalar(),
    authValues => FlowPDF::Types::Reference('HASH')
});


1;
