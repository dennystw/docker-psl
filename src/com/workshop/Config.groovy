#!/usr/bin/groovy
package com.workshop

class Config {
    // Docker related default variable
    def default_docker_registry = "registry.hub.docker.com"

    // Golang related default variable
    def default_golang_base_image = "golang"
    def default_golang_base = "alpine"
    def default_golang_version = "1.14.4"
}