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
            String dockerTool = tool name: 'docker', type: 'dockerTool'
            println("${dockerTool}")

            withEnv(["PATH+DOCKER=${dockerTool}/bin"]) {
                // sprebuild.checkout()
                git branch: "${branch_name}", url: "https://github.com/tobapramudia/${repository_name}.git"

                def golangImage = docker.image("${c.default_golang_base_image}:${golang_tag}")
                golangImage.inside {
                    // sprebuild.buildTest()
                    def list_dir_go = sh returnStdout: true, script: "/bin/bash -c 'grep -l \"func\\ main()\" * -r | grep \"\\.go\$\" | xargs dirname'"
                    for(build_dir in "${list_dir_go}".split('\n')) {
                        dir("${build_dir}") {
                            build_status["'${build_dir}'"] = [:]
                            build_status["'${build_dir}'"]['val'] = "${build_dir}"
                            build_status["'${build_dir}'"]['error_message'] = "Error building ${build_dir}, please read error log above"
                            build_status["'${build_dir}'"]['label'] = "${build_dir}"

                            println "----------------------------------"
                            println "\u001b[36mBuilding \u001b[33m${build_dir}...\u001b[0m"

                            build = sh returnStatus: true, script: "go build -v"
                            if (build == 0) {
                                build_status["'${build_dir}'"]['status'] = true
                                println "\u001b[36mBuilding \u001b[33m${build_dir} \u001b[32mDONE !!!\u001b[0m"
                            } else {
                                build_status["'${build_dir}'"]['status'] = false
                                println "\u001b[36mBuilding \u001b[33m${build_dir} \u001b[31mFAILED !!!\u001b[0m"
                            }
                            println "----------------------------------"
                        }
                    }
                    println("=================\u001b[44mBuild Summary\u001b[0m================")
                    def resultBuild = u.checkValidation(build_status)
                    if (resultBuild) {
                        println "\u001b[32mDone...\u001b[0m"
                    } else {
                        println "\u001b[31mThere are/is failed build, please revised above log!!!\u001b[0m"
                        println "==============================================\n\n"
                        error "Failed Build"
                    }
                    println "==============================================\n\n"
                }
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

return this