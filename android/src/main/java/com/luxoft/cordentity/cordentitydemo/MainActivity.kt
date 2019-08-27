package com.luxoft.cordentity.cordentitydemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.luxoft.blockchainlab.corda.hyperledger.indy.PythonRefAgentConnection


val indyAgentWSEndpoint = "ws://3.17.65.252:8094/ws"

val agentConnection = PythonRefAgentConnection()
val agentConnect = agentConnection.connect(indyAgentWSEndpoint, login = "medical-supplychain", password = "secretPassword").toCompletable()

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        agentConnect.await()
        agentConnection
    }
}
