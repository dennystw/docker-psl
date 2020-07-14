#!/usr/bin/groovy
package com.workshop

import com.workshop.Config
import com.workshop.stages.*


def main(script) {
    // Object initialization
    c = new Config()
    sprebuild = new prebuild()
    sbuild = new build()
    spostbuild = new postbuild()
    sdeploy = new deploy()
    spostdeploy = new postdeploy()

    // Pipeline specific variable get from injected env
    // Mandatory variable wil be check at details & validation steps
    def repository_name = ("${script.env.repository_name}" != "null") ? "${script.env.repository_name}" : ""
    def branch_name = ("${script.env.branch_name}" != "null") ? "${script.env.branch_name}" : ""

    // Have default value
    def golang_version = ("${script.env.golang_version}" != "null") ? "${script.env.golang_version}" : "${c.default_golang_version}"
    def docker_registry = ("${script.env.docker_registry}" != "null") ? "${script.env.docker_registry}" : "${c.default_docker_registry}"

    def golang_tag = "${golang_version}-${c.default_golang_base}"

    ansiColor('xterm') {
        stage('Pre Build - Details') {
            // sprebuild.details()
            if(!repository_name) {
                "Repository name can't be empty"
                error("ERROR101 - MISSING REPOSITORY_NAME")
            }
            if(!branch_name) {
                "Branch name can't be empty"
                error("ERROR101 - MISSING BRANCH_NAME")
            }

            println("================\u001b[44mDetails Of Jobs\u001b[0m===============")
            println("\u001b[36mRepository Name : \u001b[0m${repository_name}")
            println("\u001b[36mBranch Name : \u001b[0m${branch_name}")
        }

        stage('Pre Build - Checkout & Test') {
            tool name: 'docker', type: 'dockerTool'
            docker.image("${c.default_golang_base_image}:${golang_tag}").inside {
                // sprebuild.checkout()
                git branch: "${branch_name}", url: "https://github.com/tobapramudia/${repository_name}.git"
                // sprebuild.buildTest()
                // sprebuild.unitTest()
                sh "pwd"
            }
        }

        stage('Docker Build') {
            sbuild.build()
        }

        stage('Push Docker Image') {
            spostbuild.pushRegistry()
        }

        stage('Deploy') {
            sdeploy.deploy()
        }

        stage('Service Healthcheck') {
            spostdeploy.healthCheck()
        }
    }

}