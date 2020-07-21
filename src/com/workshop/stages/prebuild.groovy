#!/usr/bin/groovy
package com.workshop.stages

import com.workshop.Pipeline
import com.workshop.Config

def details(Pipeline p) {
    println("================\u001b[44mDetails Of Jobs\u001b[0m===============")
    println("\u001b[36mRepository Name : \u001b[0m${p.repository_name}")
    println("\u001b[36mBranch Name : \u001b[0m${p.branch_name}")
    println("\u001b[36mApplication Port : \u001b[0m${p.app_port}")
    println("")
    println("\u001b[36mTesting this PR : \u001b[0m#${p.pr_num} - https://github.com/${p.git_user}/${p.repository_name}/pull/${p.pr_num}")
    println("\u001b[36mMerging to branch : \u001b[0m${p.branch_name}")
}

def checkPR(Pipeline p) {
    def git_pr_response = httpRequest authentication: 'github-personal', url: "https://api.github.com/repos/${p.git_user}/${p.repository_name}/pulls/1", wrapAsMultipart: false
    def parsed_git_pr_response = readJSON text: "${git_pr_response.content}"
    
    println("================\u001b[44mDetails Of Jobs\u001b[0m===============")
    if ("${parsed_git_pr_response['base']['ref']}" != "${p.branch_name}") {
        error "Base branch on pull request is different than used in branch, used branch : ${p.branch_name}, your pull request base is pointed to : ${parsed_git_pr_response['base']['ref']}"
    }
    println "\u001b[36mBase Branch check \u001b[32mPASSED\u001b[0m"
    println "==============================================\n\n"
    
    println "=============\u001b[44mPull Request Detail\u001b[0m=============="
    println "\u001b[36mPR Creator : \u001b[0m${parsed_git_pr_response['user']['login']}"
    println "\u001b[36mRepository : \u001b[0m${parsed_git_pr_response['base']['repo']['html_url']}"
    println "\u001b[36mHead Branch : \u001b[0m${parsed_git_pr_response['head']['ref']}"
    println "\u001b[36mBase Branch : \u001b[0m${parsed_git_pr_response['base']['ref']}"
    println "\u001b[36mMerged Status : \u001b[33m${parsed_git_pr_response['merged']}\u001b[0m"
    println "==============================================\n\n"

    p.is_merged = "${parsed_git_pr_response['merged']}"
}

def validation(Pipeline p) {
    if(!p.repository_name) {
        "Repository name can't be empty"
        error("ERROR101 - MISSING REPOSITORY_NAME")
    }
    if(!p.branch_name) {
        "Branch name can't be empty"
        error("ERROR101 - MISSING BRANCH_NAME")
    }
    if(!p.app_port) {
        "Application port can't be empty"
        error("ERROR101 - MISSING APP_PORT")
    }
    if(!p.pr_num) {
        "PR Number can't be empty"
        error("ERROR101 - MISSING PR_NUM")
    }
}

def checkoutBuildTest(Pipeline p) {
    c = new Config()

    withCredentials([usernamePassword(credentialsId: 'github-personal', passwordVariable: 'git_token', usernameVariable: 'git_username')]) {
        println "============\u001b[44mCommencing PR Checkout\u001b[0m============"
        git branch: "${p.branch_name}", url: "https://github.com/${p.git_user}/${p.repository_name}.git"
        
        println "\u001b[36mChecking out from : \u001b[0mpull/${p.pr_num}/head:pr/${p.pr_num}..."
        sh "git config --global user.name '${git_username}'"
        sh "git config --global user.email '${git_username}@example.com'"
        sh "git branch -D pr/${p.pr_num} &> /dev/null || true"
        sh "git fetch origin pull/${p.pr_num}/head:pr/${p.pr_num}"
        sh "git merge --no-ff pr/${p.pr_num}"
    }

    docker.withTool("${c.default_docker_jenkins_tool}") {
        def golangImage = docker.image("${c.default_golang_base_image}")
        golangImage.inside("-u 0") {
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
