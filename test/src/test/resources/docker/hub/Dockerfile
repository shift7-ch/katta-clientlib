ARG KATTA_SERVER_IMAGE
FROM ${KATTA_SERVER_IMAGE}

USER root
RUN microdnf update -y && microdnf -y install wget net-tools nginx gettext && microdnf clean all -y && systemctl enable nginx
