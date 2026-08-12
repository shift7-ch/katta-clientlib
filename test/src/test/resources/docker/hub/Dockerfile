ARG KATTA_SERVER_IMAGE

FROM ${KATTA_SERVER_IMAGE} AS server

FROM registry.access.redhat.com/ubi9-minimal:latest
USER root
RUN microdnf update -y && microdnf -y install net-tools nginx gettext && microdnf clean all -y
COPY --from=server /work /work
