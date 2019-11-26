#!/usr/bin/perl
use strict;
use warnings;
use Test::More;
use DateTime;
use DateTime::Format::ISO8601;
use Data::Dumper;

my $date = '2019-11-24T13:10:25.227-05:00';

# my $dateObj = DateTime::Format::ISO8601::parse_datetime('%Y-%m-%dT%H:%M%S.%3N', $date);
my $dateObj = DateTime::Format::ISO8601::parse_datetime('YYYY-MM-DDThh:mm:ssZ', $date);
$dateObj->set_time_zone('UTC');
my $utcDate = $dateObj->strftime("%Y-%m-%dT%H:%M:%S.%3N") . "Z";
print $utcDate . "\n";

ok($utcDate =~ /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,4})?Z$/, 'Date was converted to a compatible format');

done_testing();

