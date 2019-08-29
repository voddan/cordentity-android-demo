# Technical requirements for running the servers

## LibIndy

Install LibIndy version 1.8.2

## Indy Agents
<!--NOT NEEDED?-->
Make sure there is an instance of `teamblockchain/indy-agent` for every communicating device.
Start it up with [agents/agents-compose.yml](agents/agents-compose.yml)

## Indy Pool
<!--NOT NEEDED?-->
Make sure you running `teamblockchain/indy-pool:1.7.0`.  
Start it up with [indypool-compose.yml](indypool-compose.yml)

<!--//todo?-->
clean up yu local wallet?