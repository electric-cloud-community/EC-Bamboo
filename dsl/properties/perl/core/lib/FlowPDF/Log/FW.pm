package FlowPDF::Log::FW;
use base qw/Exporter/;

our @EXPORT = qw/fwLogInfo fwLogDebug fwLogTrace fwLogError fwLogWarning/;
use strict;
use warnings;
use Data::Dumper;
use Carp;
use FlowPDF::Helpers qw/inArray/;

our $LOG_LEVEL = 0;
our $LOG_TO_PROPERTY = '';
our $LOG_TO_FILE = '';
our $MASK_PATTERNS = [];
our $LOGGER = undef;

# BEGIN {
#     if (defined $ENV{FLOWPDF_LOG_LEVEL}) {
#         $LOG_LEVEL = $ENV{FLOWPDF_LOG_LEVEL};
#     }

#     if (!$LOGGER) {
#         $LOGGER = __PACKAGE__->new();
#     }
# }
use constant {
    ERROR => -1,
    INFO  => 0,
    DEBUG => 1,
    TRACE => 2,
};

sub setMaskPatterns {
    my (@params) = @_;

    unless (@params) {
        croak "Missing mask patterns for setMastPatterns.";
    }
    if ($params[0] eq __PACKAGE__ || ref $params[0] eq __PACKAGE__) {
        shift @params;
    }
    for my $p (@params) {
        next if isCommonPassword($p);
        $p = quotemeta($p);
        # avoiding duplicates
        if (inArray($p, @$MASK_PATTERNS)) {
            next;
        }

        push @$MASK_PATTERNS, $p;
    }
    return 1;
}

sub isCommonPassword {
    my ($password) = @_;

    # well, huh.
    if ($password eq 'password') {
        return 1;
    }
    if ($password =~ m/^(?:TEST)+$/is) {
        return 1;
    }
    return 0;
}

sub maskLine {
    my ($self, $line) = @_;

    if (!ref $self || $self eq __PACKAGE__) {
        $line = $self;
    }

    for my $p (@$MASK_PATTERNS) {
        $line =~ s/$p/[PROTECTED]/gs;
    }
    return $line;
}

sub setLogToProperty {
    my ($param1, $param2) = @_;

    # 1st case, when param 1 is a reference, we are going to set log to property for current object.
    # but if this reference is not a FlowPDF::Log reference, it will bailOut
    if (ref $param1 and ref $param1 ne __PACKAGE__) {
        croak(q|Expected a reference to FlowPDF::Log, not a '| . ref $param1 . q|' reference|);
    }

    if (ref $param1) {
        if (!defined $param2) {
            croak "Property path is mandatory parameter";
        }
        $param1->{logToProperty} = $param2;
        return $param1;
    }
    else {
        if ($param1 eq __PACKAGE__) {
            $param1 = $param2;
        }
        if (!defined $param1) {
            croak "Property path is mandatory parameter";
        }
        $LOG_TO_PROPERTY = $param1;
        return 1;
    }
}

sub getLogProperty {
    my ($self) = @_;

    if (ref $self && ref $self eq __PACKAGE__) {
        return $self->{logToProperty};
    }
    return $LOG_TO_PROPERTY;
}

sub getLogLevel {
    my ($self) = @_;

    if (ref $self && ref $self eq __PACKAGE__) {
        return $self->{level};
    }

    return $LOG_LEVEL;
}


sub setLogLevel {
    my ($param1, $param2) = @_;

    if (ref $param1 and ref $param1 ne __PACKAGE__) {
        croak (q|Expected a reference to FlowPDF::Log, not a '| . ref $param1 . q|' reference|);
    }

    if (ref $param1) {
        if (!defined $param2) {
            croak "Log level is mandatory parameter";
        }
        $param1->{level} = $param2;
        return $param1;
    }
    else {
        if ($param1 eq __PACKAGE__) {
            $param1 = $param2;
        }
        if (!defined $param1) {
            croak "Property path is mandatory parameter";
        }
        $LOG_LEVEL = $param1;
        return 1;
    }
}
sub new {
    my ($class, $opts) = @_;

    my ($level, $logToProperty);

    if (!defined $opts->{level}) {
        $level = $LOG_LEVEL;
    }
    else {
        $level = $opts->{level};
    }

    if (!defined $opts->{logToProperty}) {
        $logToProperty = $LOG_TO_PROPERTY;
    }
    else {
        $logToProperty = $opts->{logToProperty};
    }
    my $self = {
        level           => $level,
        logToProperty   => $logToProperty
    };
    bless $self, $class;
    return $self;
}

sub fwLogInfo {
    my @params = @_;

    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, $LOGGER;
    }
    return info(@params);
}
sub info {
    my ($self, @messages) = @_;
    $self->_log(INFO, @messages);
}


sub fwLogDebug {
    my @params = @_;

    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, $LOGGER;
    }
    return debug(@params);
}
sub debug {
    my ($self, @messages) = @_;
    $self->_log(DEBUG, '[FLOWPDF_DEBUG]', @messages);
}


sub fwLogError {
    my @params = @_;

    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, $LOGGER;
    }
    return error(@params);
}
sub error {
    my ($self, @messages) = @_;
    $self->_log(ERROR, '[FLOWPDF_ERROR]', @messages);
}


sub fwLogWarning {
    my @params = @_;

    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, $LOGGER;
    }
    return warning(@params);
}
sub warning {
    my ($self, @messages) = @_;
    $self->_log(INFO, '[FLOWPDF_WARNING]', @messages);
}


sub fwLogTrace {
    my @params = @_;
    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, $LOGGER;
    }
    return trace(@params);
}
sub trace {
    my ($self, @messages) = @_;
    $self->_log(TRACE, '[FLOWPDF_TRACE]', @messages);
}

sub level {
    my ($self, $level) = @_;

    if (defined $level) {
        $self->{level} = $level;
    }
    else {
        return $self->{level};
    }
}

sub logToProperty {
    my ($self, $prop) = @_;

    if (defined $prop) {
        $self->{logToProperty} = $prop;
    }
    else {
        return $self->{logToProperty};
    }
}


my $length = 40;

sub divider {
    my ($self, $thick) = @_;

    if ($thick) {
        $self->info('=' x $length);
    }
    else {
        $self->info('-' x $length);
    }
}

sub header {
    my ($self, $header, $thick) = @_;

    my $symb = $thick ? '=' : '-';
    $self->info($header);
    $self->info($symb x $length);
}

sub _log {
    my ($self, $level, @messages) = @_;

    return if $level > $self->level;
    my @lines = ();
    for my $message (@messages) {
        if (ref $message) {
            my $t = Dumper($message);
            $t = $self->maskLine($t);
            print $t;
            push @lines, $t;
        }
        else {
            $message = $self->maskLine($message);
            print "$message\n";
            push @lines, $message;
        }
    }

    if ($self->{logToProperty}) {
        my $prop = $self->{logToProperty};
        my $value = "";
        eval {
            $value = $self->ec->getProperty($prop)->findvalue('//value')->string_value;
            1;
        };
        unshift @lines, split("\n", $value);
        $self->ec->setProperty($prop, join("\n", @lines));
    }
}


sub ec {
    my ($self) = @_;
    unless($self->{ec}) {
        require ElectricCommander;
        my $ec = ElectricCommander->new;
        $self->{ec} = $ec;
    }
    return $self->{ec};
}

1;
