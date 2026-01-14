package com.gitranker.api.global.auth;

import java.util.Map;

public record OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String username, String nodeId,
                              String profileImage) {

    public static OAuthAttributes ofGitHub(String userNameAttributeName, Map<String, Object> attributes) {
        return new OAuthAttributes(
                attributes,
                userNameAttributeName,
                (String) attributes.get("login"),
                (String) attributes.get("node_id"),
                (String) attributes.get("avatar_url")
        );
    }
}
