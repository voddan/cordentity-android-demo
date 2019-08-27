package com.luxoft.cordentity.cordentitydemo.server.components


import com.luxoft.blockchainlab.hyperledger.indy.IndyUser
import com.luxoft.blockchainlab.hyperledger.indy.SsiUser
import com.luxoft.blockchainlab.hyperledger.indy.helpers.GenesisHelper
import com.luxoft.blockchainlab.hyperledger.indy.helpers.PoolHelper
import com.luxoft.blockchainlab.hyperledger.indy.helpers.WalletHelper
import com.luxoft.blockchainlab.hyperledger.indy.ledger.IndyPoolLedgerUser
import com.luxoft.blockchainlab.hyperledger.indy.models.DidConfig
import com.luxoft.blockchainlab.hyperledger.indy.wallet.IndySDKWalletUser
import com.luxoft.blockchainlab.hyperledger.indy.wallet.getOwnIdentities
import com.luxoft.cordentity.cordentitydemo.server.data.PossibleIndySchemas
import mu.KotlinLogging
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.pool.Pool
import java.io.File

class IndyComponent (
    val GENESIS_PATH: String,
    val poolName: String,
    val walletName: String,
    val walletPassword: String,
    val did: String? = null,
    val seed: String? = null
) {
    val credDefStorage: CredentialDefinitionCrudRepository = hashMapOf()

    private val logger = KotlinLogging.logger {}


    lateinit var user: SsiUser

    init {
        initFunction()
    }

    fun initFunction() {
        logger.info("Indy initialization")

        val wallet = WalletHelper.openOrCreate(walletName, walletPassword)
        val genesisFile = File(GENESIS_PATH)
        if (!GenesisHelper.exists(genesisFile))
            throw RuntimeException("Genesis file $GENESIS_PATH doesn't exist")

        val pool = PoolHelper.openOrCreate(genesisFile, poolName)

        val constantDid = did ?: wallet.getOwnIdentities().firstOrNull()?.did

        val walletUser = if(constantDid != null && wallet.getOwnIdentities().any { it.did == did }) {
            // did != null && wallet.getOwnIdentities().any { it.did == did }
            // did == null && wallet.getOwnIdentities().isNotEmpty()
            logger.info { "Opening wallet with DID($constantDid)" }
            IndySDKWalletUser(wallet, constantDid)
        } else {
            // did != null && wallet.getOwnIdentities().none { it.did == did }
            // did == null && wallet.getOwnIdentities().isEmpty()
            logger.info { "Creating new identity with DID(${did ?: "\$random"}) and seed(${seed ?: "\$random"}" }
            val user = IndySDKWalletUser(wallet, DidConfig(did, seed))
            logger.info { "Opened wallet with DID(${user.did}, ${user.verkey})" }
            user
        }

        val ledgerUser = IndyPoolLedgerUser(pool, walletUser.getIdentityDetails().did) { walletUser.sign(it) }
        user = IndyUser.with(walletUser).with(ledgerUser).build()

        val nym = user.ledgerUser.getNym(user.walletUser.getIdentityDetails())
        if (nym.result.getData() == null) {
            grantTrust(pool)
        }

        for (schema in PossibleIndySchemas.active) {
            val key = schema.hashCode()
            val newSchema = user.createSchemaAndStoreOnLedger(schema.name, schema.version, schema.attributes.keys.toList())

            logger.info { "Create (or retrieve) schema($newSchema)" }

            val schemaId = newSchema.getSchemaIdObject()

            val credentialDefinition =
                    user.ledgerUser.retrieveCredentialDefinition(schemaId, IndySDKWalletUser.TAG)
                            ?: user.createCredentialDefinitionAndStoreOnLedger(schemaId, false)

            credDefStorage.put(key, CredentialDefinitionEntity(schema, credentialDefinition))
        }

        logger.info("Indy initialization passed")
    }

    private fun grantTrust(pool: Pool) {
        val TRUSTEE_SEED = "000000000000000000000000Trustee1"
        val trusteeWalletName = "Trustee"
        val trusteeWalletPassword = "123"

        WalletHelper.createOrTrunc(trusteeWalletName, trusteeWalletPassword)
        val trusteeWallet = WalletHelper.openExisting(trusteeWalletName, trusteeWalletPassword)
        val trusteeDid = Did.createAndStoreMyDid(trusteeWallet, """{"seed":"$TRUSTEE_SEED"}""").get()

        IndyPoolLedgerUser(pool, trusteeDid.did) {
            IndySDKWalletUser(trusteeWallet, trusteeDid.did).sign(it)
        }.storeNym(user.walletUser.getIdentityDetails().copy(role = "TRUSTEE"))
    }

}
