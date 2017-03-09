#
#  Copyright 2015 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

=head1 NAME

preamble.pl

=head1 DESCRIPTION

Preamble for application server plugins. Imports necessary modules.

=cut

use strict;
use warnings;
use ElectricCommander::PropDB;
use ElectricCommander::PropMod;
use Carp;

$| = 1;

my $ec = ElectricCommander->new();

my $load = sub {
    my $property_path = shift;

    ElectricCommander::PropMod::loadPerlCodeFromProperty(
        $ec, $property_path
    ) or do {
        croak "Can't load property $property_path";
    };
};

$load->('/myProject/lib/EC/Plugin/Core');
$load->('/myProject/lib/EC/Plugin/MicroRest');
$load->('/myProject/lib/EC/Bamboo');

