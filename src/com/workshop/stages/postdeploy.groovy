#!/usr/bin/groovy
package com.workshop.stages

import com.workshop.Pipeline

def healthcheck(Pipeline p) {
    def hostIp = sh script: "ip route show | awk '/default/ {print \$3}' | tr -d '\n'", returnStdout: true

    timeout(time: p.timeout_hc, unit: 'SECONDS'){
        waitUntil(quiet: true) {
            def response = sh script: "curl ${hostIp}:${p.app_port}/ping", returnStdout: true
            if (response != "pong!"){
                error("ERROR102 - Service is Unhealthy for last ${p.timeout_hc} Second")
            } else {
                println "Service is Healthy :D"
                return true
            }
        }
    }
}

def deleteOldImage(Pipeline p) {
    withEnv(["PATH+DOCKER=${p.dockerTool}/bin"]){
        def output = sh script: "docker rmi \$(docker images | grep -Ev 'latest|${BUILD_NUMBER}' | awk '/${p.git_user}\\/${p.repository_name}/ {print \$1\":\"\$2}')", returnStdout: true
        println "Remove old Image"
        println output

        //def output2 = sh script: "docker image prune -f",  returnStdout: true
        //println "Remove unused images"
        //println output2
    }
}
