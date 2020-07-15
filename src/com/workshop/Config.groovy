#!/usr/bin/groovy
package com.workshop

class Config {
    // Docker related default variable
    def default_docker_registry = "https://registry-1.docker.io/v2/"

    // Golang related default variable
    def default_golang_base_image = "tobapramudia/tkpd-demo:onbuild"
}