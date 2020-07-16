package com.workshop

class Pipeline implements Serializable {

    String repository_name
    String branch_name
    String git_user
    String app_port
    String pr_num
    String dockerTool
    String merge_url

    Pipeline(
        String repository_name,
        String branch_name,
        String git_user,
        String app_port,
        String pr_num,
        String dockerTool
    ){
        this.repository_name = repository_name
        this.branch_name = branch_name
        this.git_user = git_user
        this.app_port = app_port
        this.pr_num = pr_num
        this.dockerTool = dockerTool
    }

}

