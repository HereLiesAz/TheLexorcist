package com.hereliesaz.lexorcist.service

import android.util.Log
import com.hereliesaz.lexorcist.auth.OutlookAuthManager // Keep for now, though accessToken is passed directly
import com.microsoft.graph.models.Message
import com.microsoft.graph.models.MessageCollectionResponse
import com.microsoft.graph.serviceclient.GraphServiceClient 
// Correct Kiota imports
import com.microsoft.kiota.authentication.AccessTokenProvider
import com.microsoft.kiota.authentication.AllowedHostsValidator
import com.microsoft.kiota.authentication.BaseBearerTokenAuthenticationProvider

// Import for request configuration, e.g., queryParameters
import com.microsoft.graph.users.item.messages.MessagesRequestBuilder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
// Removed CompletableFuture import as getAuthorizationToken is now synchronous for this provider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutlookService @Inject constructor(
    private val authManager: OutlookAuthManager // This is currently unused as token is passed to methods
) {

    private class OutlookAccessTokenProvider(private val currentAccessToken: String) : AccessTokenProvider {
        // Implement the synchronous getAuthorizationToken method
        override fun getAuthorizationToken(
            uri: URI?,
            additionalAuthenticationContext: MutableMap<String, Any>?
        ): String {
            return currentAccessToken
        }

        override fun getAllowedHostsValidator(): AllowedHostsValidator {
            val validHosts = arrayOf("graph.microsoft.com") 
            return AllowedHostsValidator(*validHosts) // Used spread operator
        }
    }

    private fun getGraphClient(accessToken: String): GraphServiceClient {
        val tokenProvider = OutlookAccessTokenProvider(accessToken)
        // Use BaseBearerTokenAuthenticationProvider from Kiota
        val authProvider = BaseBearerTokenAuthenticationProvider(tokenProvider)
        return GraphServiceClient(authProvider)
    }

    suspend fun searchEmails(
        accessToken: String,
        from: String,
        subject: String,
        before: String, // Expected ISO 8601 format: YYYY-MM-DDTHH:mm:ssZ
        after: String   // Expected ISO 8601 format: YYYY-MM-DDTHH:mm:ssZ
    ): List<Message>? = withContext(Dispatchers.IO) {
        val client = getGraphClient(accessToken)
        val filterClauses = mutableListOf<String>()

        if (from.isNotBlank()) {
            filterClauses.add("from/emailAddress/address eq '$from'")
        }
        if (subject.isNotBlank()) {
            filterClauses.add("subject eq '$subject'")
        }
        if (after.isNotBlank()) {
            filterClauses.add("receivedDateTime ge $after")
        }
        if (before.isNotBlank()) {
            filterClauses.add("receivedDateTime le $before")
        }

        val filterQuery = filterClauses.joinToString(separator = " and ")

        try {
            // The get() call itself returns the MessageCollectionResponse or null
            val messagesPage: MessageCollectionResponse? = client.me().messages().get { requestConfig ->
                if (filterQuery.isNotBlank()) {
                    requestConfig.queryParameters.filter = filterQuery
                }
                requestConfig.queryParameters.select = arrayOf(
                    "id", "receivedDateTime", "subject", "from", "bodyPreview", "sender", "toListRecipients", "body"
                )
                requestConfig.queryParameters.orderby = arrayOf("receivedDateTime desc")
                requestConfig.queryParameters.top = 50
            }
            messagesPage?.value // This is the value returned by the try block
        } catch (e: Exception) {
            Log.e("OutlookService", "Error searching emails via Graph: ${e.message}", e)
            null // This is the value returned by the catch block
        }
    }
}