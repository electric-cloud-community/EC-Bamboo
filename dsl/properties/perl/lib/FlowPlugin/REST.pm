package FlowPlugin::REST;
use strict;
use warnings FATAL => 'all';

use base qw/FlowPDF::Client::REST/;

use FlowPDF::Log;
use FlowPDF::Helpers qw/bailOut/;
use JSON::XS qw/decode_json encode_json/;
use Data::Dumper;

__PACKAGE__->defineClass({
    restClient   => FlowPDF::Types::Reference('FlowPDF::Client::REST'),
    headers      => FlowPDF::Types::Reference('ARRAY'),
    hooks        => FlowPDF::Types::Reference('HASH'),
    logger       => FlowPDF::Types::Reference('FlowPDF::Log'),
    credential   => FlowPDF::Types::Reference('FlowPDF::Credential'),
    ignoreErrors => FlowPDF::Types::Enum('0', '1'),
});

sub new {
    my $class = shift;
    my FlowPDF::Context $context = shift;
    my ($params) = @_;

    my FlowPDF::Config $config = $context->getConfigValues();

    # Should create logger before initializing
    my $debugLog = 0;
    if (!$params->{debug}) {
        $debugLog = $config->getParameter('debugLevel')->getValue() if $config->isParameterExists('debugLevel');
    }
    my $logger = FlowPDF::Log->new({ level => $debugLog });

    my $self = {
        endpoint      => $config->getRequiredParameter('endpoint')->getValue(),

        # No default encode/decode
        encodeContent => sub {return shift},
        decodeContent => sub {return shift},

        # Allow to not repeat API base path for each request
        APIBase       => '',
        %$params
    };
    bless($self, $class);

    $self->setRestClient($context->newRESTClient());

    $self->setLogger($logger);
    $self->setErrorHandler($params->{errorHandler}) if ($params->{errorHandler});

    $self->init();

    return $self;
}

sub init {
    my ($self, $params) = @_;
    # Default content type is JSON
    $self->{contentType} = 'application/json' unless $params->{contentType};

    if ($self->{contentType} eq 'application/json') {
        $self->{headers}{Accept} = 'application/json';
        $self->{contentHeader} = 'application/json';
        $self->{encodeContent} = \&encode_json;
        $self->{decodeContent} = \&decode_json;
    }
    # TODO: add XML
    else {
        bailOut("No content type specified or it is not implemented");
    }

    return $self;
}

#@returns FlowPDF::Log
sub logger {shift->{logger}};

sub _call {
    my ($self, $method, $path, $queryParams, $content, $params) = @_;

    my FlowPDF::Client::REST $rest = $self->getRestClient();

    my HTTP::Request $request = $self->buildRequest($method, $path, {
        queryParams => $queryParams,
        content     => $content
    });
    $self->logger->trace("Request", $request);

    $self->logger->debug("Request URI: " . $request->uri);
    my HTTP::Response $response = $rest->doRequest($request);
    $self->logger->trace("Response", $response);

    if (!$response->is_success) {
        # Error handling
        return $self->processRequestError($response, $params);
    }

    my $responseContent = $response->decoded_content();
    if ($responseContent eq '' ){
        return 1;
    }

    return $self->decodeOrDie($responseContent);
}

sub processRequestError {
    my ($self, $response, $requestParams) = @_;

    if ($requestParams->{ignoreErrors} || $self->getIgnoreErrors()) {
        $self->logger->debug("Ignoring error: " . $response->status_line());
        return;
    }

    my $errorCode = $response->code();

    # Priorities to choose handler
    # 1. Request specific for code
    # 2. Global handler for code
    # 3. Request default handler
    # 4. Global default handler
    # 5. Bail out (moved to method to allow override)

    my $handlerSub;
    if (exists $requestParams->{errorHook}{$errorCode}) {
        $handlerSub = $requestParams->{errorHook}{$errorCode};
    }
    elsif (exists $self->{errorHook}{$errorCode}) {
        $handlerSub = $self->{errorHook}{$errorCode};
    }
    elsif (exists $requestParams->{errorHook}{default}) {
        $handlerSub = $requestParams->{errorHook}{default};
    }
    elsif (exists $self->{errorHook}{default}) {
        $handlerSub = $self->{errorHook}{default};
    }
    else {
        $handlerSub = \&defaultErrorHandler;
    }

    # Trying to parse the response
    my $parsed;
    if ($response->decoded_content()) {
        eval {
            $parsed = $self->decodeContent($response->decoded_content());
        } or do {
            $self->logger->debug("Failed to decode error content: $@");
        }
    }

    die "Empty error handler" unless defined $handlerSub;
    return &$handlerSub($response, $parsed);
}

sub defaultErrorHandler {
    my ($self, $response) = @_;
    $self->logger->debug("Received response with error", $response);
    bailOut("Error received in response");
}


sub buildRequest {
    my ($self, $method, $path, $params) = @_;

    my FlowPDF::Client::REST $rest = $self->{restClient};

    my $queryParams = $params->{queryParams};
    $self->logger->trace("Query parameters: ", $queryParams) if defined $queryParams;
    my $requestContent = $params->{content};

    # Making a copy to allow adding request specific headers
    my %headers = %{$self->{headers}};

    my $requestPath = $self->buildRequestPath($path);

    my HTTP::Request $request = $rest->newRequest($method => $requestPath);

    # Query parameters
    if (defined $queryParams && ref $queryParams eq 'HASH') {
        $request->uri->query_form(%$queryParams);
    }

    if (defined $requestContent) {
        my $encoded = $self->encodeOrDie($requestContent);
        $request->content($encoded);
        $headers{'Content-Type'} = $self->{contentHeader};
    }

    while (my ($name, $value) = each %headers) {
        $request->header($name, $value);
    }

    return $request;
}

sub buildRequestPath {
    my ($self, $path) = @_;

    my $endpoint = $self->{endpoint};
    my $apiPath = $self->{APIBase};

    $endpoint =~ s|/+$||g;
    $path =~ s|^/+||g;

    if (defined $apiPath && $apiPath ne '') {
        $endpoint =~ s|/+$||g;
        $apiPath =~ s|^/+||g;

        $endpoint .= '/' . $apiPath
    }

    $endpoint =~ s|/+$||g;
    $path =~ s|^/+||g;

    return $endpoint . '/' . $path;
}

sub post {return shift->_call('POST', @_)}
sub get {return shift->_call('GET', @_)}
sub delete {return shift->_call('DELETE', @_)}
sub patch {return shift->_call('PATCH', @_)}
sub put {return shift->_call('PUT', @_)}

sub encodeContent {
    my ($self, $content) = @_;
    return &{$self->{encodeContent}}($content);
}

sub decodeContent {
    my ($self, $content) = @_;
    return &{$self->{decodeContent}}($content);
}

sub encodeOrDie {
    my ($self, $content) = @_;
    my $result = eval {$self->encodeContent($content)};
    bailOut("Failed to encode content: $@.\nReceived: " . Dumper($content)) if (defined $@ && $@ ne '');
    return $result;
}
sub decodeOrDie {
    my ($self, $content) = @_;
    my $result = eval {$self->decodeContent($content)};
    bailOut("Failed to decode content: $@.\nReceived: " . Dumper($content)) if (defined $@ && $@ ne '');
    return $result;
}


1;