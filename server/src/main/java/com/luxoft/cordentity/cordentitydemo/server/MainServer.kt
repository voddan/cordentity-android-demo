package com.luxoft.cordentity.cordentitydemo.server

import com.luxoft.blockchainlab.corda.hyperledger.indy.AgentConnection
import com.luxoft.blockchainlab.corda.hyperledger.indy.handle
import com.luxoft.blockchainlab.hyperledger.indy.logger
import com.luxoft.blockchainlab.hyperledger.indy.models.CredentialValue
import com.luxoft.cordentity.cordentitydemo.server.components.IndyAgentClient
import com.luxoft.cordentity.cordentitydemo.server.components.IndyComponent
import com.luxoft.cordentity.cordentitydemo.server.data.IndySchema
import com.luxoft.cordentity.cordentitydemo.server.data.Invite
import com.luxoft.cordentity.cordentitydemo.server.data.IssueCredentialsRequest
import com.luxoft.cordentity.cordentitydemo.server.data.PossibleIndySchemas
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.random.nextUInt


private val indyAgent1: AgentConnection by lazy {
    val indyAgentClient = IndyAgentClient("ws://3.17.65.252:8094/ws", "agentUser", "password")
    indyAgentClient.agentClient()
}

private val indyAgent2: AgentConnection by lazy {
    val indyAgentClient = IndyAgentClient("ws://3.17.65.252:8095/ws", "agentUser", "password")
    indyAgentClient.agentClient()
}


private val indy1 = IndyComponent(
        "genesis/lux-private.txn",
        "default_pool",
        walletName = "verifierWallet",
        walletPassword = "password",
        did = "RRqP3Ze8x953G3chawJFJm",
        seed = "000000000000000000000000Trustee1")


private val server1 = embeddedServer(Netty, port = 8080) {
    routing {
        route("api/credential") {
            get("issuerDid") {
                call.respondText(indy1.user.walletUser.getIdentityDetails().did)
            }

            get("schemas") {
                val map: Map<Int, IndySchema> = PossibleIndySchemas.active.associate { it.hashCode() to it }
                call.respond(map)
            }

            post("issueCredentials") {
                val issueCredential = call.receive(IssueCredentialsRequest::class)
                val invite = issueCredentials(indyAgent1, indy1, issueCredential)
                call.respond(invite)
            }

            get("test") {
                val issueCredential = IssueCredentialsRequest(
                        mapOf(
                                PossibleIndySchemas.Human.v1_0.hashCode() to mapOf(
                                        "name" to "Alex",
                                        "age" to "28",
                                        "sex" to "male",
                                        "height" to "175"
                                ),
                                PossibleIndySchemas.Patient.v1_0.hashCode() to mapOf(
                                        "medical id" to "${Random.nextUInt()}",
                                        "medical condition" to "<medicalCondition>"
                                )
                        )
                )

                val invite = issueCredentials(indyAgent1, indy1, issueCredential)
//                call.respond(invite)

                val connection = indyAgent2.acceptInvite(invite.invite).toBlocking().value()

                call.respondText(connection.toString())
            }
        }

        get("/") {
            call.respondText("Hello World!")
        }

    }

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
}


fun issueCredentials(indyAgent: AgentConnection, indyComponent: IndyComponent, request: IssueCredentialsRequest): Invite {
    val invite = indyAgent.generateInvite().toBlocking().value()

    indyAgent.waitForInvitedParty(invite, timeout = 10000).observeOn(Schedulers.newThread())
            .handle { indyPartyConnection, ex ->
                ex?.run {
                    logger.error(ex) { "Error waiting for invited party" }
                    return@handle
                }
                indyPartyConnection!!

                for ((requestSchema, requestedAttributes) in request.listOfCredentials) {
                    val credentialDefinition = indyComponent.credDefStorage.get(requestSchema.hashCode())!!.credDef
                    val createCredentialOffer = indyComponent.user.createCredentialOffer(credentialDefinition.getCredentialDefinitionIdObject())

                    indyPartyConnection.sendCredentialOffer(createCredentialOffer)
                    val credentialRequest = indyPartyConnection
                            .receiveCredentialRequest()
                            .timeout(60, TimeUnit.SECONDS)
                            .toBlocking().value()

                    val credentialInfo =
                            indyComponent.user.issueCredentialAndUpdateLedger(credentialRequest, createCredentialOffer, null) {
                                val newAttributes = requestedAttributes.mapValues { CredentialValue(it.value) }
                                attributes.putAll(newAttributes)
                            }
                    indyPartyConnection.sendCredential(credentialInfo)
                }
            }

    return Invite(invite)
}



fun main() {
    server1.start(wait = true)
}