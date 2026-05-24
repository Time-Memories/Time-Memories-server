package com.example.memories.infra.oauth;

import com.example.memories.domain.user.entity.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OAuthClientComposite {

    private final Map<AuthProvider, OAuthClient> clientMap;

    public OAuthClientComposite(List<OAuthClient> clients) {
        clientMap = clients.stream()
                .collect(Collectors.toMap(OAuthClient::getProvider, c -> c));
    }

    public OAuthClient getClient(AuthProvider provider) {
        return clientMap.get(provider);
    }
}