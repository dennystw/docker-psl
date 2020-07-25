package com.workshop

class Pipeline implements Serializable {

    String repository_name
    String branch_name
    String git_user
    String docker_user
    String app_port
    String pr_num
    String dockerTool
    String merge_url
    String docker_registry
    String timeout_hc
    
    String is_merged

    Pipeline(
        String repository_name,
        String branch_name,
        String git_user,
        String docker_user,
        String app_port,
        String pr_num,
        String dockerTool,
        String docker_registry,
        String timeout_hc
    ){
        this.repository_name = repository_name
        this.branch_name = branch_name
        this.git_user = git_user
        this.docker_user = docker_user
        this.app_port = app_port
        this.pr_num = pr_num
        this.dockerTool = dockerTool
        this.docker_registry = docker_registry
        this.timeout_hc = timeout_hc
    }

}

