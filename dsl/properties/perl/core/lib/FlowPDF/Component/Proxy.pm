=head1 NAME

FlowPDF::Component::Proxy

=head1 DESCRIPTION

This module provides standard mechanism for proxy-handling for ElectricFlow plugins.
This implementation should be used any time when proxy requirement appears.

=head1 Q&A

=head2 Why do we need a module, if one can just set environment variables?

It is a good question. Since our current perl is way too old (5.8.8 now), we have some
outdated modules and bugs, that have been fixed in releases after 5.8.8. For example,
we can't use default LWP functionality for https proxy handling due to bugs in LWP.
But there is a way to deal with that. We have Crypt::SSLeay module installed,
that provides requried functionality by the default way of the *nix OS. It is respects env variables.

This module does B<proper> implementation of all that logic and workarounds and should be used as
said in this help and provides a possibility to developer to change actual implementation of proxy
code without changing the interface. It is mush simpler than fix it in each plugin separately.


=head1 AUTHOR

Dmitriy <dshamatr@electric-cloud.com> Shamatrin

=head1 SYNOPSIS

%%%LANG=perl%%%
    use EC::ProxyDriver;

    # create proxy dispatcher object
    my $proxy = EC::ProxyDriver->new({
        url => 'http://docker:3128',
        username => 'user1',
        password => 'password1',
        debug => 0,
    });

    # apply proxy

    $proxy->apply();

    # run some proxy related code
    ...;

    # detach proxy, if required.
    $proxy->detach();
%%%LANG%%%

=head1 METHODS

=over

=cut

package FlowPDF::Component::Proxy;
use base qw/FlowPDF::Component/;
use strict;
use warnings;
use Carp;
use Data::Dumper;
use FlowPDF::Log;

our $VERSION = 0.02;
our $componentName = 'FlowPDF::Component::Proxy';
our $componentVersion = $VERSION;

sub init {
    my ($class, $params) = @_;

    return $class->new($params);
}


=item B<new>

Constructor method, creates proxy dispatcher object.

%%%LANG=perl%%%
    my $proxy = EC::ProxyDriver->new({
        url => 'http://docker:3128',
        username => 'user1',
        password => 'password1',
        debug => 0,
    });
%%%LANG%%%

=cut

sub new {
    my ($class, $params) = @_;

    my $self = {
        _auth_method    => $params->{auth_method} || 'basic',
        _proxy_url      => $params->{url} || '',
        _proxy_username => $params->{username} || '',
        _proxy_password => $params->{password} || '',
        _debug          => ($params->{debug} ? 1 : 0),
        __detach_list   => [],
    };

    bless $self, $class;
    return $self;
}


=item B<apply>

Applies proxy changes to whole context in a right way. One should use that function
and be sure that proxy is set.

%%%LANG=perl%%%
    $proxy->apply();
%%%LANG%%%

=cut

sub apply {
    my ($self) = @_;

    unless ($self->url()) {
        $self->debug_msg("No proxy url has been provided. Nothing to do.");
    }

    ## temporary disabled setting of HTTP_PROXY env
    # $self->debug_msg("Attaching ENV HTTP_PROXY: ", $self->url());
    # $self->set_env(HTTP_PROXY => $self->url());
    $self->debug_msg("Attaching ENV HTTPS_PROXY: ", $self->url());
    $self->set_env(HTTPS_PROXY => $self->url());

    if ($self->username()) {
        $self->debug_msg("Attaching ENV HTTPS_PROXY_USERNAME: ", $self->username());
        $self->set_env(HTTPS_PROXY_USERNAME => $self->username());
    }

    if ($self->password()) {
        $self->debug_msg("Attaching HTTPS_PROXY_PASSWORD: [PROTECTED]");
        $self->set_env(HTTPS_PROXY_PASSWORD => $self->password());
    }
    return $self;
}


sub set_env {
    my ($self, $env_name, $value) = @_;

    $ENV{$env_name} = $value;
    push @{$self->{__detach_list}}, $env_name;
    return $self;
}


=item B<detach>

Disables proxy for a whole context. It could be useful sometimes to revert all changes that was made by apply function.

%%%LANG=perl%%%
    $proxy->detach();
%%%LANG%%%

=cut

sub detach {
    my ($self) = @_;

    while (my $e = pop(@{$self->{__detach_list}})) {
        $ENV{$e} = '[PROTECTED]' if $e eq 'HTTPS_PROXY_PASSWORD';
        $self->debug_msg("Detaching ENV $e value ($ENV{$e})");
        delete $ENV{$e};
    }
    return $self;
}


sub getset {
    my ($self, $field, $value) = @_;

    if (!exists $self->{$field}) {
        croak "Field $field does not exist, aborting...";
    }
    if (!defined $value) {
        return $self->{$field};
    }

    $self->{$field} = $value;
    return 1;
}


=item B<url>

Returns a proxy url if set. Returns empty string if not.

%%%LANG=perl%%%
    my $proxy_url = $proxy->url();
%%%LANG%%%

=cut

sub url {
    my ($self, $value) = @_;

    return $self->getset('_proxy_url' => $value);
}


=item B<auth_method>

Returns a proxy auth method that is being used. Currently only basic is supported,
which is set as default.

%%%LANG=perl%%%
    my $auth_method = $proxy->auth_method();
%%%LANG%%%

=cut

sub auth_method {
    my ($self, $value) = @_;

    return $self->getset('_auth_method' => $value);
}


=item B<username>

Returns a proxy auth username if set. Returns empty string if not.

%%%LANG=perl%%%
    my $proxy_url = $proxy->username();
%%%LANG%%%

=cut

sub username {
    my ($self, $value) = @_;

    return $self->getset('_proxy_username' => $value);
}


=item B<password>

Returns a proxy auth password if set. Returns empty string if not.

%%%LANG=perl%%%
    my $proxy_url = $proxy->password();
%%%LANG%%%

=cut

sub password {
    my ($self, $value) = @_;

    return $self->getset('_proxy_password' => $value);
}


=item B<debug>

Enables and disables debug mode for module.

To enable:

%%%LANG=perl%%%
    $proxy->debug(1);
%%%LANG%%%

To disable

%%%LANG=perl%%%
    $proxy->debug(0);
%%%LANG%%%

=cut

sub debug {
    my ($self, $value) = @_;

    if (defined $value && $value !~ m/^\d+$/s) {
        croak "Debug should be integer, not '$value'";
    }
    return $self->getset('_debug' => $value);
}


=item B<augment_request>

Augments HTTP::Request object with proxy headers.

%%%LANG=perl%%%
    my $req = HTTP::Request->new(...);
    $req = $proxy->augment_request($req);
%%%LANG%%%

=cut

sub augment_request {
    my ($self, $request_object) = @_;

    if (!$request_object) {
        croak "Request object could not be empty";
    }
    if (ref $request_object ne 'HTTP::Request') {
        croak "HTTP::Request object expected, got: ", ref $request_object;
    }
    my ($proxy_username, $proxy_password) = ($self->username(), $self->password());
    if ($proxy_username || $proxy_password) {
        $self->debug_msg("Augmenting request object with username: ", $proxy_username, " and password: [PROTECTED]");
        $request_object->proxy_authorization_basic($proxy_username, $proxy_password);
    }
    return $request_object;
}


=item B<detach_request>

Detaches changes of request and removes added headers.

%%%LANG=perl%%%
    $req = $proxy->detach_request($req);
%%%LANG%%%

=cut

sub detach_request {
    my ($self, $request_object) = @_;

    return $request_object;
}


=item B<augment_lwp>

Augments LWP::UserAgent object with proxy information.

%%%LANG=perl%%%
    my $ua = LWP::UserAgent->new(...);
    $ua = $proxy->augment_lwp($ua);
%%%LANG%%%

=cut

sub augment_lwp {
    my ($self, $ua) = @_;

    if (!$ua) {
        croak "LWP object could not be empty";
    }
    if (ref $ua ne 'LWP::UserAgent') {
        croak "LWP::UserAgent object expected, got: ", ref $ua;
    }
    my $proxy_url = $self->url();
    if ($proxy_url) {
        $self->debug_msg("Augmenting LWP object with HTTP proxy settings: ", $proxy_url);
        $ua->proxy(['http'] => $proxy_url);
    }

    return $ua;
}


=item B<detach_lwp>

Removes proxy setup from an LWP object.

%%%LANG=perl%%%
    $ua = $proxy->detach_lwp($ua);
%%%LANG%%%

=cut

sub detach_lwp {
    my ($self, $ua) = @_;

    if (!$ua) {
        croak "LWP object could not be empty";
    }
    if (ref $ua ne 'LWP::UserAgent') {
        croak "LWP::UserAgent object expected, got: ", ref $ua;
    }

    $ua->{proxy} = {};
    return $ua;
}


sub debug_msg {
    my ($self, @msg) = @_;

    if ($self->debug()) {
        my $msg = join '', @msg;
        $msg = '[DEBUG]: ' . $msg;
        print $msg, "\n";
    }
    return 1;
}

1;

=back

=cut
