package com.luxoft.cordentity.cordentitydemo.server.data

data class Response(val body: String)

data class Invite(val invite: String)

// [schemaHash(Int) : [ attributeName(String) : attributeRaw(String)]
data class IssueCredentialsRequest(val listOfCredentials: Map<Int, Map<String, String>>)
