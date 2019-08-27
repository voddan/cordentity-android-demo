package com.luxoft.cordentity.cordentitydemo.server.components

import com.luxoft.blockchainlab.hyperledger.indy.models.CredentialDefinition
import com.luxoft.blockchainlab.hyperledger.indy.utils.SerializationUtils
import com.luxoft.cordentity.cordentitydemo.server.data.IndySchema

typealias CredentialDefinitionCrudRepository = MutableMap<Int, CredentialDefinitionEntity>

class CredentialDefinitionEntity(val schema: IndySchema, val credDef: CredentialDefinition) {
    val schemaId = schema.hashCode()
    val json = SerializationUtils.anyToJSON(credDef)

    fun getObj() = SerializationUtils.jSONToAny<CredentialDefinition>(json)
}