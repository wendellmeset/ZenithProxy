package com.zenith.feature.api.prioban;

import com.zenith.feature.api.Api;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.zenith.Shared.DEFAULT_LOG;

public class PriobanApi extends Api {
    public static final PriobanApi INSTANCE = new PriobanApi();

    public PriobanApi() {
        super("https://shop.2b2t.org");
    }

    /**
     * Note: the webstore is behind Cloudflare, so its highly likely it receives a cloudflare challenge
     * which we do not currently handle at all
     * This might not happen on home internet connections, but from VPS's it is very likely
     */
    public Optional<Boolean> checkPrioBan(String playerName) {
        HttpRequest request = buildBaseRequest("/checkout/packages/add/1994962/single")
            .POST(HttpRequest.BodyPublishers.ofString("ign=" + playerName))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build();
        try (HttpClient client = buildHttpClient()) {
            var r = client
                .send(request, HttpResponse.BodyHandlers.ofString());
            try {
                var cfMitigatedHeader = r.headers().map().get("cf-mitigated");
                if (cfMitigatedHeader != null && cfMitigatedHeader.contains("challenge")) {
                    DEFAULT_LOG.warn("[PrioBan Check] Received Cloudflare challenge, unable to proceed");
                    return Optional.empty();
                }
                List<String> cookies = r.headers().map().get("Set-Cookie");
                Optional<String> buycraftBasketCookie = cookies.stream()
                    .filter(c -> c.startsWith("buycraft_basket"))
                    .findFirst();
                Optional<String> bannedCookie = cookies.stream()
                    .filter(c -> c.contains("XRxlbOYKOzX5HYSsk7VO72KxURUxqkzYCSTxTat"))
                    .findFirst();
                if (buycraftBasketCookie.isPresent())
                    return Optional.of(false);
                else if (bannedCookie.isPresent())
                    return Optional.of(true);
                else {
                    DEFAULT_LOG.warn("[PrioBan Check] Unexpected response from 2b2t webstore: {}\n{}", r.statusCode(), cookies);
                    return Optional.empty();
                }
            } catch (final Exception e) {
                DEFAULT_LOG.error("[PrioBan Check] Unable to parse response cookies from 2b2t webstore", e);
                return Optional.empty();
            }
        } catch (Exception e) {
            DEFAULT_LOG.error("[PrioBan Check] Failed to parse response", e);
            return Optional.empty();
        }
    }

    @Override
    protected HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    }

}
