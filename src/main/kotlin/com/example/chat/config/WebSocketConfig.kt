package com.example.chat.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * STOMP-over-WebSocket setup:
 * - clients connect to `/ws` (handshake is open; the JWT is authenticated on the STOMP CONNECT
 *   frame by [WebSocketAuthInterceptor], so `/ws` must be permitted in the   security chain);
 * - the in-memory simple broker delivers to `/topic` and `/queue`;
 * - client messages are routed to `@MessageMapping` methods under the `/app` prefix;
 * - per-user destinations use the `/user` prefix. A client subscribes to `/user/queue/inbox`
 *   (conversation-list metadata) and `/user/queue/read` (read receipts) while online, and to
 *   `/user/queue/conv/{id}` only while a thread is open — subscribing to the latter is what marks
 *   the thread "viewed"/read server-side (see [WebSocketPresenceListener]).
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val authInterceptor: WebSocketAuthInterceptor,
    @Value($$"${app.cors.allowed-origins:}") private val allowedOrigins: List<String>,
) : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val origins = allowedOrigins.filter { it.isNotBlank() }
        val endpoint = registry.addEndpoint("/ws")
        // No origins set = browsers blocked; native apps (no Origin header) still connect.
        if (origins.isNotEmpty()) {
            endpoint.setAllowedOriginPatterns(*origins.toTypedArray())
        }
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(authInterceptor)
    }
}
