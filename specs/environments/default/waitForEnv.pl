#!ec-perl

use LWP::UserAgent;
use HTTP::Request;
use HTTP::Response;
use Data::Dumper;

my $ua = LWP::UserAgent->new();

my $bamboo_url = 'http://localhost:8081' ;
my $bamboo_username = $ENV{BAMBOO_USERNAME} || 'admin';
my $bamboo_password = $ENV{BAMBOO_PASSWORD} || 'admin123';

my $request = HTTP::Request->new('GET',
    $bamboo_url,
    undef,
    "username=$bamboo_username&password=$bamboo_password"
);

my $code = 500;
my $counter = 300;

while ($code >= 300 && $counter ){
    my HTTP::Response $resp = $ua->request($request);
    print Dumper $resp;

    $code = $resp->code();
    exit 0 if ($code == 200);

    sleep 5;
    $counter -= 5;
}

exit 1;