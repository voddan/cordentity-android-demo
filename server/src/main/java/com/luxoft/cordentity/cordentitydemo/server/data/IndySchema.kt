package com.luxoft.cordentity.cordentitydemo.server.data

import kotlin.math.absoluteValue
import kotlin.random.Random

data class IndySchema(
    val name: String,
    val attributes: AttributesWithDefaultValueMap,
    val version: String
) {
    constructor(name: String, attributes: Map<String, String>, version: String) : this(
        name,
        AttributesWithDefaultValueMap(attributes),
        version
    )

    //This is required because objects hash is used as ID, but we don`t wont it to change because of different default values
    class AttributesWithDefaultValueMap(map: Map<String, String>) : HashMap<String, String>() {
        init {
            putAll(map)
        }

        override fun hashCode(): Int {
            return keys.hashCode()
        }
    }
}

object PossibleIndySchemas {
    object Human {
        val v1_0 = IndySchema(
            name = "Human",
            attributes = mapOf("name" to "John", "age" to "18", "sex" to "male", "height" to "180"),
            version = "1.0"
        )
    }

    object Patient {
        val v1_0 = IndySchema(
            name = "Patient",
            attributes = mapOf("medical_id" to "${Random.nextInt().absoluteValue}", "medical condition" to "Healthy"),
            version = "1.0"
        )
    }

    object Citizen {
        val v1_0 = IndySchema(
            name = "Citizen",
            attributes = mapOf("government_id" to "${Random.nextInt().absoluteValue}", "address" to "Russia, St. Petersburg, Lenina st. 1"),
            version = "1.0"
        )
    }

    object Test {
        val v1_0 = IndySchema(
            name = "Test",
            attributes = mapOf("government_id" to "${Random.nextInt().absoluteValue}", "address" to "Russia, St. Petersburg, Lenina st. 1"),
            version = "1.0"
        )
    }

    object Test2 {
        val v1_0 = IndySchema(
            name = "Test2",
            attributes = mapOf("government_id" to "${Random.nextInt().absoluteValue}", "address" to "Russia, St. Petersburg, Lenina st. 1"),
            version = "2.0"
        )
    }

    val active = listOf(Human.v1_0, Patient.v1_0, Citizen.v1_0)
//    val active = listOf(Test.v1_0)
}
