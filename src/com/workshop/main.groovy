#!/usr/bin/groovy
package com.workshop

import com.workshop.Config
import com.workshop.stages.*


def main(script) {
    c = new Config()
    sprebuild = new prebuild()
    sbuild = new build()
    spostbuild = new postbuild()
    sdeploy = new deploy()
    spostdeploy = new postdeploy()

    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm', 'defaultFg': 1, 'defaultBg': 2]) {
        stage('Details') {
            sprebuild.details()
        }

        stage('Checkout') {
            sprebuild.checkout()
        }

        stage('Unit & Build Test') {
            sprebuild.buildTest()
            sprebuild.unitTest()
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