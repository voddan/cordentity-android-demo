package com.luxoft.cordentity.cordentitydemo.server.components


import com.luxoft.blockchainlab.corda.hyperledger.indy.AgentConnection
import com.luxoft.blockchainlab.corda.hyperledger.indy.PythonRefAgentConnection
import mu.KotlinLogging

class IndyAgentClient(val agentEndpoint: String,
                      val agentUser: String,
                      val agentPassword: String){

    private  val logger = KotlinLogging.logger {}

    fun agentClient(): AgentConnection {
        logger.info { "AgentConnection creation STARTED!!!" }
        return try {
            PythonRefAgentConnection().apply {
                connect(
                    url = agentEndpoint,
                    login = agentUser,
                    password = agentPassword
                ).toBlocking().value()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error while creating IndyAgentClient" }
            throw e
        }
    }
}