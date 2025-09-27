package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.auth.OutlookAuthManager
import com.microsoft.graph.models.Message
import com.microsoft.graph.options.QueryOption
import com.microsoft.graph.requests.GraphServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutlookService @Inject constructor(
    private val authManager: OutlookAuthManager
) {

    private fun getGraphClient(accessToken: String): GraphServiceClient {
        val authProvider = com.microsoft.graph.authentication.IAuthenticationProvider { requestUrl ->
            accessToken
        }
        return GraphServiceClient.builder().authenticationProvider(authProvider).buildClient()
    }

    suspend fun searchEmails(
        accessToken: String,
        from: String,
        subject: String,
        before: String,
        after: String
    ): List<Message>? = withContext(Dispatchers.IO) {
        val client = getGraphClient(accessToken)
        val filterOptions = mutableListOf<String>()

        if (from.isNotBlank()) {
            filterOptions.add("from/emailAddress/address eq '$from'")
        }
        if (subject.isNotBlank()) {
            filterOptions.add("subject eq '$subject'")
        }
        if (after.isNotBlank()) {
            filterOptions.add("receivedDateTime ge $after")
        }
        if (before.isNotBlank()) {
            filterOptions.add("receivedDateTime le $before")
        }

        val filterQuery = filterOptions.joinToString(" and ")
        val queryOptions = listOf(QueryOption("$" + "filter", filterQuery))

        client.me().messages().buildRequest(queryOptions).get()?.currentPage
    }
}