=====INDEX=====
1.Included files
2.How to run
3.Console Commands

-----------------------------------------------------------------------------------------------------------------------

1.INCLUDED FILES:

All source files are within the following packages.

cs555/nodes
cs555/routing
cs555/transport
cs555/util
cs555/wireformats
READ_ME.txt
Makefile

-----------------------------------------------------------------------------------------------------------------------

2.HOW TO RUN:

DiscoveryNode 	: java cs555.nodes.DiscoveryNode <listening_portnum>
Peer		: java cs555.nodes.PeerNode <DiscoveryNode-host> <DiscoveryNode-port> <listening_portnum> <Identifier>
DataStore	: java cs555.nodes.DataStore <DiscoveryNode-host> <DiscoveryNode-port> <listening_portnum>

-----------------------------------------------------------------------------------------------------------------------

3.CONSOLE COMMANDS

DiscoveryNode:
	list-nodes

Peer:
	leaf-set
	routing-table
	list-files

DataStore:
	store key:<key> <file_name>
	read key:<key> <file_name>
-----------------------------------------------------------------------------------------------------------------------
