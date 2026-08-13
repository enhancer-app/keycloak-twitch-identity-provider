FROM quay.io/keycloak/keycloak:26.5.2 AS builder
COPY build/docker/keycloak-twitch.jar /opt/keycloak/providers/keycloak-twitch.jar
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.5.2
COPY --from=builder /opt/keycloak/ /opt/keycloak/
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start"]
