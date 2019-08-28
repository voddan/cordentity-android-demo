package com.luxoft.cordentity.cordentitydemo.server

import com.luxoft.blockchainlab.corda.hyperledger.indy.AgentConnection
import com.luxoft.cordentity.cordentitydemo.server.components.IndyAgentClient
import com.luxoft.cordentity.cordentitydemo.server.components.IndyComponent
import com.luxoft.cordentity.cordentitydemo.server.data.IndySchema
import com.luxoft.cordentity.cordentitydemo.server.data.Invite
import com.luxoft.cordentity.cordentitydemo.server.data.IssueCredentialsRequest
import com.luxoft.cordentity.cordentitydemo.server.data.PossibleIndySchemas
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.random.Random
import kotlin.random.nextUInt


private val indyAgent2: AgentConnection by lazy {
    val indyAgentClient = IndyAgentClient("ws://3.17.65.252:8095/ws", "agentUser", "password")
    indyAgentClient.agentClient()
}

private val indy2 = IndyComponent(
        "genesis/lux-private.txn",
        "default_pool",
        walletName = "secondWallet",
        walletPassword = "password",
        did = "RRqP3Ze8x953G3chawJFJm",
        seed = "000000000000000000000000Trustee1")


private val server2 = embeddedServer(Netty, port = 8081) {
    routing {
        get("issuerDid") {
            call.respondText(indy2.user.walletUser.getIdentityDetails().did)
        }

        get("schemas") {
            val map: Map<Int, IndySchema> = PossibleIndySchemas.active.associate { it.hashCode() to it }
            call.respond(map)
        }

        get("requestConnection") {
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

            val client = HttpClient(Apache) {
                install(JsonFeature) {
                    serializer = GsonSerializer()
                }
            }

            val invite = client.post<Invite> {
                url(port = 8080, path = "/api/credential/issueCredentials")
                contentType(ContentType.Application.Json)
                body = issueCredential
            }

            val connection = indyAgent2.acceptInvite(invite.invite).toBlocking().value()

            call.respondText(""" 
                Connection:
                    me: ${connection.myDID()}
                    they: ${connection.partyDID()}
            """.trimIndent())
        }

        get("/") {
            call.respondText("Hello World!")
        }

        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
    }
}


fun main() {
    server2.start(wait = true)
}