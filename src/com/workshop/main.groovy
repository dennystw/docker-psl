#!/usr/bin/groovy
package com.workshop

import com.workshop.Config
import com.workshop.utils
import com.workshop.stages.*


def main(script) {
    // Object initialization
    c = new Config()
    u = new utils()
    sprebuild = new prebuild()
    sbuild = new build()
    spostbuild = new postbuild()
    sdeploy = new deploy()
    spostdeploy = new postdeploy()

    // Pipeline specific variable get from injected env
    // Mandatory variable wil be check at details & validation steps
    def repository_name = ("${script.env.repository_name}" != "null") ? "${script.env.repository_name}" : ""
    def branch_name = ("${script.env.branch_name}" != "null") ? "${script.env.branch_name}" : ""
    def git_user = ("${script.env.git_user}" != "null") ? "${script.env.git_user}" : ""
    def app_port = ("${script.env.app_port}" != "null") ? "${script.env.app_port}" : ""

    // Timeout for Healtcheck
    def timeout_hc = (script.env.timeout_hc != "null") ? script.env.timeout_hc : 10

    // Have default value
    def docker_registry = ("${script.env.docker_registry}" != "null") ? "${script.env.docker_registry}" : "${c.default_docker_registry}"
    
    // Initialize docker tools
    def dockerTool = tool name: 'docker', type: 'dockerTool'
    
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
            if(!app_port) {
                "Application port can't be empty"
                error("ERROR101 - MISSING APP_PORT")
            }

            println("================\u001b[44mDetails Of Jobs\u001b[0m===============")
            println("\u001b[36mRepository Name : \u001b[0m${repository_name}")
            println("\u001b[36mBranch Name : \u001b[0m${branch_name}")
            println("\u001b[36mApplication Port : \u001b[0m${app_port}")
        }

        stage('Pre Build - Checkout & Test') {

            docker.withTool('docker') {
                // sprebuild.checkout()
                // println "============\u001b[44mCommencing PR Checkout\u001b[0m============"
                // println "\u001b[36mChecking out from : \u001b[0mpull/${p.pr_num}/head:pr/${p.pr_num}..."
                // sh "git branch -D pr/${p.pr_num} &> /dev/null || true"
                // sh "git fetch origin pull/${p.pr_num}/head:pr/${p.pr_num}"
                // sh "git merge --no-ff pr/${p.pr_num}"
                git branch: "${branch_name}", url: "https://github.com/${git_user}/${repository_name}.git"

                def golangImage = docker.image("${c.default_golang_base_image}")
                golangImage.inside("-u 0") {
                    // sprebuild.buildTest()
                    build = sh returnStatus: true, script: "go build -v"
                    if (build == 0) {
                        println "\u001b[36mBuilding \u001b[33m. \u001b[32mDONE !!!\u001b[0m"
                    } else {
                        println "\u001b[36mBuilding \u001b[33m. \u001b[31mFAILED !!!\u001b[0m"
                        error("Build test failed")
                    }
                }
                golangImage.inside("-u 0") {
                    test = sh returnStatus: true, script: "go test ./..."
                    if (build == 0) {
                        println "\u001b[36mTesting \u001b[33m. \u001b[32mDONE !!!\u001b[0m"
                    } else {
                        println "\u001b[36mTesting \u001b[33m. \u001b[31mFAILED !!!\u001b[0m"
                        error("Unit test failed")
                    }
                }
            }
        }

        stage('Build & Push Image') {
            docker.withTool('docker') {
                 docker.withRegistry(docker_registry, "cred-docker") {
                    def image = docker.build("${git_user}/${repository_name}:build-$BUILD_NUMBER")
                    image.push()
                    image.push('latest')
                }
            }
        }

        stage('Deploy') {
            withEnv(["PATH+DOCKER=${dockerTool}/bin"]){
                println "Take Down previous Deployment"
                def response = sh script: "docker rm -f \$(docker ps -aq -f 'name=${repository_name}') &> /dev/null", returnStatus: true
                if ("${response}" == "0") {
                    println("Successfuly removing old container")
                } else {
                    println("Old version removed")
                }
            }

            docker.withTool('docker') {
                def image = docker.build("${git_user}/${repository_name}:build-$BUILD_NUMBER")
                image.run("--name ${repository_name}-$BUILD_NUMBER -p ${app_port}:${app_port}")
            }
        }

        stage('Service Healthcheck') {
            def hostIp = sh script: "ip route show | awk '/default/ {print \$3}' | tr -d '\n'", returnStdout: true

            timeout(time: timeout_hc, unit: 'SECONDS'){
                waitUntil(quiet: true) {
                    def response = sh script: "curl ${hostIp}:${app_port}/ping", returnStdout: true
                    if (response != "pong!"){
                        error("ERROR102 - Service is Unhealthy for last ${timeout_hc} Second")
                    } else {
                        println "Service is Healthy :D"
                        return true
                    }
                }
            }
        }
    }
}

return this