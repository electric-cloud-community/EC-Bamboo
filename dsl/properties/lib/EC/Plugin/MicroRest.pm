package EC::Plugin::MicroRest;

use strict;
use warnings;

use LWP::UserAgent;
use Data::Dumper;
use Carp;
use JSON;
use HTTP::Request;

use subs qw/allowed_methods ignore_errors/;

my $ignore_errors = 0;

my @ALLOWED_METHODS = ('GET', 'POST', 'PUT', 'OPTIONS', 'DELETE');


sub ignore_errors {
    my (undef, $flag) = @_;

    unless (defined $flag) {
        return $ignore_errors;
    }

    $ignore_errors = $flag;
    return $ignore_errors;
}


sub new {
    my ($class, %params) = @_;

    my $self = {};

    for my $p (qw/url user password auth/) {
        if ($params{auth} && $params{auth} !~ m/^basic$/s) {
            croak "Wrong auth method";
        }
        if (!defined $params{$p}) {
            croak "Missing mandatory parameter $p\n";
        }
        $self->{_data}->{$p} = $params{$p};
    }

    $params{url} =~ s/\s+$//gs;
    $params{url} =~ s|\/$||gs;

    $params{url} .= '/';

    if ($params{dispatch} && ref $params{dispatch} eq 'HASH') {
        $self->{_dispatch} = $params{dispatch};
    }

    if ($params{content_type} && $params{content_type} =~ m/^(?:xml|json)$/is) {
        $self->{_ctype} = lc $params{content_type};
    }

    if (!$self->{_ctype}) {
        $self->{_ctype} = 'json';
    }
    $self->{ua} = LWP::UserAgent->new();
    bless $self, $class;
    return $self;
}


sub _call {
    my ($self, $meth, $url, $content) = @_;

    if ($meth !~ m/^(?:GET|POST|PUT|OPTIONS|DELETE)$/s) {
        croak "Method $meth is unknown";
    }

    # print "Request URL is:" .  $self->{_data}->{url} . $url . "\n";
    my $req = HTTP::Request->new($meth => $self->{_data}->{url} . $url);
    if ($self->{_data}->{auth} eq 'basic') {
        $req->authorization_basic($self->{_data}->{user}, $self->{_data}->{password});
    }

    if ($self->{_ctype} eq 'json') {
        $req->header('Accept' => 'application/json');
    }
    if ($content) {
        $content = $self->encode_content($content);
        if ($self->{_ctype} eq 'json') {
            $req->header('Content-Type' => 'application/json');
        }
        $req->content($content);
    }

    my $resp = $self->{ua}->request($req);
    my $object;
    if ($resp->code() < 400) {
        $object = $self->decode_content($resp->decoded_content());
    }
    elsif ($resp->code() == 401) {
        croak "Unauthorized. Check your credentials\n";
    }
    elsif ($resp->code() == 403) {
        croak "Access is forbidden, check your data\n";
    }
    else {
        if (! ignore_errors) {
            croak "Error occured: " . $resp->decoded_content() . "\n";
        }
        $object = $self->decode_content($resp->decoded_content());
        print "Error occured: " . Dumper $resp->decoded_content();
    }
    return $object;
}


sub encode_content {
    my ($self, $content) = @_;

    return undef unless $content;
    if ($self->{_ctype} eq 'json') {
        return encode_json($content);
    }
    elsif ($self->{_ctype} eq 'xml') {
        return XMLout($content);
    }

    # return as is
    return $content;
}


sub decode_content {
    my ($self, $content) = @_;

    return '' unless $content;

    if ($self->{_ctype} eq 'json') {
        return decode_json($content);
    }
    elsif ($self->{_ctype} eq 'xml') {
        return XMLin($content);
    }

    # return as is
    return $content;
}

sub get {
    my ($self, $url, $params) = @_;

    return $self->_call(
        'GET' =>  $url,
        $params
    );
}

sub post {
    my ($self, $url, $params) = @_;

    return $self->_call(
        'POST' =>  $url,
        $params
    );
}

sub delete {
    my ($self, $url, $params) = @_;

    return $self->_call(
        'DELETE' =>  $url,
        $params
    );
}



sub encode_request {
    1;
}

sub decode_response {
    1;
}
1;

